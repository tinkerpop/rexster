package com.tinkerpop.rexster;

import com.tinkerpop.rexster.client.RexsterClient;
import com.tinkerpop.rexster.protocol.BitWorks;
import com.tinkerpop.rexster.protocol.msg.*;
import junit.framework.Assert;
import org.junit.Test;

import javax.script.SimpleBindings;
import java.util.UUID;

/**
 * Tests that the SessionFilter is behaving as we'd like it to
 *
 * @author Blake Eggleston (bdeggleston.github.com)
 */
public class SessionRequestMessageTests extends AbstractRexProIntegrationTest {

    /**
     * Tests that requests to create and destroy sessions work as expected
     */
    @Test
    public void testSessionRequestAndResponse() throws Exception {
        final RexsterClient client = factory.createClient();

        //create a session
        final SessionRequestMessage outMsg = new SessionRequestMessage();
        outMsg.Channel = SessionRequestMessage.CHANNEL_MSGPACK;
        outMsg.setRequestAsUUID(UUID.randomUUID());

        RexProMessage inMsg = client.execute(outMsg);
        Assert.assertNotNull(inMsg.Session);
        Assert.assertTrue(inMsg instanceof SessionResponseMessage);

        UUID sessionKey = BitWorks.convertByteArrayToUUID(inMsg.Session);

        //kill said session
        final SessionRequestMessage deathMsg = new SessionRequestMessage();
        deathMsg.Channel = SessionRequestMessage.CHANNEL_NONE;
        deathMsg.Session = BitWorks.convertUUIDToByteArray(sessionKey);
        deathMsg.setRequestAsUUID(UUID.randomUUID());
        deathMsg.metaSetKillSession(true);

        inMsg = client.execute(deathMsg);
        Assert.assertNotNull(inMsg.Session);
        Assert.assertTrue(inMsg instanceof SessionResponseMessage);

        //try to use the killed session
        final ScriptRequestMessage scriptMessage = new ScriptRequestMessage();
        scriptMessage.Script = "5";
        scriptMessage.Bindings = new byte[0];
        scriptMessage.LanguageName = "groovy";
        scriptMessage.metaSetInSession(true);
        scriptMessage.setRequestAsUUID(UUID.randomUUID());
        scriptMessage.Session = BitWorks.convertUUIDToByteArray(sessionKey);

        inMsg = client.execute(scriptMessage);
        Assert.assertTrue(inMsg instanceof ErrorResponseMessage);
        Assert.assertEquals(((ErrorResponseMessage) inMsg).metaGetFlag(), ErrorResponseMessage.INVALID_SESSION_ERROR);
    }

    /**
     * Tests defining graph objects while opening a session
     */
    @Test
    public void testSessionGraphDefinition() throws Exception {
        final RexsterClient client = factory.createClient();

        //create a session
        final SessionRequestMessage outMsg = new SessionRequestMessage();
        outMsg.Channel = SessionRequestMessage.CHANNEL_MSGPACK;
        outMsg.setRequestAsUUID(UUID.randomUUID());
        outMsg.metaSetGraphName("emptygraph");
        outMsg.metaSetGraphObjName("graph");

        RexProMessage inMsg = client.execute(outMsg);
        Assert.assertNotNull(inMsg.Session);
        Assert.assertTrue(inMsg instanceof SessionResponseMessage);

        UUID sessionKey = BitWorks.convertByteArrayToUUID(inMsg.Session);

        //try to use the graph on the session
        final ScriptRequestMessage scriptMessage = new ScriptRequestMessage();
        scriptMessage.Script = "graph.addVertex()";
        scriptMessage.Bindings = BitWorks.convertBindingsToByteArray(new SimpleBindings());
        scriptMessage.LanguageName = "groovy";
        scriptMessage.metaSetInSession(true);
        scriptMessage.setRequestAsUUID(UUID.randomUUID());
        scriptMessage.Session = BitWorks.convertUUIDToByteArray(sessionKey);

        inMsg = client.execute(scriptMessage);
        Assert.assertTrue(inMsg instanceof MsgPackScriptResponseMessage);
        Assert.assertTrue(((MsgPackScriptResponseMessage) inMsg).Results.length > 0);
    }

    /**
     * Tests that attempting to define graph objects with graph names
     * that don't exist returns an error response
     */
    @Test
    public void testDefiningNonExistentGraphNameFails() throws Exception {
        final RexsterClient client = factory.createClient();

        //create a session
        final SessionRequestMessage outMsg = new SessionRequestMessage();
        outMsg.Channel = SessionRequestMessage.CHANNEL_MSGPACK;
        outMsg.setRequestAsUUID(UUID.randomUUID());
        outMsg.metaSetGraphName("undefined");

        RexProMessage inMsg = client.execute(outMsg);
        Assert.assertNotNull(inMsg.Session);
        Assert.assertTrue(inMsg instanceof ErrorResponseMessage);
        Assert.assertEquals(((ErrorResponseMessage) inMsg).metaGetFlag(), ErrorResponseMessage.GRAPH_CONFIG_ERROR);

    }

}
