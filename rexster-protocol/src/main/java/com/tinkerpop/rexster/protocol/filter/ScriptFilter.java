package com.tinkerpop.rexster.protocol.filter;

import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.rexster.Tokens;
import com.tinkerpop.rexster.protocol.EngineController;
import com.tinkerpop.rexster.protocol.EngineHolder;
import com.tinkerpop.rexster.protocol.RexProSession;
import com.tinkerpop.rexster.protocol.RexProSessions;
import com.tinkerpop.rexster.protocol.msg.*;
import com.tinkerpop.rexster.server.RexsterApplication;
import com.yammer.metrics.Counter;
import com.yammer.metrics.MetricRegistry;
import com.yammer.metrics.Timer;
import org.apache.log4j.Logger;
import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Processes a ScriptRequestMessage against the script engine for the channel.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 * @author Blake Eggleston (bdeggleston.github.com)
 */
public class ScriptFilter extends BaseFilter {
    private static final Logger logger = Logger.getLogger(ScriptFilter.class);

    private static final EngineController engineController = EngineController.getInstance();

    private final RexsterApplication rexsterApplication;

    private final Timer scriptTimer;

    /**
     * A count of successful execution of scripts.  Counts even if a write back to the stream fails.
     */
    private final Counter successfulExecutions;

    /**
     * A count of failed script executions. Not the same as a failed request.
     */
    private final Counter failedExecutions;

    public ScriptFilter(final RexsterApplication rexsterApplication) {
        this.rexsterApplication = rexsterApplication;

        final MetricRegistry metricRegistry = this.rexsterApplication.getMetricRegistry();
        this.scriptTimer = metricRegistry.timer(MetricRegistry.name("rexpro", "script-engine"));
        this.successfulExecutions = metricRegistry.counter(MetricRegistry.name("rexpro", "script-engine", "success"));
        this.failedExecutions = metricRegistry.counter(MetricRegistry.name("rexpro", "script-engine", "fail"));
    }

    /**
     * Handles the execution of script requests, with or without session
     */
    public NextAction handleRead(final FilterChainContext ctx) throws IOException {
        final RexProMessage message = ctx.getMessage();

        //carry on if this isn't a script request message
        if (!(message instanceof ScriptRequestMessage)) {
            return ctx.getInvokeAction();
        }

        final ScriptRequestMessage specificMessage = (ScriptRequestMessage) message;
        try {
            specificMessage.validateMetaData();
            if (specificMessage.metaGetInSession()) {
                if (specificMessage.Session == null) {
                    logger.error("no session key on message");
                    ctx.write(
                        MessageUtil.createErrorResponse(
                            specificMessage.Request,
                            RexProMessage.EMPTY_SESSION_AS_BYTES,
                            ErrorResponseMessage.INVALID_SESSION_ERROR,
                            "There was no session key on the message, set the meta field 'inSession' to true if you want to execute sessionless requests"
                        )
                    );

                }

                //session script request
                final RexProSession session = RexProSessions.getSession(specificMessage.sessionAsUUID().toString());

                // validate session and channel
                if (sessionDoesNotExist(ctx, message, session)) return ctx.getStopAction();
                if (channelIsRedefined(ctx, message, specificMessage, session)) return ctx.getStopAction();

                Graph graph = session.getGraphObj();

                // catch any graph redefinition attempts
                if (graphIsRedefined(ctx, message, specificMessage, graph)) return ctx.getStopAction();

                Bindings bindings = specificMessage.getBindings();

                // add the graph object to the bindings
                if (specificMessage.metaGetGraphName() != null) {
                    graph = rexsterApplication.getGraph(specificMessage.metaGetGraphName());
                    bindings.put(specificMessage.metaGetGraphObjName(), graph);
                }

                // start transaction
                if (graph != null && specificMessage.metaGetTransaction()) {
                    tryRollbackTransaction(graph);
                }

                final Timer.Context context = scriptTimer.time();
                try {
                    // execute script
                    final Object result = session.evaluate(
                        specificMessage.Script,
                        specificMessage.LanguageName,
                        bindings,
                        specificMessage.metaGetIsolate()
                    );

                    RexProMessage resultMessage = null;
                    if (session.getChannel() == RexProChannel.CHANNEL_CONSOLE) {
                        resultMessage = formatForConsoleChannel(specificMessage, session, result);

                    } else if (session.getChannel() == RexProChannel.CHANNEL_MSGPACK) {
                        resultMessage = formatForMsgPackChannel(specificMessage, session, result);

                    } else if (session.getChannel() == RexProChannel.CHANNEL_GRAPHSON) {
                        resultMessage = formatForGraphSONChannel(specificMessage, session, result);
                    } else {
                        // malformed channel???!!!
                        logger.warn(String.format("Session is configured for a channel that does not exist: [%s]", session.getChannel()));
                    }

                    //serialize before closing the transaction and result objects go out of scope
                    byte[] messageBytes = RexProMessage.serialize(resultMessage);

                    //commit transaction
                    if (graph != null && specificMessage.metaGetTransaction()) {
                        tryCommitTransaction(graph);
                    }

                    successfulExecutions.inc();

                    // write the message after the transaction so that we can be assured that it properly committed
                    // if auto-commit was on
                    ctx.write(messageBytes);
                } catch (Exception ex) {
                    // rollback transaction
                    if (graph != null && specificMessage.metaGetTransaction()) {
                        tryRollbackTransaction(graph);
                    }

                    failedExecutions.inc();

                    throw ex;
                } finally {
                    context.stop();
                }

            } else {
                // non-session script request
                final EngineHolder engineHolder = engineController.getEngineByLanguageName(specificMessage.LanguageName);
                final ScriptEngine scriptEngine = engineHolder.getEngine();
                final Bindings bindings = scriptEngine.createBindings();
                final Bindings rexsterBindings = specificMessage.getBindings();
                for (Map.Entry<String,Object> e : rexsterBindings.entrySet()) {
                    bindings.put(e.getKey(), e.getValue());
                }
                bindings.put(Tokens.REXPRO_REXSTER_CONTEXT, this.rexsterApplication);

                Graph graph = null;
                if (specificMessage.metaGetGraphName() != null) {
                    graph = this.rexsterApplication.getGraph(specificMessage.metaGetGraphName());
                    if (graph != null) {
                        bindings.put(specificMessage.metaGetGraphObjName(), graph);
                    } else {
                        // graph config problem
                        ctx.write(
                            MessageUtil.createErrorResponse(
                                message.Request, RexProMessage.EMPTY_SESSION_AS_BYTES,
                                ErrorResponseMessage.GRAPH_CONFIG_ERROR,
                                "the graph '" + specificMessage.metaGetGraphName() + "' was not found by Rexster"
                            )
                        );

                        return ctx.getStopAction();

                    }
                }

                // start transaction
                if (graph != null && specificMessage.metaGetTransaction()) {
                    tryRollbackTransaction(graph);
                }

                final Timer.Context context = scriptTimer.time();
                try {
                    Object result = scriptEngine.eval(specificMessage.Script, bindings);

                    RexProMessage resultMessage = null;
                    if (specificMessage.metaGetChannel() == RexProChannel.CHANNEL_CONSOLE) {
                        resultMessage = formatForConsoleChannel(specificMessage, null, result);

                    } else if (specificMessage.metaGetChannel() == RexProChannel.CHANNEL_MSGPACK) {
                        resultMessage = formatForMsgPackChannel(specificMessage, null, result);

                    } else if (specificMessage.metaGetChannel() == RexProChannel.CHANNEL_GRAPHSON) {
                        resultMessage = formatForGraphSONChannel(specificMessage, null, result);
                    } else {
                        // malformed channel???!!!
                        logger.warn(String.format("Sessionless request is configured for a channel that does not exist: [%s]", specificMessage.metaGetChannel()));
                    }

                    //serialize before closing the transaction and result objects go out of scope
                    byte[] messageBytes = RexProMessage.serialize(resultMessage);

                    // commit transaction
                    if (graph != null && specificMessage.metaGetTransaction()) {
                        tryCommitTransaction(graph);
                    }

                    successfulExecutions.inc();

                    // write the message after the transaction so that we can be assured that it properly committed
                    // if auto-commit was on
                    ctx.write(messageBytes);
                } catch (Exception ex) {
                    // rollback transaction
                    if (graph != null && specificMessage.metaGetTransaction()) {
                        tryRollbackTransaction(graph);
                    }

                    failedExecutions.dec();

                    throw ex;
                } finally {
                    context.stop();
                }
            }

        } catch (ScriptException se) {
            logger.warn("Could not process script [" + specificMessage.Script + "] for language ["
                    + specificMessage.LanguageName + "] on session [" + specificMessage.Session
                    + "] and request [" + specificMessage.Request + "]");

            ctx.write(
                MessageUtil.createErrorResponse(
                    specificMessage.Request, RexProMessage.EMPTY_SESSION_AS_BYTES,
                    ErrorResponseMessage.SCRIPT_FAILURE_ERROR,
                    String.format(
                        MessageTokens.ERROR_IN_SCRIPT_PROCESSING,
                        specificMessage.LanguageName,
                        se.getMessage()
                    )
                )
            );

        } catch (Exception e) {
            logger.error(e);
            ctx.write(
                MessageUtil.createErrorResponse(
                    specificMessage.Request,
                    RexProMessage.EMPTY_SESSION_AS_BYTES,
                    ErrorResponseMessage.SCRIPT_FAILURE_ERROR,
                    String.format(
                        MessageTokens.ERROR_IN_SCRIPT_PROCESSING,
                        specificMessage.LanguageName,
                        e.toString()
                    )
                )
            );
        }
        return ctx.getStopAction();
    }

    private static void tryRollbackTransaction(final Graph graph) {
        if (graph.getFeatures().supportsTransactions && graph instanceof TransactionalGraph) {
            ((TransactionalGraph) graph).stopTransaction(TransactionalGraph.Conclusion.FAILURE);
        }
    }

    private static void tryCommitTransaction(final Graph graph) {
        if (graph.getFeatures().supportsTransactions && graph instanceof TransactionalGraph) {
            ((TransactionalGraph) graph).stopTransaction(TransactionalGraph.Conclusion.SUCCESS);
        }
    }

    /**
     * The graph cannot be redefined within a session.
     */
    private static boolean graphIsRedefined(final FilterChainContext ctx, final RexProMessage message,
                                            final ScriptRequestMessage specificMessage, final Graph graph) {
        if (specificMessage.metaGetGraphName() != null && graph != null) {
            ctx.write(
                MessageUtil.createErrorResponse(
                        message.Request, RexProMessage.EMPTY_SESSION_AS_BYTES,
                        ErrorResponseMessage.GRAPH_CONFIG_ERROR,
                        MessageTokens.ERROR_GRAPH_REDEFINITION
                )
            );

            return true;
        }
        return false;
    }

    /**
     * The channel cannot be redefined within a session.
     */
    private static boolean channelIsRedefined(final FilterChainContext ctx, final RexProMessage message,
                                              final ScriptRequestMessage specificMessage, final RexProSession session) {
        // have to cast the channel to byte because meta converts to int internally via msgpack conversion
        if (session.getChannel() != Byte.parseByte(specificMessage.metaGetChannel().toString())) {
            ctx.write(
                    MessageUtil.createErrorResponse(
                            message.Request, RexProMessage.EMPTY_SESSION_AS_BYTES,
                            ErrorResponseMessage.CHANNEL_CONFIG_ERROR,
                            MessageTokens.ERROR_CHANNEL_REDEFINITION
                    )
            );

            return true;
        }
        return false;
    }

    /**
     * The session has to be found for script to be executed.
     */
    private static boolean sessionDoesNotExist(final FilterChainContext ctx, final RexProMessage message,
                                               final RexProSession session) {
        if (session == null) {
            // the message is assigned a session that does not exist on the server
            ctx.write(
                MessageUtil.createErrorResponse(
                        message.Request,
                        RexProMessage.EMPTY_SESSION_AS_BYTES,
                        ErrorResponseMessage.INVALID_SESSION_ERROR,
                        MessageTokens.ERROR_SESSION_INVALID
                )
            );
            return true;
        }
        return false;
    }

    private static GraphSONScriptResponseMessage formatForGraphSONChannel(final ScriptRequestMessage specificMessage, final RexProSession session, final Object result) throws Exception {
        final GraphSONScriptResponseMessage graphSONScriptResponseMessage = new GraphSONScriptResponseMessage();

        if (specificMessage.metaGetInSession()){
            graphSONScriptResponseMessage.Session = specificMessage.Session;
        } else {
            graphSONScriptResponseMessage.setSessionAsUUID(RexProMessage.EMPTY_SESSION);
        }

        graphSONScriptResponseMessage.Request = specificMessage.Request;
        graphSONScriptResponseMessage.Results = GraphSONScriptResponseMessage.convertResultToBytes(result);
        if (session != null){
            graphSONScriptResponseMessage.Bindings.putAll(session.getBindings());
        }
        graphSONScriptResponseMessage.validateMetaData();
        return graphSONScriptResponseMessage;
    }

    private static MsgPackScriptResponseMessage formatForMsgPackChannel(final ScriptRequestMessage specificMessage, final RexProSession session, final Object result) throws Exception {
        final MsgPackScriptResponseMessage msgPackScriptResponseMessage = new MsgPackScriptResponseMessage();

        if (specificMessage.metaGetInSession()){
            msgPackScriptResponseMessage.Session = specificMessage.Session;
        } else {
            msgPackScriptResponseMessage.setSessionAsUUID(RexProMessage.EMPTY_SESSION);
        }

        msgPackScriptResponseMessage.Request = specificMessage.Request;
        msgPackScriptResponseMessage.Results.set(result);
        if (session != null){
            msgPackScriptResponseMessage.Bindings.putAll(session.getBindings());
        }
        msgPackScriptResponseMessage.validateMetaData();
        return msgPackScriptResponseMessage;
    }

    private static ConsoleScriptResponseMessage formatForConsoleChannel(final ScriptRequestMessage specificMessage, final RexProSession session, final Object result) throws Exception {
        final ConsoleScriptResponseMessage consoleScriptResponseMessage = new ConsoleScriptResponseMessage();

        if (specificMessage.metaGetInSession()){
            consoleScriptResponseMessage.Session = specificMessage.Session;
        } else {
            consoleScriptResponseMessage.setSessionAsUUID(RexProMessage.EMPTY_SESSION);
        }

        consoleScriptResponseMessage.Request = specificMessage.Request;

        final List<String> consoleLines = ConsoleScriptResponseMessage.convertResultToConsoleLines(result);
        consoleScriptResponseMessage.ConsoleLines = new String[consoleLines.size()];
        consoleLines.toArray(consoleScriptResponseMessage.ConsoleLines);
        if (session != null) {
            consoleScriptResponseMessage.Bindings.putAll(session.getBindings());
        }
        consoleScriptResponseMessage.validateMetaData();
        return consoleScriptResponseMessage;
    }
}
