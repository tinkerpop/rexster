package com.tinkerpop.rexster;

import com.tinkerpop.rexster.client.RexsterClient;
import com.tinkerpop.rexster.protocol.BitWorks;
import com.tinkerpop.rexster.protocol.msg.*;
import junit.framework.Assert;
import org.junit.Test;

import javax.script.SimpleBindings;
import java.util.UUID;

/**
 * Tests that the ScriptFilter is behaving as we'd like it to
 *
 * @author Blake Eggleston (bdeggleston.github.com)
 */
public class ScriptRequestIntegrationTests extends AbstractRexProIntegrationTest {

    @Test
    public void testGraphObjMetaOnSessionlessRequest() throws Exception {
        final RexsterClient client = factory.createClient();

        final ScriptRequestMessage scriptMessage = new ScriptRequestMessage();
        scriptMessage.Script = "graph.addVertex()";
        scriptMessage.Bindings = BitWorks.convertBindingsToByteArray(new SimpleBindings());
        scriptMessage.LanguageName = "groovy";
        scriptMessage.metaSetInSession(false);
        scriptMessage.metaSetGraphName("emptygraph");
        scriptMessage.metaSetGraphObjName("graph");
        scriptMessage.setRequestAsUUID(UUID.randomUUID());
        scriptMessage.Session = null;

        RexProMessage inMsg = client.execute(scriptMessage);
        Assert.assertTrue(inMsg instanceof MsgPackScriptResponseMessage);
        Assert.assertTrue(((MsgPackScriptResponseMessage) inMsg).Results.length > 0);
    }

    /**
     * Tests that parameterized bindings from one request aren't available in subsequent ones
     */
    @Test
    public void testBindingsDontStickAroundAfterRequests() throws Exception {
        final RexsterClient client = factory.createClient();

        //create a session
        final SessionRequestMessage outMsg = new SessionRequestMessage();
        outMsg.Channel = SessionRequestMessage.CHANNEL_MSGPACK;
        outMsg.setRequestAsUUID(UUID.randomUUID());
        outMsg.metaSetGraphName("emptygraph");

        RexProMessage inMsg = client.execute(outMsg);
        Assert.assertNotNull(inMsg.Session);
        Assert.assertTrue(inMsg instanceof SessionResponseMessage);

        UUID sessionKey = BitWorks.convertByteArrayToUUID(inMsg.Session);

        SimpleBindings b = new SimpleBindings();
        b.put("o", 5);

        final ScriptRequestMessage scriptMessage = new ScriptRequestMessage();
        scriptMessage.Script = "o";
        scriptMessage.Bindings = BitWorks.convertBindingsToByteArray(b);
        scriptMessage.LanguageName = "groovy";
        scriptMessage.metaSetInSession(true);
        scriptMessage.setRequestAsUUID(UUID.randomUUID());
        scriptMessage.setSessionAsUUID(sessionKey);

        inMsg = client.execute(scriptMessage);
        Assert.assertTrue(inMsg instanceof MsgPackScriptResponseMessage);
        Assert.assertTrue(((MsgPackScriptResponseMessage) inMsg).Results.length > 0);

        final ScriptRequestMessage scriptMessage2 = new ScriptRequestMessage();
        scriptMessage2.Script = "o";
        scriptMessage2.Bindings = BitWorks.convertBindingsToByteArray(new SimpleBindings());
        scriptMessage2.LanguageName = "groovy";
        scriptMessage2.metaSetInSession(true);
        scriptMessage2.setRequestAsUUID(UUID.randomUUID());
        scriptMessage2.setSessionAsUUID(sessionKey);

        inMsg = client.execute(scriptMessage2);
        Assert.assertNotNull(inMsg.Session);
        Assert.assertTrue(inMsg instanceof ErrorResponseMessage);
        Assert.assertEquals(((ErrorResponseMessage) inMsg).metaGetFlag(), ErrorResponseMessage.SCRIPT_FAILURE_ERROR);
    }

    @Test
    public void testGraphObjMetaOnSessionedRequest() {

        //test that it works

        // test that it's not available on the next request
        // if the meta flag is not set

    }

    @Test
    public void testGraphObjMetaOnSessionWithExistingGraphObjFails() throws Exception {
        final RexsterClient client = factory.createClient();

        //create a session
        final SessionRequestMessage outMsg = new SessionRequestMessage();
        outMsg.Channel = SessionRequestMessage.CHANNEL_MSGPACK;
        outMsg.setRequestAsUUID(UUID.randomUUID());
        outMsg.metaSetGraphName("emptygraph");

        RexProMessage inMsg = client.execute(outMsg);
        Assert.assertNotNull(inMsg.Session);
        Assert.assertTrue(inMsg instanceof SessionResponseMessage);

        UUID sessionKey = BitWorks.convertByteArrayToUUID(inMsg.Session);

        //try defining a new graph object
        final ScriptRequestMessage scriptMessage = new ScriptRequestMessage();
        scriptMessage.Script = "graph.addVertex()";
        scriptMessage.Bindings = BitWorks.convertBindingsToByteArray(new SimpleBindings());
        scriptMessage.LanguageName = "groovy";
        scriptMessage.metaSetInSession(true);
        scriptMessage.metaSetGraphName("emptygraph");
        scriptMessage.setRequestAsUUID(UUID.randomUUID());
        scriptMessage.Session = BitWorks.convertUUIDToByteArray(sessionKey);

        inMsg = client.execute(outMsg);
        Assert.assertNotNull(inMsg.Session);
        Assert.assertTrue(inMsg instanceof ErrorResponseMessage);
        Assert.assertEquals(((ErrorResponseMessage) inMsg).metaGetFlag(), ErrorResponseMessage.GRAPH_CONFIG_ERROR);

    }

    @Test
    public void testDefiningNonExistentGraphNameFails() throws Exception {

    }

    @Test
    public void testQueryIsolation() {

    }

    @Test
    public void testTransactionMetaFlag() {

    }

}
