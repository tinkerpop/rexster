package com.tinkerpop.rexster.gremlin;

import java.util.concurrent.Callable;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.apache.log4j.Logger;

public class GremlinEvaluationJobCallable implements Callable<GremlinEvaluationJob> {
    private static final Logger logger = Logger.getLogger(GremlinEvaluationJobCallable.class);
    private final Object scriptEngineLock;
    private final ScriptEngine scriptEngine;
    private final GremlinEvaluationJob job;

    public GremlinEvaluationJobCallable(final ScriptEngine scriptEngine, final Object scriptEngineLock, final String script) {
        this.scriptEngine = scriptEngine;
        this.scriptEngineLock = scriptEngineLock;
        this.job = new GremlinEvaluationJob(script);
    }

    @Override
    public GremlinEvaluationJob call() throws Exception {
        synchronized (scriptEngineLock) {
            try {
                scriptEngine.getContext().setWriter(job.getOutputWriter());
                scriptEngine.getContext().setErrorWriter(job.getOutputWriter());
                job.setResult(scriptEngine.eval(job.getScript()));

            } catch (ScriptException e) {
                logger.error("ScriptEngine error running [%s]", e);
                job.setResult(e);
            } catch (RuntimeException e) {
                logger.error("ScriptEngine error running [%s]", e);
                job.setResult(e);
            }
            return job;
        }
    }

}
