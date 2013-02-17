package com.tinkerpop.rexster.protocol.msg;

import junit.framework.Assert;
import org.junit.Test;
import org.msgpack.MessagePack;
import org.msgpack.packer.Packer;
import org.msgpack.unpacker.Unpacker;

import javax.script.Bindings;
import javax.script.SimpleBindings;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class ConsoleScriptResponseMessageTest {

    @Test
    public void consoleLinesAsListValid() {
        final ConsoleScriptResponseMessage msg = new ConsoleScriptResponseMessage();
        msg.ConsoleLines = new String[] { "a", "b", "c" };

        final List<String> lines = msg.consoleLinesAsList();
        Assert.assertEquals(3, lines.size());
        Assert.assertEquals("a", lines.get(0));
        Assert.assertEquals("b", lines.get(1));
        Assert.assertEquals("c", lines.get(2));
    }

    @Test
    public void bindingsAsListValid() throws IOException {
        final ConsoleScriptResponseMessage msg = new ConsoleScriptResponseMessage();
        final Bindings b = new SimpleBindings();
        b.put("a", "aaa");
        b.put("b", "bbb");
        b.put("c", 3);

        msg.Bindings = ConsoleScriptResponseMessage.convertBindingsToConsoleLineByteArray(b);

        final List<String> bindingsList = msg.bindingsAsList();
        Assert.assertEquals(3, bindingsList.size());
        Assert.assertEquals("a=aaa", bindingsList.get(2));
        Assert.assertEquals("b=bbb", bindingsList.get(0));
        Assert.assertEquals("c=3", bindingsList.get(1));
    }

    @Test
    public void estimateMessageSize() throws IOException {
        final ConsoleScriptResponseMessage msg = new ConsoleScriptResponseMessage();
        msg.ConsoleLines = new String[] { "a", "b", "c" };

        final Bindings b = new SimpleBindings();
        b.put("a", "aaa");
        b.put("b", "bbb");
        b.put("c", 3);

        msg.Bindings = ConsoleScriptResponseMessage.convertBindingsToConsoleLineByteArray(b);

        Assert.assertEquals(64, msg.estimateMessageSize());
    }

    @Test
    public void testSerialization() {
        MessagePack msgpack = new MessagePack();
        msgpack.register(RexProMessageMeta.class, RexProMessageMeta.SerializationTemplate.getInstance());

        ConsoleScriptResponseMessage outMsg = new ConsoleScriptResponseMessage();
        outMsg.setRequestAsUUID(UUID.randomUUID());
        outMsg.setSessionAsUUID(UUID.randomUUID());
        outMsg.ConsoleLines = new String[2];
        outMsg.ConsoleLines[0] = "a";
        outMsg.ConsoleLines[1] = "b";
        outMsg.Bindings = new byte[1];
        outMsg.Bindings[0] = 1;

        final ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        final Packer packer = msgpack.createPacker(outStream);
        try {
            packer.write(outMsg);
            packer.close();
        } catch (IOException ex) {
            Assert.fail();
        }

        byte[] bytes = outStream.toByteArray();

        final ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        final Unpacker unpacker = msgpack.createUnpacker(in);

        ConsoleScriptResponseMessage inMsg;
        try {
            inMsg = unpacker.read(ConsoleScriptResponseMessage.class);
            Assert.assertEquals(outMsg.Meta, inMsg.Meta);
            Assert.assertEquals(UUID.nameUUIDFromBytes(outMsg.Request), UUID.nameUUIDFromBytes(inMsg.Request));
            Assert.assertEquals(UUID.nameUUIDFromBytes(outMsg.Session), UUID.nameUUIDFromBytes(inMsg.Session));
            Assert.assertTrue(Arrays.deepEquals(outMsg.ConsoleLines, inMsg.ConsoleLines));
            Assert.assertEquals(outMsg.Bindings[0], inMsg.Bindings[0]);
        } catch (IOException ex) {
            Assert.fail();
        }
    }
}
