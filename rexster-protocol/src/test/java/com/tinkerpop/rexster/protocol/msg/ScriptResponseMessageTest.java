package com.tinkerpop.rexster.protocol.msg;

import com.tinkerpop.rexster.protocol.serializer.msgpack.templates.MetaTemplate;
import com.tinkerpop.rexster.protocol.serializer.msgpack.templates.ResultsConverter;
import com.tinkerpop.rexster.protocol.serializer.msgpack.templates.ResultsTemplate;
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
public class ScriptResponseMessageTest {

    final static MessagePack msgpack = new MessagePack();

    @Test
    public void serializeString() throws Exception {
        byte[] bytes = ScriptResponseMessage.convertResultToBytes("xyz");
        Unpacker unpacker = msgpack.createUnpacker(new ByteArrayInputStream(bytes));
        Object dst = ResultsConverter.deserializeObject(unpacker.readValue());

        Assert.assertEquals("xyz", dst);
    }

    @Test
    public void serializeInt() throws Exception {
        byte[] bytes = ScriptResponseMessage.convertResultToBytes(31);
        Unpacker unpacker = msgpack.createUnpacker(new ByteArrayInputStream(bytes));
        Object dst = ResultsConverter.deserializeObject(unpacker.readValue());

        Assert.assertEquals(31L, dst);
    }

    @Test
    public void serializeFloat() throws Exception {
        byte[] bytes = ScriptResponseMessage.convertResultToBytes(1.2);
        Unpacker unpacker = msgpack.createUnpacker(new ByteArrayInputStream(bytes));
        Object dst = ResultsConverter.deserializeObject(unpacker.readValue());

        Assert.assertEquals(1.2, dst);
    }

    @Test
    public void serializeBool() throws Exception {
        byte[] bytes = ScriptResponseMessage.convertResultToBytes(true);
        Unpacker unpacker = msgpack.createUnpacker(new ByteArrayInputStream(bytes));
        Object dst = ResultsConverter.deserializeObject(unpacker.readValue());

        Assert.assertEquals(true, dst);
    }

    @Test
    public void serializeArray() throws Exception {
        ArrayList<Object> srcArray = new ArrayList<Object>();
        srcArray.add(true);
        srcArray.add("abc");
        srcArray.add(1L);

        byte[] bytes = ScriptResponseMessage.convertResultToBytes(srcArray);
        Unpacker unpacker = msgpack.createUnpacker(new ByteArrayInputStream(bytes));
        Object dst = ResultsConverter.deserializeObject(unpacker.readValue());

        Assert.assertEquals(srcArray, dst);
    }

    @Test
    public void serializeMap() throws Exception {
        HashMap<Object, Object> srcMap = new HashMap<Object, Object>();
        srcMap.put("city", "LA");
        srcMap.put(1L, 2L);
        ArrayList<Object> arr = new ArrayList<Object>();
        arr.add(1L);
        arr.add("str");
        arr.add(true);
        srcMap.put(1.2d, arr);

        byte[] bytes = ScriptResponseMessage.convertResultToBytes(srcMap);
        Unpacker unpacker = msgpack.createUnpacker(new ByteArrayInputStream(bytes));
        Object dst = ResultsConverter.deserializeObject(unpacker.readValue());
        for (Object key : srcMap.keySet()) {
            Assert.assertEquals(srcMap.get(key), ((HashMap<Object, Object>) dst).get(key));
        }
    }

    @Test
    public void testSerialization() {
        MessagePack msgpack = new MessagePack();
        msgpack.register(RexProMessageMeta.class, MetaTemplate.getInstance());
        msgpack.register(RexProBindings.class, RexProBindings.SerializationTemplate.getInstance());
        msgpack.register(RexProScriptResult.class, ResultsTemplate.getInstance());

        ScriptResponseMessage outMsg = new ScriptResponseMessage();
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

        ScriptResponseMessage inMsg;
        try {
            inMsg = unpacker.read(ScriptResponseMessage.class);
            Assert.assertEquals(outMsg.Meta, inMsg.Meta);
            Assert.assertEquals(UUID.nameUUIDFromBytes(outMsg.Request), UUID.nameUUIDFromBytes(inMsg.Request));
            Assert.assertEquals(UUID.nameUUIDFromBytes(outMsg.Session), UUID.nameUUIDFromBytes(inMsg.Session));
            Assert.assertEquals(inMsg.Results.get(), 5L);
            Assert.assertEquals(inMsg.Bindings.get("something"), "or other");
        } catch (IOException ex) {
            Assert.fail();
        }
    }

}
