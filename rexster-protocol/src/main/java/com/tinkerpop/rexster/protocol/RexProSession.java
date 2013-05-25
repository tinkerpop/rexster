package com.tinkerpop.rexster.protocol;

import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.rexster.Tokens;
import com.tinkerpop.rexster.client.RexProException;
import com.tinkerpop.rexster.protocol.server.RexProRequest;
import com.tinkerpop.rexster.protocol.server.ScriptServer;
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

    public void evaluate(final String script, final String languageName, final Bindings rexsterBindings, final Boolean isolate,
                           final Boolean inTransaction, final Graph graph, final RexProRequest request) throws ScriptException {

        try {
            final EngineHolder engine = this.controller.getEngineByLanguageName(languageName);

            Future future;
            if (isolate) {
                //use separate bindings
                Bindings tempBindings = new SimpleBindings();
                tempBindings.putAll(this.bindings);

                if (bindings != null) {
                    tempBindings.putAll(rexsterBindings);
                }

                future = this.executor.submit(
                        new Evaluator(
                                engine.getEngine(), script, tempBindings,
                                inTransaction, graph, request
                        )
                );
            } else {
                if (rexsterBindings != null) {
                    bindings.putAll(rexsterBindings);
                }

                future = this.executor.submit(
                        new Evaluator(
                                engine.getEngine(), script, this.bindings,
                                inTransaction, graph, request
                        )
                );
            }

            future.get();
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

    private class Evaluator implements Callable {
        private ScriptEngine engine;
        private final Bindings bindings;
        private final String script;
        private Boolean inTransaction;
        private Graph graph;
        private RexProRequest request;

        public Evaluator(final ScriptEngine engine, final String script, final Bindings bindings,
                         final Boolean inTransaction, final Graph graph, final RexProRequest request) {
            this.script = script;
            this.engine = engine;
            this.bindings = bindings;
            this.inTransaction = inTransaction;
            this.graph = graph;
            this.request = request;
        }

        @Override
        public Object call() throws Exception {
            if (inTransaction) ScriptServer.tryRollbackTransaction(graph);
            Object result = this.engine.eval(this.script, this.bindings);
            request.writeScriptResults(result);
            if (inTransaction) ScriptServer.tryCommitTransaction(graph);
            return null;
        }
    }
}
