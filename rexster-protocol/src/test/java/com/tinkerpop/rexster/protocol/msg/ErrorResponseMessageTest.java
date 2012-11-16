package com.tinkerpop.rexster.protocol.msg;

import junit.framework.Assert;
import org.junit.Test;

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class ErrorResponseMessageTest {

    @Test
    public void estimateMessageSize(){
        final ErrorResponseMessage msg = new ErrorResponseMessage();
        msg.ErrorMessage = "this was an error";

        Assert.assertEquals(53, msg.estimateMessageSize());
    }
}
