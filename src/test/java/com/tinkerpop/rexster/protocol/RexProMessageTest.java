package com.tinkerpop.rexster.protocol;

import junit.framework.Assert;
import org.glassfish.grizzly.Buffer;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.Sequence;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Test;

public class RexProMessageTest {
    private Mockery mockery = new JUnit4Mockery();

    @Test
    public void readValidateBinary() {
        this.mockery = new JUnit4Mockery();

        final Buffer sourceBuffer = this.mockery.mock(Buffer.class);
        final Sequence sequence = this.mockery.sequence("sequence");

        this.mockery.checking(new Expectations() {{
            // magic - r
            oneOf(sourceBuffer).get();
            will(returnValue((byte) 'R'));
            inSequence(sequence);

            // magic - e
            oneOf(sourceBuffer).get();
            will(returnValue((byte) 'E'));
            inSequence(sequence);

            // magic - x
            oneOf(sourceBuffer).get();
            will(returnValue((byte) 'X'));
            inSequence(sequence);

            // magic - p
            oneOf(sourceBuffer).get();
            will(returnValue((byte) 'P'));
            inSequence(sequence);

            // version
            oneOf(sourceBuffer).get();
            will(returnValue((byte) 1));
            inSequence(sequence);

            // type
            oneOf(sourceBuffer).get();
            will(returnValue((byte) 2));
            inSequence(sequence);

            // flag
            oneOf(sourceBuffer).get();
            will(returnValue((byte) 3));
            inSequence(sequence);

            // session
            oneOf(sourceBuffer).getInt();
            will(returnValue(100));
            inSequence(sequence);

            // request
            oneOf(sourceBuffer).getInt();
            will(returnValue(200));
            inSequence(sequence);

            // body length
            oneOf(sourceBuffer).getInt();
            will(returnValue(5));
            inSequence(sequence);

            // body
            byte[] bytes = new byte[5];
            oneOf(sourceBuffer).get(with(any(bytes.getClass())));
            inSequence(sequence);
        }});

        RexProMessage msg = RexProMessage.read(sourceBuffer);

        this.mockery.assertIsSatisfied();

        Assert.assertEquals('R', msg.getHeader()[0]);
        Assert.assertEquals('E', msg.getHeader()[1]);
        Assert.assertEquals('X', msg.getHeader()[2]);
        Assert.assertEquals('P', msg.getHeader()[3]);
        Assert.assertEquals((byte) 1, msg.getVersion());
        Assert.assertEquals((byte) 2, msg.getType());
        Assert.assertEquals((byte) 3, msg.getFlag());
        Assert.assertEquals(100, msg.getSession());
        Assert.assertEquals(200, msg.getRequest());
        Assert.assertEquals(5, msg.getBodyLength());
        Assert.assertEquals(5, msg.getBody().length);
    }

    @Test
    public void writeValidateBinary() {
        this.mockery = new JUnit4Mockery();

        final Buffer output = this.mockery.mock(Buffer.class);
        final Sequence sequence = this.mockery.sequence("sequence");

        final RexProMessage sentMessage = new RexProMessage((byte) 1, (byte) 1, (byte) 1, 123, 456, "hello".getBytes());

        this.mockery.checking(new Expectations() {{
            oneOf(output).put(sentMessage.getHeader());
            inSequence(sequence);
            oneOf(output).put(sentMessage.getVersion());
            inSequence(sequence);
            oneOf(output).put(sentMessage.getType());
            inSequence(sequence);
            oneOf(output).put(sentMessage.getFlag());
            inSequence(sequence);
            oneOf(output).putInt(sentMessage.getSession());
            inSequence(sequence);
            oneOf(output).putInt(sentMessage.getRequest());
            inSequence(sequence);
            oneOf(output).putInt(sentMessage.getBodyLength());
            inSequence(sequence);
            oneOf(output).put(sentMessage.getBody());
            inSequence(sequence);
        }});

        RexProMessage.write(output, sentMessage);

        this.mockery.assertIsSatisfied();
    }
}
