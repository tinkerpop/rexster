package com.tinkerpop.rexster.gremlin;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.script.ScriptEngine;

import org.apache.log4j.Logger;

import com.tinkerpop.rexster.server.RexsterApplication;

/**
 * A wrapper thread for a given gremlin instance. Webadmin spawns one of these
 * threads for each client that uses the gremlin console.
 * <p/>
 * Credit to Neo Technology (http://neotechnology.com/) for most of the code related to the
 * Gremlin in Rexster.  Specifically, this code was borrowed from
 * https://github.com/neo4j/webadmin and re-purposed for Rexster's needs.
 * <p/>
 * Original author Jacob Hansson <jacob@voltvoodoo.com>
 */
public class GremlinSession {
    private static final Logger logger = Logger.getLogger(GremlinSession.class);

    private static final ExecutorService EXECUTOR;

    public static final int MAX_COMMANDS_WAITING = 128;

    static {
        int nThreads = Runtime.getRuntime().availableProcessors() * 2;
        EXECUTOR = Executors.newFixedThreadPool(nThreads);
    }

    /**
     * Keep track of the last time this was used.
     */
    protected volatile Date lastTimeUsed = new Date();

    /**
     * Lock used to manage the scriptEngine field.
     */
    private final Object scriptEngineLock = new Object();

    /**
     * The gremlin evaluator instance beeing wrapped.
     */
    protected volatile ScriptEngine scriptEngine;

    private final String graphName;

    private final RexsterApplication ra;

    public GremlinSession(final String graphName, final RexsterApplication ra) {
        this.graphName = graphName;
        this.ra = ra;
    }

    /**
     * Take some gremlin script, evaluate it in the context of this gremlin
     * session, and return the result.
     *
     * @param script
     * @param scriptTimeoutMillis
     * @return
     */
    public GremlinEvaluationJob evaluate(final String script, final long scriptTimeoutMillis) {
        GremlinEvaluationJob fail = new GremlinEvaluationJob(script);
        synchronized (scriptEngineLock) {
            if (scriptEngine == null) {
                Map<String, Object> context = new HashMap<String, Object>();
                context.put("g", this.ra.getApplicationGraph(this.graphName).getGraph());
                scriptEngine = com.tinkerpop.rexster.gremlin.GremlinFactory.createGremlinScriptEngine(context);
            }
        }
        Future<GremlinEvaluationJob> jobFuture = EXECUTOR.submit(new GremlinEvaluationJobCallable(scriptEngine, scriptEngineLock, script));
        try {
            lastTimeUsed = new Date();
            return jobFuture.get(scriptTimeoutMillis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            logger.error("Interrupted in script execution", e);
            fail.setResult(e);
            Thread.currentThread().interrupt();
            return fail;
        } catch (ExecutionException e) {
            logger.error("Execution Exception in script evaluation", e);
            fail.setResult(e);
            return fail;
        } catch (TimeoutException e) {
            logger.error("Timeout Exception in script evaluation", e);
            fail.setResult(e);
            return fail;
        }
    }

    /**
     * Destroy the internal gremlin evaluator and replace it with a clean slate.
     */
    public synchronized void reset() {
        synchronized (scriptEngineLock) {
            this.scriptEngine = null;
        }
    }

    /**
     * Get the number of milliseconds this worker has been idle.
     */
    public long getIdleTime() {
        return (new Date()).getTime() - lastTimeUsed.getTime();
    }

    public void die() {
        // nop
    }
}
