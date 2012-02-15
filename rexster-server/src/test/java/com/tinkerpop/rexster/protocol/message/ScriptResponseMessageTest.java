package com.tinkerpop.rexster.protocol.message;

import com.tinkerpop.rexster.protocol.RexsterBindings;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;

public class ScriptResponseMessageTest {
    private final UUID sessionKey = UUID.randomUUID();

    @Test
    public void constructEmptyConstructorEnsureFormat() throws IOException {

        ScriptResponseMessage msg = new com.tinkerpop.rexster.protocol.message.ScriptResponseMessage(this.sessionKey,
                com.tinkerpop.rexster.protocol.message.ScriptResponseMessage.FLAG_COMPLETE_MESSAGE, "test".getBytes(), new RexsterBindings());

        Assert.assertEquals(sessionKey, msg.getSessionAsUUID());
        Assert.assertTrue(msg.hasSession());
        Assert.assertEquals((byte) 0, msg.getFlag());
        //Assert.assertEquals(8, msg.getBodyLength());
        Assert.assertEquals(MessageType.SCRIPT_RESPONSE, msg.getType());
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructCopyRexProMessageWrongType() throws IOException {
        RexProMessage msgToConvert = new com.tinkerpop.rexster.protocol.message.ScriptResponseMessage(this.sessionKey,
                com.tinkerpop.rexster.protocol.message.ScriptResponseMessage.FLAG_COMPLETE_MESSAGE, "test".getBytes(), new RexsterBindings());
        msgToConvert.setType(MessageType.SESSION_RESPONSE);

        new com.tinkerpop.rexster.protocol.message.SessionRequestMessage(msgToConvert);
    }

    @Test
    public void constructCopyRexProMessage() throws IOException {
        RexProMessage msgToConvert = new com.tinkerpop.rexster.protocol.message.ScriptResponseMessage(this.sessionKey,
                com.tinkerpop.rexster.protocol.message.ScriptResponseMessage.FLAG_COMPLETE_MESSAGE, "test".getBytes(), new RexsterBindings());
        RexProMessage convertedMsg = new com.tinkerpop.rexster.protocol.message.ScriptResponseMessage(msgToConvert);

        Assert.assertNotNull(convertedMsg);
        Assert.assertTrue(Arrays.equals(msgToConvert.getSession(), convertedMsg.getSession()));
        Assert.assertTrue(Arrays.equals(msgToConvert.getHeaderIdentification(), convertedMsg.getHeaderIdentification()));
        Assert.assertTrue(Arrays.equals(msgToConvert.getRequest(), convertedMsg.getRequest()));
        Assert.assertTrue(Arrays.equals(msgToConvert.getBody(), convertedMsg.getBody()));
        Assert.assertEquals(msgToConvert.getBodyLength(), convertedMsg.getBodyLength());
        Assert.assertEquals(msgToConvert.getFlag(), convertedMsg.getFlag());
        Assert.assertEquals(msgToConvert.getType(), convertedMsg.getType());
        Assert.assertEquals(msgToConvert.getVersion(), convertedMsg.getVersion());
    }
}
