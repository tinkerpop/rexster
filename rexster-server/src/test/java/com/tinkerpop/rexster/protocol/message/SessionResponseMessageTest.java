package com.tinkerpop.rexster.protocol.message;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.UUID;

public class SessionResponseMessageTest {

    private UUID sessionKey = UUID.randomUUID();
    private UUID requestKey = UUID.randomUUID();

    @Test
    public void constructEmptyConstructorEnsureFormat() {
        RexProMessage msg = new SessionResponseMessage(sessionKey, requestKey, null);

        Assert.assertEquals(sessionKey, msg.getSessionAsUUID());
        Assert.assertTrue(msg.hasSession());
        Assert.assertEquals((byte) 0, msg.getFlag());
        Assert.assertEquals(0, msg.getBodyLength());
        Assert.assertEquals(MessageType.SESSION_RESPONSE, msg.getType());
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructCopyRexProMessageWrongType() {
        RexProMessage msgToConvert = new SessionResponseMessage(sessionKey, requestKey, null);
        msgToConvert.setType(MessageType.SESSION_REQUEST);

        new com.tinkerpop.rexster.protocol.message.SessionResponseMessage(msgToConvert);
    }

    @Test
    public void constructCopyRexProMessage() {
        RexProMessage msgToConvert = new SessionResponseMessage(sessionKey, requestKey, null);
        RexProMessage convertedMsg = new SessionResponseMessage(msgToConvert);

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
