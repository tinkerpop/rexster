package com.tinkerpop.rexster.protocol.msg;

import junit.framework.Assert;
import org.junit.Test;

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class ScriptRequestMessageTest {

    @Test
    public void estimateMessageSize() {
        final ScriptRequestMessage msg = new ScriptRequestMessage();
        msg.Bindings = new byte[10];
        msg.LanguageName = "groovy";
        msg.Script = "script";

        Assert.assertEquals(58, msg.estimateMessageSize());
    }
}
