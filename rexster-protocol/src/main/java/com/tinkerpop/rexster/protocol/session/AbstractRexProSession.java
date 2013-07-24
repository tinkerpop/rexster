package com.tinkerpop.rexster.protocol.session;

import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.rexster.Tokens;
import com.tinkerpop.rexster.client.RexProException;
import com.tinkerpop.rexster.protocol.EngineController;
import com.tinkerpop.rexster.protocol.RexsterApplicationHolder;
import com.tinkerpop.rexster.protocol.server.RexProRequest;
import com.tinkerpop.rexster.protocol.server.ScriptServer;
import com.tinkerpop.rexster.server.RexsterApplication;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import java.util.concurrent.Callable;

/**
 * Server-side rexster session.  All requests to a session are bound to a specific thread.
 *
 * @author Blake Eggleston (bdeggleston.github.com)
 */
public abstract class AbstractRexProSession {
    protected final Bindings bindings = new SimpleBindings();

    protected final EngineController controller = EngineController.getInstance();

    protected final RexsterApplication rexsterApplication;

    //the graph bound to this session
    protected Graph graphObj = null;

    //the variable name of the graph in the interperter
    protected String graphObjName = null;

    public AbstractRexProSession(final RexsterApplication rexsterApplication) {
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

    public Bindings getBindings() {
        return this.bindings;
    }

    public void evaluate(final String script, final String languageName, final Bindings requestBindings, final Boolean isolate,
                         final Boolean inTransaction, final Graph graph, final RexProRequest request) throws ScriptException {

        final ScriptEngine engine = this.controller.getEngineByLanguageName(languageName).getEngine();

        //setup the bindings for the request
        Bindings executorBindings;
        if (isolate) {
            executorBindings = new SimpleBindings();
            executorBindings.putAll(this.bindings);
        } else {
            executorBindings = this.bindings;
        }
        if (requestBindings != null) executorBindings.putAll(requestBindings);

        //execute request in the same thread the session was created on
        execute(new Evaluator(engine, script, executorBindings, inTransaction, graph, request));
    }

    protected abstract void execute(Evaluator evaluator) throws ScriptException;

    protected class Evaluator implements Callable {
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
