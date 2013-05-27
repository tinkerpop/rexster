package com.tinkerpop.rexster.protocol.server;

import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.rexster.Tokens;
import com.tinkerpop.rexster.protocol.EngineController;
import com.tinkerpop.rexster.protocol.EngineHolder;
import com.tinkerpop.rexster.protocol.session.AbstractRexProSession;
import com.tinkerpop.rexster.protocol.session.RexProSession;
import com.tinkerpop.rexster.protocol.session.RexProSessions;
import com.tinkerpop.rexster.protocol.msg.*;
import com.tinkerpop.rexster.server.RexsterApplication;
import org.apache.log4j.Logger;
import com.yammer.metrics.Counter;
import com.yammer.metrics.MetricRegistry;
import com.yammer.metrics.Timer;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.io.IOException;

/**
 */
public class ScriptServer {
    private static final Logger logger = Logger.getLogger(ScriptServer.class);

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

    public ScriptServer(final RexsterApplication rexsterApplication) {
        this.rexsterApplication = rexsterApplication;

        final MetricRegistry metricRegistry = this.rexsterApplication.getMetricRegistry();
        this.scriptTimer = metricRegistry.timer(MetricRegistry.name("rexpro", "script-engine"));
        this.successfulExecutions = metricRegistry.counter(MetricRegistry.name("rexpro", "script-engine", "success"));
        this.failedExecutions = metricRegistry.counter(MetricRegistry.name("rexpro", "script-engine", "fail"));
    }
    public void handleRequest(ScriptRequestMessage message, RexProRequest request) throws IOException {

        try {
            final AbstractRexProSession session;
            Graph graph = null;

            message.validateMetaData();
            if (message.metaGetInSession()) {
                if (message.Session == null) {
                    logger.error("no session key on message");
                    request.writeResponseMessage(
                        MessageUtil.createErrorResponse(
                            message.Request,
                            RexProMessage.EMPTY_SESSION_AS_BYTES,
                            ErrorResponseMessage.INVALID_SESSION_ERROR,
                            "There was no session key on the message, set the meta field 'inSession' to false if you want to execute sessionless requests"
                        )
                    );
                }

                //session script request
                session = RexProSessions.getSession(message.sessionAsUUID().toString());

                // validate session and channel
                if (sessionDoesNotExist(request, message, session)) return;
                if (channelIsRedefined(request, message, message, session)) return;

                graph = session.getGraphObj();

                // catch any graph redefinition attempts
                if (graphIsRedefined(request, message, message, graph)) return;

            } else {
                session = new EmptySession(rexsterApplication, 0);
            }

            Bindings bindings = message.getBindings();

            // add the graph object to the bindings
            if (message.metaGetGraphName() != null) {
                graph = rexsterApplication.getGraph(message.metaGetGraphName());
                bindings.put(message.metaGetGraphObjName(), graph);
                if (graph == null) {
                    // graph config problem
                    request.writeResponseMessage(
                            MessageUtil.createErrorResponse(
                                    message.Request, RexProMessage.EMPTY_SESSION_AS_BYTES,
                                    ErrorResponseMessage.GRAPH_CONFIG_ERROR,
                                    "the graph '" + message.metaGetGraphName() + "' was not found by Rexster"
                            )
                    );
                    return;
                }
            }

            final Timer.Context timer = scriptTimer.time();
            try {

                // execute script
                session.evaluate(
                        message.Script,
                        message.LanguageName,
                        bindings,
                        message.metaGetIsolate(),
                        message.metaGetTransaction(),
                        graph,
                        request
                );

                successfulExecutions.inc();

            } catch (Exception ex) {
                // rollback transaction
                if (message.metaGetTransaction()) {
                    tryRollbackTransaction(graph);
                }

                failedExecutions.inc();

                throw ex;
            } finally {
                timer.stop();
            }

        } catch (ScriptException se) {
            logger.warn("Could not process script [" + message.Script + "] for language ["
                    + message.LanguageName + "] on session [" + message.Session
                    + "] and request [" + message.Request + "]");

            request.writeResponseMessage(
                    MessageUtil.createErrorResponse(
                            message.Request, RexProMessage.EMPTY_SESSION_AS_BYTES,
                            ErrorResponseMessage.SCRIPT_FAILURE_ERROR,
                            String.format(
                                    MessageTokens.ERROR_IN_SCRIPT_PROCESSING,
                                    message.LanguageName,
                                    se.getMessage()
                            )
                    )
            );

        } catch (Exception e) {
            logger.error(e);
            request.writeResponseMessage(
                    MessageUtil.createErrorResponse(
                            message.Request,
                            RexProMessage.EMPTY_SESSION_AS_BYTES,
                            ErrorResponseMessage.SCRIPT_FAILURE_ERROR,
                            String.format(
                                    MessageTokens.ERROR_IN_SCRIPT_PROCESSING,
                                    message.LanguageName,
                                    e.toString()
                            )
                    )
            );
        }
    }

    public static void tryRollbackTransaction(final Graph graph) {
        if (graph == null) return;
        if (graph.getFeatures().supportsTransactions && graph instanceof TransactionalGraph) {
            ((TransactionalGraph) graph).stopTransaction(TransactionalGraph.Conclusion.FAILURE);
        }
    }

    public static void tryCommitTransaction(final Graph graph) {
        if (graph == null) return;
        if (graph.getFeatures().supportsTransactions && graph instanceof TransactionalGraph) {
            ((TransactionalGraph) graph).stopTransaction(TransactionalGraph.Conclusion.SUCCESS);
        }
    }

    /**
     * The graph cannot be redefined within a session.
     */
    private static boolean graphIsRedefined(final RexProRequest request, final RexProMessage message,
                                            final ScriptRequestMessage specificMessage, final Graph graph) throws IOException {
        if (specificMessage.metaGetGraphName() != null && graph != null) {
            request.writeResponseMessage(
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
    private static boolean channelIsRedefined(final RexProRequest request, final RexProMessage message,
                                              final ScriptRequestMessage specificMessage, final AbstractRexProSession session) throws IOException {
        // have to cast the channel to byte because meta converts to int internally via msgpack conversion
        if (session.getChannel() != Byte.parseByte(specificMessage.metaGetChannel().toString())) {
            request.writeResponseMessage(
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
    private static boolean sessionDoesNotExist(final RexProRequest request, final RexProMessage message, final AbstractRexProSession session) throws IOException {
        if (session == null) {
            // the message is assigned a session that does not exist on the server
            request.writeResponseMessage(
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

    /**
     * Simplified session class that executes scripts immediately
     */
    private class EmptySession extends AbstractRexProSession {

        private EmptySession(RexsterApplication rexsterApplication, int channel) {
            super(rexsterApplication, channel);
        }

        protected void execute(Evaluator evaluator) throws ScriptException {
            try {
                evaluator.call();
            } catch (Exception e) {
                throw new ScriptException(e);
            }
        }
    }
}
