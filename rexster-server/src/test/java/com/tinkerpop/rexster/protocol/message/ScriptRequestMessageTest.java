package com.tinkerpop.rexster.protocol.message;

import com.tinkerpop.rexster.protocol.RexsterBindings;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;

public class ScriptRequestMessageTest {
    private final UUID sessionKey = UUID.randomUUID();
    private final RexsterBindings bindings = new RexsterBindings();

    @Test
    public void constructEmptyConstructorEnsureFormat() throws IOException {

        ScriptRequestMessage msg = new com.tinkerpop.rexster.protocol.message.ScriptRequestMessage(this.sessionKey, "language", bindings, "x=y;");

        Assert.assertEquals(sessionKey, msg.getSessionAsUUID());
        Assert.assertTrue(msg.hasSession());
        Assert.assertEquals((byte) 0, msg.getFlag());
        Assert.assertEquals(20, msg.getBodyLength());
        Assert.assertEquals(MessageType.SCRIPT_REQUEST, msg.getType());
    }

    @Test
    public void getLanguageValid() throws IOException {
        com.tinkerpop.rexster.protocol.message.ScriptRequestMessage msg = new com.tinkerpop.rexster.protocol.message.ScriptRequestMessage(this.sessionKey, "language", bindings, "x=y;");

        Assert.assertEquals("language", msg.getLanguageName());
    }

    @Test
    public void getScriptValid() throws IOException {
        com.tinkerpop.rexster.protocol.message.ScriptRequestMessage msg = new com.tinkerpop.rexster.protocol.message.ScriptRequestMessage(this.sessionKey, "language", bindings, "x=y;");

        Assert.assertEquals("x=y;", msg.getScript());
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructCopyRexProMessageWrongType() throws IOException {
        RexProMessage msgToConvert = new com.tinkerpop.rexster.protocol.message.ScriptRequestMessage(this.sessionKey, "language", bindings, "x=y;");
        msgToConvert.setType(MessageType.SESSION_RESPONSE);

        new com.tinkerpop.rexster.protocol.message.SessionRequestMessage(msgToConvert);
    }

    @Test
    public void constructCopyRexProMessage() throws IOException {
        RexProMessage msgToConvert = new com.tinkerpop.rexster.protocol.message.ScriptRequestMessage(this.sessionKey, "language", bindings, "x=y;");
        RexProMessage convertedMsg = new com.tinkerpop.rexster.protocol.message.ScriptRequestMessage(msgToConvert);

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
