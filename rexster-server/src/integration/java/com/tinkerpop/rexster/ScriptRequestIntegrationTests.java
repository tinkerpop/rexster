package com.tinkerpop.rexster;

import com.tinkerpop.rexster.client.RexsterClient;
import com.tinkerpop.rexster.client.RexsterClientFactory;
import com.tinkerpop.rexster.protocol.BitWorks;
import com.tinkerpop.rexster.protocol.msg.ErrorResponseMessage;
import com.tinkerpop.rexster.protocol.msg.MsgPackScriptResponseMessage;
import com.tinkerpop.rexster.protocol.msg.RexProChannel;
import com.tinkerpop.rexster.protocol.msg.RexProMessage;
import com.tinkerpop.rexster.protocol.msg.ScriptRequestMessage;
import com.tinkerpop.rexster.protocol.msg.SessionRequestMessage;
import com.tinkerpop.rexster.protocol.msg.SessionResponseMessage;
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
        final RexsterClient client = RexsterClientFactory.open();

        final ScriptRequestMessage scriptMessage = new ScriptRequestMessage();
        scriptMessage.Script = "graph.addVertex()";
        scriptMessage.LanguageName = "groovy";
        scriptMessage.metaSetInSession(false);
        scriptMessage.metaSetGraphName("emptygraph");
        scriptMessage.metaSetGraphObjName("graph");
        scriptMessage.setRequestAsUUID(UUID.randomUUID());
        scriptMessage.Session = null;

        RexProMessage inMsg = client.execute(scriptMessage);
        Assert.assertTrue(inMsg instanceof MsgPackScriptResponseMessage);
        Assert.assertTrue(((MsgPackScriptResponseMessage) inMsg).Results.get() != null);
    }

    /**
     * Tests that parameterized bindings from one request aren't available in subsequent ones
     */
    @Test
    public void testBindingsDontStickAroundAfterRequests() throws Exception {
        final RexsterClient client = RexsterClientFactory.open();

        //create a session
        final SessionRequestMessage outMsg = new SessionRequestMessage();
        outMsg.Channel = RexProChannel.CHANNEL_MSGPACK;
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
        scriptMessage.Bindings.put("o", 5);
        scriptMessage.LanguageName = "groovy";
        scriptMessage.metaSetInSession(true);
        scriptMessage.setRequestAsUUID(UUID.randomUUID());
        scriptMessage.setSessionAsUUID(sessionKey);

        inMsg = client.execute(scriptMessage);
        Assert.assertTrue(inMsg instanceof MsgPackScriptResponseMessage);
        Assert.assertTrue(((MsgPackScriptResponseMessage) inMsg).Results.get() != null);

        final ScriptRequestMessage scriptMessage2 = new ScriptRequestMessage();
        scriptMessage2.Script = "o";
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
    public void testGraphObjMetaOnSessionedRequest() throws Exception {
        final RexsterClient client = RexsterClientFactory.open();
        RexProMessage inMsg;

        //create a session
        final SessionRequestMessage outMsg = new SessionRequestMessage();
        outMsg.Channel = RexProChannel.CHANNEL_MSGPACK;
        outMsg.setRequestAsUUID(UUID.randomUUID());

        inMsg = client.execute(outMsg);
        Assert.assertNotNull(inMsg.Session);
        Assert.assertTrue(inMsg instanceof SessionResponseMessage);

        UUID sessionKey = BitWorks.convertByteArrayToUUID(inMsg.Session);

        //test that it works
        final ScriptRequestMessage scriptMessage = new ScriptRequestMessage();
        scriptMessage.Script = "g.addVertex()";
        scriptMessage.LanguageName = "groovy";
        scriptMessage.metaSetInSession(true);
        scriptMessage.metaSetGraphName("emptygraph");
        scriptMessage.setRequestAsUUID(UUID.randomUUID());
        scriptMessage.Session = BitWorks.convertUUIDToByteArray(sessionKey);

        inMsg = client.execute(scriptMessage);
        Assert.assertTrue(inMsg instanceof MsgPackScriptResponseMessage);
        Assert.assertTrue(((MsgPackScriptResponseMessage) inMsg).Results.get() != null);

        // test that it's not available on the next request
        // if the meta flag is not set
        final ScriptRequestMessage scriptMessage2 = new ScriptRequestMessage();
        scriptMessage2.Script = "g.addVertex()";
        scriptMessage2.LanguageName = "groovy";
        scriptMessage2.metaSetInSession(true);
        scriptMessage2.setRequestAsUUID(UUID.randomUUID());
        scriptMessage2.Session = BitWorks.convertUUIDToByteArray(sessionKey);

        inMsg = client.execute(scriptMessage2);
        Assert.assertTrue(inMsg instanceof ErrorResponseMessage);
        Assert.assertEquals(((ErrorResponseMessage) inMsg).metaGetFlag(), ErrorResponseMessage.SCRIPT_FAILURE_ERROR);

    }

    @Test
    public void testGraphObjMetaOnSessionWithExistingGraphObjFails() throws Exception {
        final RexsterClient client = RexsterClientFactory.open();

        //create a session
        final SessionRequestMessage outMsg = new SessionRequestMessage();
        outMsg.Channel = RexProChannel.CHANNEL_MSGPACK;
        outMsg.setRequestAsUUID(UUID.randomUUID());
        outMsg.metaSetGraphName("emptygraph");

        RexProMessage inMsg = client.execute(outMsg);
        Assert.assertNotNull(inMsg.Session);
        Assert.assertTrue(inMsg instanceof SessionResponseMessage);

        UUID sessionKey = BitWorks.convertByteArrayToUUID(inMsg.Session);

        //try defining a new graph object
        final ScriptRequestMessage scriptMessage = new ScriptRequestMessage();
        scriptMessage.Script = "graph.addVertex()";
        scriptMessage.LanguageName = "groovy";
        scriptMessage.metaSetInSession(true);
        scriptMessage.metaSetGraphName("emptygraph");
        scriptMessage.setRequestAsUUID(UUID.randomUUID());
        scriptMessage.Session = BitWorks.convertUUIDToByteArray(sessionKey);

        inMsg = client.execute(scriptMessage);
        Assert.assertNotNull(inMsg.Session);
        Assert.assertTrue(inMsg instanceof ErrorResponseMessage);
        Assert.assertEquals(ErrorResponseMessage.GRAPH_CONFIG_ERROR, ((ErrorResponseMessage) inMsg).metaGetFlag());

    }

    @Test
    public void testChannelChangeFails() throws Exception {
        final RexsterClient client = RexsterClientFactory.open();

        //create a session
        final SessionRequestMessage outMsg = new SessionRequestMessage();
        outMsg.Channel = RexProChannel.CHANNEL_MSGPACK;
        outMsg.setRequestAsUUID(UUID.randomUUID());

        RexProMessage inMsg = client.execute(outMsg);
        Assert.assertNotNull(inMsg.Session);
        Assert.assertTrue(inMsg instanceof SessionResponseMessage);

        UUID sessionKey = BitWorks.convertByteArrayToUUID(inMsg.Session);

        //try defining a new channel different from the session
        final ScriptRequestMessage scriptMessage = new ScriptRequestMessage();
        scriptMessage.Script = "graph.addVertex()";
        scriptMessage.LanguageName = "groovy";
        scriptMessage.metaSetInSession(true);
        scriptMessage.metaSetGraphName("emptygraph");
        scriptMessage.metaSetChannel(RexProChannel.CHANNEL_GRAPHSON);
        scriptMessage.setRequestAsUUID(UUID.randomUUID());
        scriptMessage.Session = BitWorks.convertUUIDToByteArray(sessionKey);

        inMsg = client.execute(scriptMessage);
        Assert.assertNotNull(inMsg.Session);
        Assert.assertTrue(inMsg instanceof ErrorResponseMessage);
        Assert.assertEquals(ErrorResponseMessage.CHANNEL_CONFIG_ERROR, ((ErrorResponseMessage) inMsg).metaGetFlag());

    }

    @Test
    public void testDefiningNonExistentGraphNameFails() throws Exception {
        final RexsterClient client = RexsterClientFactory.open();

        final ScriptRequestMessage scriptMessage = new ScriptRequestMessage();
        scriptMessage.Script = "graph.addVertex()";
        scriptMessage.LanguageName = "groovy";
        scriptMessage.metaSetInSession(false);
        scriptMessage.metaSetGraphName("undefined");
        scriptMessage.metaSetGraphObjName("graph");
        scriptMessage.setRequestAsUUID(UUID.randomUUID());
        scriptMessage.Session = null;

        RexProMessage inMsg = client.execute(scriptMessage);
        Assert.assertTrue(inMsg instanceof ErrorResponseMessage);
        Assert.assertEquals(((ErrorResponseMessage) inMsg).metaGetFlag(), ErrorResponseMessage.GRAPH_CONFIG_ERROR);
    }

    /**
     * Tests that variables introduced in one query are not available in the next
     */
    @Test
    public void testQueryIsolation() throws Exception {
        final RexsterClient client = RexsterClientFactory.open();
        RexProMessage inMsg;

        //create a session
        final SessionRequestMessage outMsg = new SessionRequestMessage();
        outMsg.Channel = RexProChannel.CHANNEL_MSGPACK;
        outMsg.setRequestAsUUID(UUID.randomUUID());

        inMsg = client.execute(outMsg);
        Assert.assertNotNull(inMsg.Session);
        Assert.assertTrue(inMsg instanceof SessionResponseMessage);

        UUID sessionKey = BitWorks.convertByteArrayToUUID(inMsg.Session);

        //test that it works
        final ScriptRequestMessage scriptMessage = new ScriptRequestMessage();
        scriptMessage.Script = "n = 5\nn";
        scriptMessage.LanguageName = "groovy";
        scriptMessage.metaSetInSession(true);
        scriptMessage.setRequestAsUUID(UUID.randomUUID());
        scriptMessage.Session = BitWorks.convertUUIDToByteArray(sessionKey);

        inMsg = client.execute(scriptMessage);
        Assert.assertTrue(inMsg instanceof MsgPackScriptResponseMessage);
        Assert.assertTrue(((MsgPackScriptResponseMessage) inMsg).Results.get() != null);

        // test that 'n' is not available if the isolate meta flag is not set to false
        final ScriptRequestMessage scriptMessage2 = new ScriptRequestMessage();
        scriptMessage2.Script = "m = n + 1";
        scriptMessage2.LanguageName = "groovy";
        scriptMessage2.metaSetInSession(true);
        scriptMessage2.setRequestAsUUID(UUID.randomUUID());
        scriptMessage2.Session = BitWorks.convertUUIDToByteArray(sessionKey);

        inMsg = client.execute(scriptMessage2);
        Assert.assertTrue(inMsg instanceof ErrorResponseMessage);
        Assert.assertEquals(((ErrorResponseMessage) inMsg).metaGetFlag(), ErrorResponseMessage.SCRIPT_FAILURE_ERROR);

    }

    @Test
    public void testDisabledQueryIsolation() throws Exception {
        final RexsterClient client = RexsterClientFactory.open();
        RexProMessage inMsg;

        //create a session
        final SessionRequestMessage outMsg = new SessionRequestMessage();
        outMsg.Channel = RexProChannel.CHANNEL_MSGPACK;
        outMsg.setRequestAsUUID(UUID.randomUUID());

        inMsg = client.execute(outMsg);
        Assert.assertNotNull(inMsg.Session);
        Assert.assertTrue(inMsg instanceof SessionResponseMessage);

        UUID sessionKey = BitWorks.convertByteArrayToUUID(inMsg.Session);

        //test that it works
        final ScriptRequestMessage scriptMessage = new ScriptRequestMessage();
        scriptMessage.Script = "n = 5\nn";
        scriptMessage.LanguageName = "groovy";
        scriptMessage.metaSetInSession(true);
        scriptMessage.metaSetIsolate(false);
        scriptMessage.setRequestAsUUID(UUID.randomUUID());
        scriptMessage.Session = BitWorks.convertUUIDToByteArray(sessionKey);

        inMsg = client.execute(scriptMessage);
        Assert.assertTrue(inMsg instanceof MsgPackScriptResponseMessage);
        Assert.assertTrue(((MsgPackScriptResponseMessage) inMsg).Results.get() != null);

        // test that 'n' is available if the isolate meta flag is set to false
        final ScriptRequestMessage scriptMessage2 = new ScriptRequestMessage();
        scriptMessage2.Script = "m = n + 1";
        scriptMessage2.LanguageName = "groovy";
        scriptMessage2.metaSetInSession(true);
        scriptMessage2.setRequestAsUUID(UUID.randomUUID());
        scriptMessage2.Session = BitWorks.convertUUIDToByteArray(sessionKey);

        inMsg = client.execute(scriptMessage2);
        Assert.assertTrue(inMsg instanceof MsgPackScriptResponseMessage);
        Assert.assertTrue(((MsgPackScriptResponseMessage) inMsg).Results.get() != null);
    }

    @Test
    public void testTransactionMetaFlagWithoutSession() throws Exception {
        final RexsterClient client = RexsterClientFactory.open();
        RexProMessage inMsg;

        //create a session
        final SessionRequestMessage outMsg = new SessionRequestMessage();
        outMsg.Channel = RexProChannel.CHANNEL_MSGPACK;
        outMsg.setRequestAsUUID(UUID.randomUUID());

        inMsg = client.execute(outMsg);
        Assert.assertNotNull(inMsg.Session);
        Assert.assertTrue(inMsg instanceof SessionResponseMessage);

        UUID sessionKey = BitWorks.convertByteArrayToUUID(inMsg.Session);

        //test that it works
        final ScriptRequestMessage scriptMessage = new ScriptRequestMessage();
        scriptMessage.Script = "n = 5\nn";
        scriptMessage.LanguageName = "groovy";
        scriptMessage.metaSetInSession(true);
        scriptMessage.metaSetTransaction(true);
        scriptMessage.setRequestAsUUID(UUID.randomUUID());
        scriptMessage.Session = BitWorks.convertUUIDToByteArray(sessionKey);

        inMsg = client.execute(scriptMessage);
        Assert.assertTrue(inMsg instanceof MsgPackScriptResponseMessage);
        Assert.assertTrue(((MsgPackScriptResponseMessage) inMsg).Results.get() != null);

    }

    @Test
    public void testTransactionMetaFlagWithSession() throws Exception {
        final RexsterClient client = RexsterClientFactory.open();

        final ScriptRequestMessage scriptMessage = new ScriptRequestMessage();
        scriptMessage.Script = "graph.addVertex()";
        scriptMessage.LanguageName = "groovy";
        scriptMessage.metaSetInSession(false);
        scriptMessage.metaSetGraphName("emptygraph");
        scriptMessage.metaSetGraphObjName("graph");
        scriptMessage.metaSetTransaction(true);
        scriptMessage.setRequestAsUUID(UUID.randomUUID());
        scriptMessage.Session = null;

        RexProMessage inMsg = client.execute(scriptMessage);
        Assert.assertTrue(inMsg instanceof MsgPackScriptResponseMessage);
        Assert.assertTrue(((MsgPackScriptResponseMessage) inMsg).Results.get() != null);
    }

}
