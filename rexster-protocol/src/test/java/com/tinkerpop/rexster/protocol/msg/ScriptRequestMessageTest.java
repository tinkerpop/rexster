package com.tinkerpop.rexster.protocol.msg;

import com.tinkerpop.rexster.client.RexProException;
import com.tinkerpop.rexster.protocol.serializer.msgpack.templates.MetaTemplate;
import com.tinkerpop.rexster.protocol.serializer.msgpack.templates.ResultsTemplate;
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
    public void testMetaValidation() {
        final ScriptRequestMessage msg = new ScriptRequestMessage();
        msg.setRequestAsUUID(UUID.randomUUID());
        msg.setSessionAsUUID(UUID.randomUUID());
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
            msg.Meta.put(ScriptRequestMessage.META_KEY_IN_SESSION, 5);
            msg.validateMetaData();
            Assert.fail();
        } catch (RexProException ex) {
            //exception is expected
        }

        //these should fail
        try {
            msg.Meta.put(ScriptRequestMessage.META_KEY_IN_SESSION, "yup");
            msg.validateMetaData();
            Assert.fail();
        } catch (RexProException ex) {
            //exception is expected
        }
    }

    @Test
    public void testSerialization() {
        MessagePack msgpack = new MessagePack();
        msgpack.register(RexProMessageMeta.class, MetaTemplate.getInstance());
        msgpack.register(RexProBindings.class, RexProBindings.SerializationTemplate.getInstance());
        msgpack.register(RexProScriptResult.class, ResultsTemplate.getInstance());

        final ScriptRequestMessage outMsg = new ScriptRequestMessage();
        outMsg.setRequestAsUUID(UUID.randomUUID());
        outMsg.setSessionAsUUID(UUID.randomUUID());
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
            Assert.assertEquals(outMsg.LanguageName, inMsg.LanguageName);
            Assert.assertEquals(outMsg.Script, inMsg.Script);
        } catch (IOException ex) {
            Assert.fail();
        }
    }
}
