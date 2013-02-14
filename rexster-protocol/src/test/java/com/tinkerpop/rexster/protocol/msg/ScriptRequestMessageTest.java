package com.tinkerpop.rexster.protocol.msg;

import com.tinkerpop.rexster.client.RexProException;
import junit.framework.Assert;
import org.junit.Test;
import org.msgpack.MessagePack;
import org.msgpack.packer.Packer;
import org.msgpack.unpacker.Unpacker;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class ScriptRequestMessageTest {

    @Test
    public void estimateMessageSize() {
        final ScriptRequestMessage msg = new ScriptRequestMessage();
        msg.Bindings = new byte[10];
        msg.LanguageName = "groovy";
        msg.Script = "script";

        Assert.assertEquals(58, msg.estimateMessageSize());
    }

    @Test
    public void testMetaValidation() {
        final ScriptRequestMessage msg = new ScriptRequestMessage();
        msg.Version = 0;
        msg.setRequestAsUUID(UUID.randomUUID());
        msg.setSessionAsUUID(UUID.randomUUID());
        msg.Bindings = new byte[10];
        for(int i=0; i<10; i++) msg.Bindings[i] = (byte)i;
        msg.LanguageName = "groovy";
        msg.Script = "script";

        //these should work
        try {
            msg.metaSetInSession(true);
            msg.validateMetaData();
            msg.metaSetInSession(false);
            msg.validateMetaData();
        } catch (RexProException ex) {
            Assert.fail();
        }

        //these should fail
        try {
            msg.Meta.put(ScriptRequestMessage.IN_SESSION_META_KEY, 5);
            msg.validateMetaData();
            Assert.fail();
        } catch (RexProException ex) {
            //exception is expected
        }

        //these should fail
        try {
            msg.Meta.put(ScriptRequestMessage.IN_SESSION_META_KEY, "yup");
            msg.validateMetaData();
            Assert.fail();
        } catch (RexProException ex) {
            //exception is expected
        }
    }

    @Test
    public void testSerialization() {
        MessagePack msgpack = new MessagePack();
        msgpack.register(RexProMessageMeta.class, RexProMessageMeta.SerializationTemplate.getInstance());

        final ScriptRequestMessage outMsg = new ScriptRequestMessage();
        outMsg.Version = 0;
        outMsg.setRequestAsUUID(UUID.randomUUID());
        outMsg.setSessionAsUUID(UUID.randomUUID());
        outMsg.Bindings = new byte[10];
        for(int i=0; i<10; i++) outMsg.Bindings[i] = (byte)i;
        outMsg.LanguageName = "groovy";
        outMsg.Script = "script";

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

        ScriptRequestMessage inMsg;
        try {
            inMsg = unpacker.read(ScriptRequestMessage.class);
            Assert.assertEquals(outMsg.Meta, inMsg.Meta);
            Assert.assertEquals(UUID.nameUUIDFromBytes(outMsg.Request), UUID.nameUUIDFromBytes(inMsg.Request));
            Assert.assertEquals(UUID.nameUUIDFromBytes(outMsg.Session), UUID.nameUUIDFromBytes(inMsg.Session));
            for (int i=0; i<10; i++) Assert.assertEquals(outMsg.Bindings[i], inMsg.Bindings[i]);
            Assert.assertEquals(outMsg.LanguageName, inMsg.LanguageName);
            Assert.assertEquals(outMsg.Script, inMsg.Script);
        } catch (IOException ex) {
            Assert.fail();
        }
    }
}
