package com.tinkerpop.rexster.protocol.msg;

import junit.framework.Assert;
import org.junit.Test;

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class SessionResponseMessageTest {
    @Test
    public void estimateMessageSize() {
        final SessionResponseMessage msg = new SessionResponseMessage();
        msg.Languages = new String[]{"groovy"};

        Assert.assertEquals(42, msg.estimateMessageSize());
    }
}
