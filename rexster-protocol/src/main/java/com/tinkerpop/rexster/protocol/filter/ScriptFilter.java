package com.tinkerpop.rexster.protocol.filter;

import com.tinkerpop.rexster.protocol.msg.ConsoleScriptResponseMessage;
import com.tinkerpop.rexster.protocol.msg.ErrorResponseMessage;
import com.tinkerpop.rexster.protocol.msg.GraphSONScriptResponseMessage;
import com.tinkerpop.rexster.protocol.msg.MessageTokens;
import com.tinkerpop.rexster.protocol.msg.MessageUtil;
import com.tinkerpop.rexster.protocol.msg.MsgPackScriptResponseMessage;
import com.tinkerpop.rexster.protocol.msg.RexProMessage;
import com.tinkerpop.rexster.protocol.msg.ScriptRequestMessage;
import com.tinkerpop.rexster.protocol.msg.SessionRequestMessage;
import com.tinkerpop.rexster.server.RexsterApplication;
import com.tinkerpop.rexster.Tokens;
import com.tinkerpop.rexster.protocol.EngineController;
import com.tinkerpop.rexster.protocol.EngineHolder;
import com.tinkerpop.rexster.protocol.RexProSession;
import com.tinkerpop.rexster.protocol.RexProSessions;
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

    public ScriptFilter(final RexsterApplication rexsterApplication) {
        this.rexsterApplication = rexsterApplication;
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
                //session script request
                final RexProSession session = RexProSessions.getSession(specificMessage.sessionAsUUID().toString());

                //validate session
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
                    return ctx.getStopAction();
                }

                //catch any graph redefinition attempts
                if (specificMessage.metaGetGraphName() != null && session.hasGraphObj()) {
                    //graph config problem
                    ctx.write(
                        MessageUtil.createErrorResponse(
                            message.Request, RexProMessage.EMPTY_SESSION_AS_BYTES,
                            ErrorResponseMessage.GRAPH_CONFIG_ERROR,
                            MessageTokens.ERROR_GRAPH_REDEFINITION
                        )
                    );

                    return ctx.getStopAction();
                }

                Bindings bindings = specificMessage.getBindings();

                //add the graph object to the bindings
                if (specificMessage.metaGetGraphName() != null) {
                    bindings.put(specificMessage.metaGetGraphObjName(), rexsterApplication.getGraph(specificMessage.metaGetGraphName()));
                }

                final Object result = session.evaluate(
                    specificMessage.Script,
                    specificMessage.LanguageName,
                    bindings,
                    specificMessage.metaGetIsolate(),
                    specificMessage.metaGetTransaction()
                );

                if (session.getChannel() == SessionRequestMessage.CHANNEL_CONSOLE) {
                    ctx.write(formatForConsoleChannel(specificMessage, session, result));

                } else if (session.getChannel() == SessionRequestMessage.CHANNEL_MSGPACK) {
                    ctx.write(formatForMsgPackChannel(specificMessage, result));

                }  else if (session.getChannel() == SessionRequestMessage.CHANNEL_GRAPHSON) {
                    ctx.write(formatForGraphSONChannel(specificMessage, result));
                }

            } else {
                //non-session script request
                final EngineHolder engineHolder = engineController.getEngineByLanguageName(specificMessage.LanguageName);
                final ScriptEngine scriptEngine = engineHolder.getEngine();
                final Bindings bindings = scriptEngine.createBindings();
                final Bindings rexsterBindings = specificMessage.getBindings();
                for (Map.Entry<String,Object> e : rexsterBindings.entrySet()) {
                    bindings.put(e.getKey(), e.getValue());
                }
                bindings.put(Tokens.REXPRO_REXSTER_CONTEXT, this.rexsterApplication);

                if (specificMessage.metaGetGraphName() != null) {
                    bindings.put(specificMessage.metaGetGraphObjName(), this.rexsterApplication.getGraph(specificMessage.metaGetGraphName()));
                }

                final Object result = scriptEngine.eval(specificMessage.Script, bindings);
                ctx.write(formatForMsgPackChannel(specificMessage, result));
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

    private static GraphSONScriptResponseMessage formatForGraphSONChannel(final ScriptRequestMessage specificMessage, final Object result) throws Exception {
        final GraphSONScriptResponseMessage graphSONScriptResponseMessage = new GraphSONScriptResponseMessage();

        if (specificMessage.metaGetInSession()){
            graphSONScriptResponseMessage.Session = specificMessage.Session;
        } else {
            graphSONScriptResponseMessage.setSessionAsUUID(RexProMessage.EMPTY_SESSION);
        }

        graphSONScriptResponseMessage.Request = specificMessage.Request;
        graphSONScriptResponseMessage.Results = GraphSONScriptResponseMessage.convertResultToBytes(result);
        graphSONScriptResponseMessage.validateMetaData();
        return graphSONScriptResponseMessage;
    }

    private static MsgPackScriptResponseMessage formatForMsgPackChannel(final ScriptRequestMessage specificMessage, final Object result) throws Exception {
        final MsgPackScriptResponseMessage msgPackScriptResponseMessage = new MsgPackScriptResponseMessage();

        if (specificMessage.metaGetInSession()){
            msgPackScriptResponseMessage.Session = specificMessage.Session;
        } else {
            msgPackScriptResponseMessage.setSessionAsUUID(RexProMessage.EMPTY_SESSION);
        }

        msgPackScriptResponseMessage.Request = specificMessage.Request;
        msgPackScriptResponseMessage.Results = MsgPackScriptResponseMessage.convertResultToBytes(result);
        msgPackScriptResponseMessage.validateMetaData();
        return msgPackScriptResponseMessage;
    }

    private static ConsoleScriptResponseMessage formatForConsoleChannel(final ScriptRequestMessage specificMessage, final RexProSession session, final Object result) throws Exception {
        final ConsoleScriptResponseMessage consoleScriptResponseMessage = new ConsoleScriptResponseMessage();
        consoleScriptResponseMessage.Bindings = ConsoleScriptResponseMessage.convertBindingsToConsoleLineByteArray(session.getBindings());

        if (specificMessage.metaGetInSession()){
            consoleScriptResponseMessage.Session = specificMessage.Session;
        } else {
            consoleScriptResponseMessage.setSessionAsUUID(RexProMessage.EMPTY_SESSION);
        }

        consoleScriptResponseMessage.Request = specificMessage.Request;

        final List<String> consoleLines = ConsoleScriptResponseMessage.convertResultToConsoleLines(result);
        consoleScriptResponseMessage.ConsoleLines = new String[consoleLines.size()];
        consoleLines.toArray(consoleScriptResponseMessage.ConsoleLines);
        consoleScriptResponseMessage.validateMetaData();
        return consoleScriptResponseMessage;
    }
}
