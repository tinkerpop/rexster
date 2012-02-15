package com.tinkerpop.rexster.gremlin;

import java.io.StringWriter;
import java.io.Writer;

/**
 * Data structure keeping a script to be evaluated and its result/output together.
 * <p/>
 * Credit to Neo Technology (http://neotechnology.com/) for most of the code related to the
 * Gremlin in Rexster.  Specifically, this code was borrowed from
 * https://github.com/neo4j/webadmin and re-purposed for Rexster's needs.
 * <p/>
 * Original author Jacob Hansson <jacob@voltvoodoo.com>
 */
public class GremlinEvaluationJob {

    protected String script;
    protected Object result;
    protected volatile boolean complete = false;

    /**
     * The scriptengine error and out streams are directed into this string
     * writer.
     */
    protected StringWriter outputWriter;

    public GremlinEvaluationJob(String script) {
        this.script = script;
    }

    public boolean isComplete() {
        return complete;
    }

    public void setResult(Object result) {
        this.result = result;
        this.complete = true;
    }

    public Object getResult() {
        return this.result;
    }

    public String getScript() {
        return this.script;
    }

    public Writer getOutputWriter() {
        if (this.outputWriter == null) {
            this.outputWriter = new StringWriter();
        }

        return this.outputWriter;
    }

}
