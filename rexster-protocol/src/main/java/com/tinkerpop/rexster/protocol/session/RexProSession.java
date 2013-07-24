package com.tinkerpop.rexster.protocol.session;

import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.rexster.server.RexsterApplication;

import javax.script.ScriptException;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Server-side rexster session.  All requests to a session are bound to a specific thread.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 * @author Blake Eggleston (bdeggleston.github.com)
 */
public class RexProSession extends AbstractRexProSession{

    private final String sessionKey;

    private Date lastTimeUsed = new Date();

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public RexProSession(final String sessionKey, final RexsterApplication rexsterApplication) {
        super(rexsterApplication);
        this.sessionKey = sessionKey;
    }

    public void kill() {
        this.executor.shutdown();
    }

    public long getIdleTime() {
        return (new Date()).getTime() - this.lastTimeUsed.getTime();
    }

    @Override
    protected void execute(Evaluator evaluator) throws ScriptException {

        try {
            //execute request in the same thread the session was created on
            this.executor.submit(evaluator).get();

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

}
