package com.tinkerpop.rexster.protocol.msg;

import com.tinkerpop.rexster.protocol.MsgPackConverter;
import junit.framework.Assert;
import org.junit.Test;
import org.msgpack.MessagePack;
import org.msgpack.packer.Packer;
import org.msgpack.unpacker.Unpacker;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class MsgPackScriptResponseMessageTest {
    @Test
    public void estimateMessageSize() {
        final MsgPackScriptResponseMessage msg = new MsgPackScriptResponseMessage();

        Assert.assertEquals(32, msg.estimateMessageSize());
    }

    final static MessagePack msgpack = new MessagePack();

    @Test
    public void serializeString() throws Exception {
        byte[] bytes = MsgPackScriptResponseMessage.convertResultToBytes("xyz");
        Unpacker unpacker = msgpack.createUnpacker(new ByteArrayInputStream(bytes));
        Object dst = MsgPackConverter.deserializeObject(unpacker.readValue());

        Assert.assertEquals("xyz", dst);
    }

    @Test
    public void serializeInt() throws Exception {
        byte[] bytes = MsgPackScriptResponseMessage.convertResultToBytes(31);
        Unpacker unpacker = msgpack.createUnpacker(new ByteArrayInputStream(bytes));
        Object dst = MsgPackConverter.deserializeObject(unpacker.readValue());

        Assert.assertEquals(31, dst);
    }

    @Test
    public void serializeFloat() throws Exception {
        byte[] bytes = MsgPackScriptResponseMessage.convertResultToBytes(1.2);
        Unpacker unpacker = msgpack.createUnpacker(new ByteArrayInputStream(bytes));
        Object dst = MsgPackConverter.deserializeObject(unpacker.readValue());

        Assert.assertEquals(1.2, dst);
    }

    @Test
    public void serializeBool() throws Exception {
        byte[] bytes = MsgPackScriptResponseMessage.convertResultToBytes(true);
        Unpacker unpacker = msgpack.createUnpacker(new ByteArrayInputStream(bytes));
        Object dst = MsgPackConverter.deserializeObject(unpacker.readValue());

        Assert.assertEquals(true, dst);
    }

    @Test
    public void serializeArray() throws Exception {
        ArrayList<Object> srcArray = new ArrayList<Object>();
        srcArray.add(true);
        srcArray.add("abc");
        srcArray.add(1);

        byte[] bytes = MsgPackScriptResponseMessage.convertResultToBytes(srcArray);
        Unpacker unpacker = msgpack.createUnpacker(new ByteArrayInputStream(bytes));
        Object dst = MsgPackConverter.deserializeObject(unpacker.readValue());

        Assert.assertEquals(srcArray, dst);
    }

    @Test
    public void serializeMap() throws Exception {
        HashMap<Object, Object> srcMap = new HashMap<Object, Object>();
        srcMap.put("city", "LA");
        srcMap.put(1, 2);
        ArrayList<Object> arr = new ArrayList<Object>();
        arr.add(1);
        arr.add("str");
        arr.add(true);
        srcMap.put(1.2d, arr);

        byte[] bytes = MsgPackScriptResponseMessage.convertResultToBytes(srcMap);
        Unpacker unpacker = msgpack.createUnpacker(new ByteArrayInputStream(bytes));
        Object dst = MsgPackConverter.deserializeObject(unpacker.readValue());
        for (Object key : srcMap.keySet()) {
            Assert.assertEquals(srcMap.get(key), ((HashMap<Object, Object>) dst).get(key));
        }
    }

    @Test
    public void testSerialization() {
        MessagePack msgpack = new MessagePack();
        msgpack.register(RexProMessageMeta.class, RexProMessageMeta.SerializationTemplate.getInstance());
        msgpack.register(RexProBindings.class, RexProBindings.SerializationTemplate.getInstance());
        msgpack.register(RexProScriptResult.class, RexProScriptResult.SerializationTemplate.getInstance());

        MsgPackScriptResponseMessage outMsg = new MsgPackScriptResponseMessage();
        outMsg.setRequestAsUUID(UUID.randomUUID());
        outMsg.setSessionAsUUID(UUID.randomUUID());
        outMsg.Results.set(5);
        outMsg.Bindings.put("something", "or other");

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

        MsgPackScriptResponseMessage inMsg;
        try {
            inMsg = unpacker.read(MsgPackScriptResponseMessage.class);
            Assert.assertEquals(outMsg.Meta, inMsg.Meta);
            Assert.assertEquals(UUID.nameUUIDFromBytes(outMsg.Request), UUID.nameUUIDFromBytes(inMsg.Request));
            Assert.assertEquals(UUID.nameUUIDFromBytes(outMsg.Session), UUID.nameUUIDFromBytes(inMsg.Session));
            Assert.assertEquals(inMsg.Results.get(), 5);
            Assert.assertEquals(inMsg.Bindings.get("something"), "or other");
        } catch (IOException ex) {
            Assert.fail();
        }
    }

}
