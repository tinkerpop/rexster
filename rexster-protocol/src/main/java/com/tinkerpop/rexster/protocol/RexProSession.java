package com.tinkerpop.rexster.protocol;

import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.rexster.Tokens;
import com.tinkerpop.rexster.client.RexProException;
import com.tinkerpop.rexster.protocol.filter.ScriptFilter;
import com.tinkerpop.rexster.protocol.msg.RexProChannel;
import com.tinkerpop.rexster.protocol.msg.RexProMessage;
import com.tinkerpop.rexster.protocol.msg.ScriptRequestMessage;
import com.tinkerpop.rexster.server.RexsterApplication;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Server-side rexster session.  All requests to a session are bound to a specific thread.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class RexProSession {

    private final Bindings bindings = new SimpleBindings();

    private final String sessionKey;

    private final int channel;

    private final EngineController controller = EngineController.getInstance();

    private Date lastTimeUsed = new Date();

    private final RexsterApplication rexsterApplication;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    //the graph bound to this session
    private Graph graphObj = null;

    //the variable name of the graph in the interperter
    private String graphObjName = null;

    public RexProSession(final String sessionKey, final RexsterApplication rexsterApplication, final int channel) {
        this.sessionKey = sessionKey;
        this.channel = channel;
        this.bindings.put(Tokens.REXPRO_REXSTER_CONTEXT, new RexsterApplicationHolder(rexsterApplication));
        this.rexsterApplication = rexsterApplication;
    }

    /**
     * Configures a graph object on the session, and sets the variable name of
     * the graph in the interpreter
     *
     * @param graphName: the name of the graph (in rexster.xml)
     * @param graphObjName: the variable name of the graph in the interpreter (usually "g")
     */
    public void setGraphObj(final String graphName, final String graphObjName) throws RexProException{
        graphObj = rexsterApplication.getGraph(graphName);
        if (graphObj == null) {
            throw new RexProException("the graph '" + graphName + "' was not found by Rexster");
        }
        this.graphObjName = graphObjName;
        bindings.put(this.graphObjName, graphObj);
    }

    public Graph getGraphObj() {
        return graphObj;
    }

    public Boolean hasGraphObj() {
        return graphObj != null;
    }

    public String getSessionKey() {
        return this.sessionKey;
    }

    public Bindings getBindings() {
        return this.bindings;
    }

    public int getChannel() {
        return this.channel;
    }

    public long getIdleTime() {
        return (new Date()).getTime() - this.lastTimeUsed.getTime();
    }

    public void kill() {
        this.executor.shutdown();
    }

    public byte[] evaluate(final ScriptRequestMessage msg, final Bindings bindings) throws ScriptException {

        try {
            final EngineHolder engine = this.controller.getEngineByLanguageName(msg.LanguageName);

            Future<byte[]> future;
            if (msg.metaGetIsolate()) {
                //use separate bindings
                Bindings tempBindings = new SimpleBindings();
                tempBindings.putAll(this.bindings);

                if (bindings != null) {
                    tempBindings.putAll(bindings);
                }

                future = this.executor.submit(new Evaluator(engine.getEngine(), this, msg, tempBindings, getGraphObj()));
            } else {
                if (bindings != null) {
                    this.bindings.putAll(bindings);
                }
                future = this.executor.submit(new Evaluator(engine.getEngine(), this, msg, this.bindings, getGraphObj()));
            }

            return future.get();
        } catch (Exception e) {
            // attempt to abort the transaction across all graphs since a new thread will be created on the next request.
            // don't want transactions lingering about, though this seems like a brute force way to deal with it.
            for (String graphName : this.rexsterApplication.getGraphNames()) {
                try {
                    final Graph g = this.rexsterApplication.getGraph(graphName);
                    if (g instanceof TransactionalGraph) {
                        ((TransactionalGraph) g).stopTransaction(TransactionalGraph.Conclusion.FAILURE);
                    }
                } catch (Throwable t) { }
            }

            throw new ScriptException(e);
        } finally {
            this.lastTimeUsed = new Date();
        }
    }

    private class Evaluator implements Callable<byte[]> {
        private ScriptEngine engine;
        private RexProSession session;
        private final ScriptRequestMessage msg;
        private final Bindings bindings;
        private final Graph graph;

        public Evaluator(final ScriptEngine engine, RexProSession session, final ScriptRequestMessage msg, final Bindings bindings, final Graph graph) {
            this.engine = engine;
            this.session = session;
            this.msg = msg;
            this.bindings = bindings;
            this.graph = graph;
        }

        @Override
        public byte[] call() throws Exception {
            //open a transaction if applicable
            if ((graph instanceof TransactionalGraph) && msg.metaGetTransaction()) {
                ((TransactionalGraph) graph).stopTransaction(TransactionalGraph.Conclusion.FAILURE);
            }

            try {
                //execute the script
                final Object result = this.engine.eval(msg.Script, bindings);

                //create and serialize the rexpro message
                RexProMessage resultMessage = null;
                if (session.getChannel() == RexProChannel.CHANNEL_CONSOLE) {
                    resultMessage = ScriptFilter.formatForConsoleChannel(msg, session, result);

                } else if (session.getChannel() == RexProChannel.CHANNEL_MSGPACK) {
                    resultMessage = ScriptFilter.formatForMsgPackChannel(msg, session, result);

                } else if (session.getChannel() == RexProChannel.CHANNEL_GRAPHSON) {
                    resultMessage = ScriptFilter.formatForGraphSONChannel(msg, session, result);
                }
                //serialize before closing the transaction and result objects go out of scope
                byte[] bytes = RexProMessage.serialize(resultMessage);

                //close the transaction if applicable
                if ((graph instanceof TransactionalGraph) && msg.metaGetTransaction()) {
                    ((TransactionalGraph) graph).stopTransaction(TransactionalGraph.Conclusion.SUCCESS);
                }

                return bytes;
            } catch (Exception ex) {
                if ((graph instanceof TransactionalGraph) && msg.metaGetTransaction()) {
                    ((TransactionalGraph) graph).stopTransaction(TransactionalGraph.Conclusion.FAILURE);
                }
                throw new ScriptException(ex);
            }
        }
    }
}
