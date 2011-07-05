package com.tinkerpop.rexster.protocol;

import junit.framework.Assert;
import org.junit.Test;

import java.util.Arrays;

public class SessionRequestMessageTest {
    @Test
    public void constructEmptyConstructorEnsureFormat() {
        RexProMessage msg = new com.tinkerpop.rexster.protocol.message.SessionRequestMessage();

        Assert.assertEquals(RexProMessage.EMPTY_SESSION, msg.getSessionAsUUID());

        // the session is empty for a session request
        Assert.assertFalse(msg.hasSession());
        Assert.assertEquals((byte) 0, msg.getFlag());
        Assert.assertEquals(0, msg.getBodyLength());
        Assert.assertEquals(MessageType.SESSION_REQUEST, msg.getType());
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructCopyRexProMessageWrongType() {
        RexProMessage msgToConvert = new com.tinkerpop.rexster.protocol.message.SessionRequestMessage();
        msgToConvert.setType(MessageType.SESSION_RESPONSE);

        new com.tinkerpop.rexster.protocol.message.SessionRequestMessage(msgToConvert);
    }

    @Test
    public void constructCopyRexProMessage() {
        RexProMessage msgToConvert = new com.tinkerpop.rexster.protocol.message.SessionRequestMessage();
        RexProMessage convertedMsg = new com.tinkerpop.rexster.protocol.message.SessionRequestMessage(msgToConvert);

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
