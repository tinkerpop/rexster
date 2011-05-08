package com.tinkerpop.rexster.servlet.gremlin;

import java.util.List;

/**
 * Data structure keeping a script to be evaluated and its result together.
 * <p/>
 * Credit to Neo Technology (http://neotechnology.com/) for most of the code related to the
 * Gremlin Terminal in Rexster.  Specifically, this code was borrowed from
 * https://github.com/neo4j/webadmin and re-purposed for Rexster's needs.
 * <p/>
 * Original author Jacob Hansson <jacob@voltvoodoo.com>
 */
public class ConsoleEvaluationJob {

    protected String script;
    protected List<String> result;
    protected volatile boolean complete = false;

    public ConsoleEvaluationJob(String script) {
        this.script = script;
    }

    public boolean isComplete() {
        return complete;
    }

    public void setResult(List<String> result) {
        this.result = result;
        this.complete = true;
    }

    public List<String> getResult() {
        return this.result;
    }

    public String getScript() {
        return this.script;
    }

}
