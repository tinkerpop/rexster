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
public class SessionRequestMessageTest {
    @Test
    public void estimateMessageSize() {
        final SessionRequestMessage msg = new SessionRequestMessage();
        msg.Channel = SessionRequestMessage.CHANNEL_CONSOLE;
        msg.Username = "user";
        msg.Password = "pass";

        Assert.assertEquals(45, msg.estimateMessageSize());
    }

    @Test
    public void testMetaValidation() {
        SessionRequestMessage msg = new SessionRequestMessage();
        msg.Version = 0;
        msg.setRequestAsUUID(UUID.randomUUID());
        msg.setSessionAsUUID(UUID.randomUUID());
        msg.metaSetKillSession(true);
        msg.Channel = 0;
        msg.Username = "mr test";
        msg.Password = "password";

        //this should work
        try {
            msg.validateMetaData();
        } catch (RexProException ex) {
            Assert.fail();
        }

        //these should fail
        try {
            msg.Meta.put(SessionRequestMessage.KILL_SESSION_META_KEY, 5);
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

        SessionRequestMessage outMsg = new SessionRequestMessage();
        outMsg.Version = 0;
        outMsg.setRequestAsUUID(UUID.randomUUID());
        outMsg.setSessionAsUUID(UUID.randomUUID());
        outMsg.metaSetKillSession(true);
        outMsg.Channel = 0;
        outMsg.Username = "mr test";
        outMsg.Password = "password";

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

        SessionRequestMessage inMsg;
        try {
            inMsg = unpacker.read(SessionRequestMessage.class);
            Assert.assertEquals(outMsg.Meta, inMsg.Meta);
            Assert.assertEquals(UUID.nameUUIDFromBytes(outMsg.Request), UUID.nameUUIDFromBytes(inMsg.Request));
            Assert.assertEquals(UUID.nameUUIDFromBytes(outMsg.Session), UUID.nameUUIDFromBytes(inMsg.Session));
            Assert.assertEquals(outMsg.Channel, inMsg.Channel);
            Assert.assertEquals(outMsg.Username, inMsg.Username);
            Assert.assertEquals(outMsg.Password, inMsg.Password);
        } catch (IOException ex) {
            Assert.fail();
        }
    }
}
