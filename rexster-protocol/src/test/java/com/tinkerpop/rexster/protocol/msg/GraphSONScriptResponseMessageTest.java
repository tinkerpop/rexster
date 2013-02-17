package com.tinkerpop.rexster.protocol.msg;

import junit.framework.Assert;
import org.junit.Test;

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class GraphSONScriptResponseMessageTest {
    @Test
    public void estimateMessageSize() {
        final GraphSONScriptResponseMessage msg = new GraphSONScriptResponseMessage();
        msg.Results = "1234567890";
        msg.Bindings = new byte[10];

        Assert.assertEquals(56, msg.estimateMessageSize());
    }
}
