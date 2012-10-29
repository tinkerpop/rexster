package com.tinkerpop.rexster.protocol.filter;

import com.tinkerpop.rexster.protocol.msg.GraphSONScriptResponseMessage;
import com.tinkerpop.rexster.protocol.msg.MessageFlag;
import com.tinkerpop.rexster.protocol.msg.MessageTokens;
import com.tinkerpop.rexster.protocol.msg.MessageUtil;
import com.tinkerpop.rexster.server.RexsterApplication;
import com.tinkerpop.rexster.Tokens;
import com.tinkerpop.rexster.protocol.EngineController;
import com.tinkerpop.rexster.protocol.EngineHolder;
import com.tinkerpop.rexster.protocol.RexProSession;
import com.tinkerpop.rexster.protocol.RexProSessions;
import com.tinkerpop.rexster.protocol.msg.ConsoleScriptResponseMessage;
import com.tinkerpop.rexster.protocol.msg.ErrorResponseMessage;
import com.tinkerpop.rexster.protocol.msg.MsgPackScriptResponseMessage;
import com.tinkerpop.rexster.protocol.msg.RexProMessage;
import com.tinkerpop.rexster.protocol.msg.ScriptRequestMessage;
import com.tinkerpop.rexster.protocol.msg.SessionRequestMessage;
import org.apache.log4j.Logger;
import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;

import javax.script.ScriptException;
import javax.script.SimpleBindings;
import java.io.IOException;
import java.util.List;

/**
 * Processes a ScriptRequestMessage against the script engine for the channel.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class ScriptFilter extends BaseFilter {
    private static final Logger logger = Logger.getLogger(ScriptFilter.class);

    private static final EngineController engineController = EngineController.getInstance();

    private final RexsterApplication rexsterApplication;

    private final SimpleBindings bindings;

    public ScriptFilter(final RexsterApplication rexsterApplication) {
        this.rexsterApplication = rexsterApplication;

        // reuse the bindings for the gremlin script engine
        this.bindings = new SimpleBindings();
        this.bindings.put(Tokens.REXPRO_REXSTER_CONTEXT, this.rexsterApplication);
    }

    public NextAction handleRead(final FilterChainContext ctx) throws IOException {
        final RexProMessage message = ctx.getMessage();

        // check message type to be ScriptRequestMessage

        // short cut the session stuff
        if (message.Flag == MessageFlag.SCRIPT_REQUEST_NO_SESSION) {
            final ScriptRequestMessage specificMessage = (ScriptRequestMessage) message;

            try {
                final EngineHolder engineHolder = engineController.getEngineByLanguageName(specificMessage.LanguageName);
                final Object result = engineHolder.getEngine().eval(specificMessage.Script, bindings);

                final MsgPackScriptResponseMessage msgPackScriptResponseMessage = new MsgPackScriptResponseMessage();
                msgPackScriptResponseMessage.Flag = MessageFlag.SCRIPT_RESPONSE_COMPLETE_MESSAGE;
                msgPackScriptResponseMessage.setSessionAsUUID(RexProMessage.EMPTY_SESSION);
                msgPackScriptResponseMessage.Request = specificMessage.Request;
                msgPackScriptResponseMessage.Results = MsgPackScriptResponseMessage.convertResultToBytes(result);

                ctx.write(msgPackScriptResponseMessage);

                return ctx.getStopAction();
            } catch (ScriptException se) {
                logger.error(se);
            } catch (ClassNotFoundException cnfe) {
                logger.error(cnfe);
            } catch (Exception e) {
                logger.error(e);
            }
        }

        // below here is all session related requests
        if (message.Flag == MessageFlag.SCRIPT_REQUEST_IN_SESSION) {
            final ScriptRequestMessage specificMessage = (ScriptRequestMessage) message;
            final RexProSession session = RexProSessions.getSession(specificMessage.sessionAsUUID().toString());

            try {
                final Object result = session.evaluate(specificMessage.Script, specificMessage.LanguageName, specificMessage.getBindings());
                if (session.getChannel() == SessionRequestMessage.CHANNEL_CONSOLE) {
                    ctx.write(formatForConsoleChannel(specificMessage, session, result));
                } else if (session.getChannel() == SessionRequestMessage.CHANNEL_MSGPACK) {
                    ctx.write(formatForMsgPackChannel(specificMessage, result));
                }  else if (session.getChannel() == SessionRequestMessage.CHANNEL_GRAPHSON) {
                    ctx.write(formatForGraphSONChannel(specificMessage, result));
                }
            } catch (ScriptException se) {
                logger.warn("Could not process script [" + specificMessage.Script + "] for language ["
                        + specificMessage.LanguageName + "] on session [" + message.Session
                        + "] and request [" + message.Request + "]");

                ctx.write(MessageUtil.createErrorResponse(message.Request, RexProMessage.EMPTY_SESSION_AS_BYTES,
                        MessageFlag.ERROR_SCRIPT_FAILURE, String.format(MessageTokens.ERROR_IN_SCRIPT_PROCESSING,
                        specificMessage.LanguageName, se.getMessage())));

                return ctx.getStopAction();
            } catch (ClassNotFoundException cnfe) {
                logger.error(cnfe);
            } catch (IOException ioe) {
                logger.error(ioe);
            } catch (Throwable t) {
                logger.error(t);
            }

            return ctx.getStopAction();
        }

        return ctx.getInvokeAction();
    }

    private static GraphSONScriptResponseMessage formatForGraphSONChannel(final ScriptRequestMessage specificMessage, final Object result) throws Exception {
        final GraphSONScriptResponseMessage graphSONScriptResponseMessage = new GraphSONScriptResponseMessage();
        graphSONScriptResponseMessage.Flag = MessageFlag.SCRIPT_RESPONSE_COMPLETE_MESSAGE;
        graphSONScriptResponseMessage.Session = specificMessage.Session;
        graphSONScriptResponseMessage.Request = specificMessage.Request;
        graphSONScriptResponseMessage.Results = GraphSONScriptResponseMessage.convertResultToBytes(result);
        return graphSONScriptResponseMessage;
    }

    private static MsgPackScriptResponseMessage formatForMsgPackChannel(final ScriptRequestMessage specificMessage, final Object result) throws Exception {
        final MsgPackScriptResponseMessage msgPackScriptResponseMessage = new MsgPackScriptResponseMessage();
        msgPackScriptResponseMessage.Flag = MessageFlag.SCRIPT_RESPONSE_COMPLETE_MESSAGE;
        msgPackScriptResponseMessage.Session = specificMessage.Session;
        msgPackScriptResponseMessage.Request = specificMessage.Request;
        msgPackScriptResponseMessage.Results = MsgPackScriptResponseMessage.convertResultToBytes(result);
        return msgPackScriptResponseMessage;
    }

    private static ConsoleScriptResponseMessage formatForConsoleChannel(final ScriptRequestMessage specificMessage, final RexProSession session, final Object result) throws Exception {
        final ConsoleScriptResponseMessage consoleScriptResponseMessage = new ConsoleScriptResponseMessage();
        consoleScriptResponseMessage.Bindings = ConsoleScriptResponseMessage.convertBindingsToByteArray(session.getBindings());
        consoleScriptResponseMessage.Flag = MessageFlag.SCRIPT_RESPONSE_COMPLETE_MESSAGE;
        consoleScriptResponseMessage.Session = specificMessage.Session;
        consoleScriptResponseMessage.Request = specificMessage.Request;

        final List<String> consoleLines = ConsoleScriptResponseMessage.convertResultToConsoleLines(result);
        consoleScriptResponseMessage.ConsoleLines = new String[consoleLines.size()];
        consoleLines.toArray(consoleScriptResponseMessage.ConsoleLines);
        return consoleScriptResponseMessage;
    }
}
