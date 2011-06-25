package com.tinkerpop.rexster.protocol;

import junit.framework.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;

public class ScriptResponseMessageTest {
    private final UUID sessionKey = UUID.randomUUID();

    @Test
    public void constructEmptyConstructorEnsureFormat() throws IOException {

        ScriptResponseMessage msg = new ScriptResponseMessage(this.sessionKey,
                ScriptResponseMessage.FLAG_COMPLETE_MESSAGE, "test".getBytes());

        Assert.assertEquals(sessionKey, msg.getSessionAsUUID());
        Assert.assertTrue(msg.hasSession());
        Assert.assertEquals((byte) 0, msg.getFlag());
        Assert.assertEquals(4, msg.getBodyLength());
        Assert.assertEquals(MessageType.SCRIPT_RESPONSE, msg.getType());
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructCopyRexProMessageWrongType() throws IOException {
        RexProMessage msgToConvert = new ScriptResponseMessage(this.sessionKey,
                ScriptResponseMessage.FLAG_COMPLETE_MESSAGE, "test".getBytes());
        msgToConvert.setType(MessageType.SESSION_RESPONSE);

        new SessionRequestMessage(msgToConvert);
    }

    @Test
    public void constructCopyRexProMessage() throws IOException {
        RexProMessage msgToConvert = new ScriptResponseMessage(this.sessionKey,
                ScriptResponseMessage.FLAG_COMPLETE_MESSAGE, "test".getBytes());
        RexProMessage convertedMsg = new ScriptResponseMessage(msgToConvert);

        Assert.assertNotNull(convertedMsg);
        Assert.assertTrue(Arrays.equals(msgToConvert.getSession(), convertedMsg.getSession()));
        Assert.assertTrue(Arrays.equals(msgToConvert.getHeader(), convertedMsg.getHeader()));
        Assert.assertTrue(Arrays.equals(msgToConvert.getRequest(), convertedMsg.getRequest()));
        Assert.assertTrue(Arrays.equals(msgToConvert.getBody(), convertedMsg.getBody()));
        Assert.assertEquals(msgToConvert.getBodyLength(), convertedMsg.getBodyLength());
        Assert.assertEquals(msgToConvert.getFlag(), convertedMsg.getFlag());
        Assert.assertEquals(msgToConvert.getType(), convertedMsg.getType());
        Assert.assertEquals(msgToConvert.getVersion(), convertedMsg.getVersion());
    }
}
