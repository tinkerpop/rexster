package com.tinkerpop.rexster.servlet.gremlin;

import com.tinkerpop.gremlin.pipes.util.Table;
import com.tinkerpop.rexster.RexsterApplicationProvider;

import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.io.StringWriter;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * A wrapper thread for a given gremlin instance. Webadmin spawns one of these
 * threads for each client that uses the gremlin console.
 * <p/>
 * Credit to Neo Technology (http://neotechnology.com/) for most of the code related to the
 * Gremlin Terminal in Rexster.  Specifically, this code was borrowed from
 * https://github.com/neo4j/webadmin and re-purposed for Rexster's needs.
 * <p/>
 * Original author Jacob Hansson <jacob@voltvoodoo.com>
 */
@SuppressWarnings("restriction")
public class ConsoleSession implements Runnable {

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
     * The scriptengine error and out streams are directed into this string
     * writer.
     */
    protected StringWriter outputWriter;

    /**
     * Commands waiting to be executed. Number of waiting commands is capped,
     * since this is meant to be used by a single client.
     */
    protected BlockingQueue<ConsoleEvaluationJob> jobQueue = new ArrayBlockingQueue<ConsoleEvaluationJob>(
            MAX_COMMANDS_WAITING);

    /**
     * Should I shut down?
     */
    protected boolean sepukko = false;

    /**
     * Mama thread.
     */
    protected Thread runner = new Thread(this, "GremlinSession");

    private String graphName;

    private RexsterApplicationProvider rap;

    public ConsoleSession(String graphName, RexsterApplicationProvider rap) {
        this.graphName = graphName;
        this.rap = rap;
        runner.start();
    }

    public void run() {

        ConsoleEvaluationJob job;
        try {
            while (true) {
                if (scriptEngine == null) {
                    scriptEngine = GremlinFactory
                            .createGremlinScriptEngine(this.graphName, this.rap);
                }

                job = jobQueue.take();
                job.setResult(performEvaluation(job.getScript()));

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
    public List<String> evaluate(String script) {
        try {
            ConsoleEvaluationJob job = new ConsoleEvaluationJob(script);

            jobQueue.add(job);

            while (!job.isComplete()) {
                Thread.sleep(10);
            }

            return job.getResult();
        } catch (InterruptedException e) {
            return new ArrayList<String>();
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
    protected List<String> performEvaluation(String line) {
        try {
            this.lastTimeUsed = new Date();
            resetOutputWriter();

            List<Object> resultLines = new ArrayList<Object>();
            Object result = scriptEngine.eval(line);
            if (result instanceof Table) {
                Table table = (Table) result;
                Iterator<Table.Row> rows = table.iterator();

                List<String> columnNames = table.getColumnNames();

                while (rows.hasNext()) {
                    Table.Row row = rows.next();
                    StringBuffer sb = new StringBuffer();
                    sb.append("[");
                    for (String columnName : columnNames) {
                        sb.append(columnName);
                        sb.append(":");
                        sb.append(row.getColumn(columnName).toString());
                        sb.append(",");
                    }

                    // delete last comma
                    if (sb.length() > 1) {
                        sb.deleteCharAt(sb.length() - 1);
                    }

                    sb.append("]");

                    resultLines.add(sb.toString());
                }
            } else if (result instanceof Iterable) {
                for (Object o : (Iterable) result) {
                    resultLines.add(o);
                }
            } else if (result instanceof Iterator) {
                Iterator itty = (Iterator) result;
                while (itty.hasNext()) {
                    resultLines.add(itty.next());
                }
            } else if (result instanceof Map) {
                Map map = (Map) result;
                for (Object key : map.keySet()) {
                    resultLines.add(key + "=" + map.get(key).toString());
                }
            } else {
                resultLines.add(result);
            }

            // Handle output data
            List<String> outputLines = new ArrayList<String>();

            // Handle eval() result
            String[] printLines = outputWriter.toString().split("\n");

            if (printLines.length > 0 && printLines[0].length() > 0) {
                for (String printLine : printLines) {
                    outputLines.add(printLine);
                }
            }

            if (resultLines == null
                    || resultLines.size() == 0
                    || (resultLines.size() == 1 && (resultLines.get(0) == null || resultLines
                    .get(0).toString().length() == 0))) {
                // Result was empty, add empty text if there was also no IO
                // output
                if (outputLines.size() == 0) {
                    outputLines.add("");
                }
            } else {
                // Make sure all lines are strings
                for (Object resultLine : resultLines) {
                    outputLines.add(resultLine.toString());
                }
            }

            return outputLines;
        } catch (ScriptException e) {
            return exceptionToResultList(e);
        } catch (RuntimeException e) {
            e.printStackTrace();
            return exceptionToResultList(e);
        }
    }

    private List<String> exceptionToResultList(Exception e) {
        ArrayList<String> resultList = new ArrayList<String>();

        resultList.add(e.getMessage());

        return resultList;
    }

    private void resetOutputWriter() {
        outputWriter = new StringWriter();
        scriptEngine.getContext().setWriter(outputWriter);
        scriptEngine.getContext().setErrorWriter(outputWriter);
    }

}
