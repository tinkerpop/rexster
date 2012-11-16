package com.tinkerpop.rexster.protocol.msg;

import junit.framework.Assert;
import org.junit.Test;

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
}
