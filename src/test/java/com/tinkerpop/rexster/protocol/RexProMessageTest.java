package com.tinkerpop.rexster.protocol;

import junit.framework.Assert;
import org.glassfish.grizzly.Buffer;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.Sequence;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Test;
import sun.reflect.generics.tree.ByteSignature;

import java.util.UUID;

public class RexProMessageTest {
    private Mockery mockery = new JUnit4Mockery();

    @Test
    public void readValidateBinary() {
        this.mockery = new JUnit4Mockery();

        final Buffer sourceBuffer = this.mockery.mock(Buffer.class);
        final Sequence sequence = this.mockery.sequence("sequence");
        final UUID request = UUID.randomUUID();
        final UUID session = UUID.randomUUID();

        final byte[] sessionBytes = BitWorks.convertUUIDToByteArray(session);
        final byte[] requestBytes = BitWorks.convertUUIDToByteArray(request);

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
            byte[] bytesSession = new byte[16];
            oneOf(sourceBuffer).get(with(any(bytesSession.getClass())));
            inSequence(sequence);

            // request
            byte[] bytesRequest = new byte[16];
            oneOf(sourceBuffer).get(with(any(bytesRequest.getClass())));
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
        Assert.assertEquals(5, msg.getBodyLength());
        Assert.assertEquals(5, msg.getBody().length);
    }

    @Test
    public void writeValidateBinary() {
        this.mockery = new JUnit4Mockery();

        final Buffer output = this.mockery.mock(Buffer.class);
        final Sequence sequence = this.mockery.sequence("sequence");

        final UUID request = UUID.randomUUID();
        final UUID session = UUID.randomUUID();

        final RexProMessage sentMessage = new RexProMessage((byte) 1, (byte) 1, (byte) 1,
                BitWorks.convertUUIDToByteArray(session),
                BitWorks.convertUUIDToByteArray(request),
                "hello".getBytes());

        this.mockery.checking(new Expectations() {{
            oneOf(output).put(sentMessage.getHeader());
            inSequence(sequence);
            oneOf(output).put(sentMessage.getVersion());
            inSequence(sequence);
            oneOf(output).put(sentMessage.getType());
            inSequence(sequence);
            oneOf(output).put(sentMessage.getFlag());
            inSequence(sequence);
            oneOf(output).put(sentMessage.getSession());
            inSequence(sequence);
            oneOf(output).put(sentMessage.getRequest());
            inSequence(sequence);
            oneOf(output).putInt(sentMessage.getBodyLength());
            inSequence(sequence);
            oneOf(output).put(sentMessage.getBody());
            inSequence(sequence);
        }});

        RexProMessage.write(output, sentMessage);

        this.mockery.assertIsSatisfied();
    }

    @Test
    public void hasSessionNoBytes() {
        RexProMessage msg = new RexProMessage();
        Assert.assertFalse(msg.hasSession());
    }

    @Test
    public void hasSessionWrongBytes() {
        RexProMessage msg = new RexProMessage();
        msg.setSession(new byte[5]);
        Assert.assertFalse(msg.hasSession());
    }

    @Test
    public void hasSessionEmptySession() {
        RexProMessage msg = new RexProMessage();
        msg.setSession(BitWorks.convertUUIDToByteArray(RexProMessage.EMPTY_SESSION));
        Assert.assertFalse(msg.hasSession());
    }

    @Test
    public void hasSessionValid() {
        RexProMessage msg = new RexProMessage();
        msg.setSession(BitWorks.convertUUIDToByteArray(UUID.randomUUID()));
        Assert.assertTrue(msg.hasSession());
    }
}
