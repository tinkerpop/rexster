package com.tinkerpop.rexster.protocol.msg;

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
import java.util.Arrays;
import java.util.UUID;

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class SessionResponseMessageTest {

    @Test
    public void testSerialization() {
        MessagePack msgpack = new MessagePack();
        msgpack.register(RexProMessageMeta.class, MetaTemplate.getInstance());
        msgpack.register(RexProBindings.class, RexProBindings.SerializationTemplate.getInstance());
        msgpack.register(RexProScriptResult.class, ResultsTemplate.getInstance());

        SessionResponseMessage outMsg = new SessionResponseMessage();
        outMsg.setRequestAsUUID(UUID.randomUUID());
        outMsg.setSessionAsUUID(UUID.randomUUID());
        outMsg.Languages = new String[3];
        outMsg.Languages[0] = "groovy";
        outMsg.Languages[1] = "scala";
        outMsg.Languages[2] = "python";

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

        SessionResponseMessage inMsg;
        try {
            inMsg = unpacker.read(SessionResponseMessage.class);
            Assert.assertEquals(outMsg.Meta, inMsg.Meta);
            Assert.assertEquals(UUID.nameUUIDFromBytes(outMsg.Request), UUID.nameUUIDFromBytes(inMsg.Request));
            Assert.assertEquals(UUID.nameUUIDFromBytes(outMsg.Session), UUID.nameUUIDFromBytes(inMsg.Session));
            Assert.assertTrue(Arrays.deepEquals(outMsg.Languages, inMsg.Languages));
        } catch (IOException ex) {
            Assert.fail();
        }
    }
}
