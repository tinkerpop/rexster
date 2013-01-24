package com.tinkerpop.rexster.protocol.msg;

import com.tinkerpop.rexster.protocol.BitWorks;
import junit.framework.Assert;
import org.junit.Test;
import org.msgpack.MessagePack;
import org.msgpack.unpacker.Unpacker;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class MsgPackScriptResponseMessageTest {
    @Test
    public void estimateMessageSize() {
        final MsgPackScriptResponseMessage msg = new MsgPackScriptResponseMessage();
        msg.Results = new byte[10];
        msg.Bindings = new byte[10];

        Assert.assertEquals(56, msg.estimateMessageSize());
    }

    final static MessagePack msgpack = new MessagePack();

    @Test
    public void serializeString() throws Exception {
        byte[] bytes = MsgPackScriptResponseMessage.convertResultToBytes("xyz");
        Unpacker unpacker = msgpack.createUnpacker(new ByteArrayInputStream(bytes));
        Object dst = BitWorks.deserializeObject(unpacker.readValue());

        Assert.assertEquals("xyz", dst);
    }

    @Test
    public void serializeInt() throws Exception {
        byte[] bytes = MsgPackScriptResponseMessage.convertResultToBytes(31);
        Unpacker unpacker = msgpack.createUnpacker(new ByteArrayInputStream(bytes));
        Object dst = BitWorks.deserializeObject(unpacker.readValue());

        Assert.assertEquals(31, dst);
    }

    @Test
    public void serializeFloat() throws Exception {
        byte[] bytes = MsgPackScriptResponseMessage.convertResultToBytes(1.2);
        Unpacker unpacker = msgpack.createUnpacker(new ByteArrayInputStream(bytes));
        Object dst = BitWorks.deserializeObject(unpacker.readValue());

        Assert.assertEquals(1.2, dst);
    }

    @Test
    public void serializeBool() throws Exception {
        byte[] bytes = MsgPackScriptResponseMessage.convertResultToBytes(true);
        Unpacker unpacker = msgpack.createUnpacker(new ByteArrayInputStream(bytes));
        Object dst = BitWorks.deserializeObject(unpacker.readValue());

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
        Object dst = BitWorks.deserializeObject(unpacker.readValue());

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
        Object dst = BitWorks.deserializeObject(unpacker.readValue());
        for (Object key : srcMap.keySet()) {
            Assert.assertEquals(srcMap.get(key), ((HashMap<Object, Object>) dst).get(key));
        }
    }

}
