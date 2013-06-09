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
public class ErrorResponseMessageTest {

    @Test
    public void testMetaValidation() {
        ErrorResponseMessage msg = new ErrorResponseMessage();
        msg.setRequestAsUUID(UUID.randomUUID());
        msg.setSessionAsUUID(UUID.randomUUID());
        msg.metaSetFlag(ErrorResponseMessage.INVALID_SESSION_ERROR);
        msg.ErrorMessage = "brokenness";

        //these should work
        try {
            msg.metaSetFlag(ErrorResponseMessage.INVALID_MESSAGE_ERROR);
            msg.validateMetaData();

            msg.metaSetFlag(ErrorResponseMessage.INVALID_SESSION_ERROR);
            msg.validateMetaData();

            msg.metaSetFlag(ErrorResponseMessage.SCRIPT_FAILURE_ERROR);
            msg.validateMetaData();

            msg.metaSetFlag(ErrorResponseMessage.AUTH_FAILURE_ERROR);
            msg.validateMetaData();

        } catch (RexProException ex) {
            Assert.fail();
        }

        //these should fail
        try {
            msg.Meta.put(ErrorResponseMessage.FLAG_META_KEY, "session");
            msg.validateMetaData();
        } catch (RexProException ex) {
            //exception is expected
        }
        try {
            msg.Meta.put(ErrorResponseMessage.FLAG_META_KEY, false);
            msg.validateMetaData();
        } catch (RexProException ex) {
            //exception is expected
        }
        try {
            msg.Meta.put(ErrorResponseMessage.FLAG_META_KEY, 3.14f);
            msg.validateMetaData();
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

        ErrorResponseMessage outMsg = new ErrorResponseMessage();
        outMsg.setRequestAsUUID(UUID.randomUUID());
        outMsg.setSessionAsUUID(UUID.randomUUID());
        outMsg.metaSetFlag(ErrorResponseMessage.INVALID_SESSION_ERROR);
        outMsg.ErrorMessage = "brokenness";

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

        ErrorResponseMessage inMsg;
        try {
            inMsg = unpacker.read(ErrorResponseMessage.class);
            Assert.assertEquals(outMsg.Meta, inMsg.Meta);
            Assert.assertEquals(UUID.nameUUIDFromBytes(outMsg.Request), UUID.nameUUIDFromBytes(inMsg.Request));
            Assert.assertEquals(UUID.nameUUIDFromBytes(outMsg.Session), UUID.nameUUIDFromBytes(inMsg.Session));
            Assert.assertEquals(outMsg.ErrorMessage, inMsg.ErrorMessage);
        } catch (IOException ex) {
            Assert.fail();
        }
    }
}
