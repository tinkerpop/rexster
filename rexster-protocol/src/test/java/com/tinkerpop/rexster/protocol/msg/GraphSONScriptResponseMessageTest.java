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

        Assert.assertEquals(42, msg.estimateMessageSize());
    }
}
