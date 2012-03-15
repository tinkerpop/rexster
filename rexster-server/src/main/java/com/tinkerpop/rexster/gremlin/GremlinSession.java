package com.tinkerpop.rexster.gremlin;

import com.tinkerpop.rexster.RexsterApplication;

import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

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
@SuppressWarnings("restriction")
public class GremlinSession implements Runnable {

    public static final int MAX_COMMANDS_WAITING = 128;

    /**
     * Keep track of the last time this was used.
     */
    protected Date lastTimeUsed = new Date();

    /**
     * The gremlin evaluator instance beeing wrapped.
     */
    protected ScriptEngine scriptEngine;

    /**
     * Commands waiting to be executed. Number of waiting commands is capped,
     * since this is meant to be used by a single client.
     */
    protected BlockingQueue<GremlinEvaluationJob> jobQueue = new ArrayBlockingQueue<GremlinEvaluationJob>(MAX_COMMANDS_WAITING);

    /**
     * Should I shut down?
     */
    protected boolean sepukko = false;

    /**
     * Mama thread.
     */
    protected Thread runner = new Thread(this, "GremlinSession");

    private String graphName;

    private RexsterApplication ra;

    public GremlinSession(String graphName, RexsterApplication ra) {
        this.graphName = graphName;
        this.ra = ra;
        runner.start();
    }

    public void run() {

        GremlinEvaluationJob job;
        try {
            while (true) {
                if (scriptEngine == null) {
                    Map<String, Object> context = new HashMap<String, Object>();
                    context.put("g", this.ra.getApplicationGraph(this.graphName).getGraph());
                    scriptEngine = com.tinkerpop.rexster.gremlin.GremlinFactory
                            .createGremlinScriptEngine(context);
                }

                job = jobQueue.take();
                job.setResult(performEvaluation(job));

                if (sepukko) {
                    break;
                }
            }
        } catch (InterruptedException e) {
            // Exit
        }
    }

    /**
     * Take some gremlin script, evaluate it in the context of this gremlin
     * session, and return the result.
     *
     * @param script
     * @return
     */
    public GremlinEvaluationJob evaluate(String script) {
        GremlinEvaluationJob job = new GremlinEvaluationJob(script);

        try {

            jobQueue.add(job);

            while (!job.isComplete()) {
                Thread.sleep(10);
            }

            return job;
        } catch (InterruptedException e) {
            return job;
        }
    }

    /**
     * Destroy the internal gremlin evaluator and replace it with a clean slate.
     */
    public synchronized void reset() {
        // #run() will pick up on this and create a new script engine. This
        // ensures it is instantiated in the correct thread context.
        this.scriptEngine = null;
    }

    /**
     * Get the number of milliseconds this worker has been idle.
     */
    public long getIdleTime() {
        return (new Date()).getTime() - lastTimeUsed.getTime();
    }

    public void die() {
        this.sepukko = true;
    }

    /**
     * Internal evaluate implementation. This actually interprets a gremlin
     * statement.
     */
    @SuppressWarnings("unchecked")
    protected Object performEvaluation(GremlinEvaluationJob job) {
        try {
            this.lastTimeUsed = new Date();

            scriptEngine.getContext().setWriter(job.getOutputWriter());
            scriptEngine.getContext().setErrorWriter(job.getOutputWriter());
            return scriptEngine.eval(job.getScript());

        } catch (ScriptException e) {
            return e;
        } catch (RuntimeException e) {
            e.printStackTrace();
            return e;
        }
    }
}
