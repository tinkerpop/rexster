package com.tinkerpop.rexster.protocol.msg;

import com.tinkerpop.rexster.protocol.BitWorks;
import junit.framework.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class MessageUtilTest {
    @Test
    public void createErrorResponse() {
        byte [] request = "request".getBytes();
        byte [] session = "session".getBytes();
        final ErrorResponseMessage msg = MessageUtil.createErrorResponse(request, session,
                MessageFlag.ERROR_AUTHENTICATION_FAILURE, "error message");

        Assert.assertNotNull(msg);
        Assert.assertEquals(request, msg.Request);
        Assert.assertEquals(session, msg.Session);
        Assert.assertEquals("error message", msg.ErrorMessage);
        Assert.assertEquals(MessageFlag.ERROR_AUTHENTICATION_FAILURE, msg.Flag);
    }

    @Test
    public void createNewSession() {
        byte [] request = "request".getBytes();
        final SessionResponseMessage msg = MessageUtil.createNewSession(request, new ArrayList<String>() {{ add("groovy"); }});

        Assert.assertNotNull(msg);
        Assert.assertEquals(request, msg.Request);
        Assert.assertEquals(MessageFlag.SESSION_RESPONSE_NO_FLAG, msg.Flag);
        Assert.assertEquals("groovy", msg.Languages[0]);

        final SessionResponseMessage newMsg = MessageUtil.createNewSession(request, new ArrayList<String>() {{ add("groovy"); }});
        Assert.assertFalse(Arrays.equals(newMsg.Session, msg.Session));
    }

    @Test
    public void createEmptySession() {
        byte [] request = "request".getBytes();
        final SessionResponseMessage msg = MessageUtil.createEmptySession(request);

        Assert.assertNotNull(msg);
        Assert.assertEquals(request, msg.Request);
        Assert.assertEquals(MessageFlag.SESSION_RESPONSE_NO_FLAG, msg.Flag);
        Assert.assertEquals(0, msg.Languages.length);
        Assert.assertTrue(Arrays.equals(BitWorks.convertUUIDToByteArray(RexProMessage.EMPTY_SESSION), msg.Session));
    }
}
