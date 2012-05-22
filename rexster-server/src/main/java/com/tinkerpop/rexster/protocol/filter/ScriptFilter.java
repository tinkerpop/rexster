package com.tinkerpop.rexster.protocol.filter;

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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ScriptFilter extends BaseFilter {
    private static final Logger logger = Logger.getLogger(RexProSession.class);

    public NextAction handleRead(final FilterChainContext ctx) throws IOException {
        final RexProMessage message = ctx.getMessage();

        if (message instanceof ScriptRequestMessage) {
            ScriptRequestMessage specificMessage = (ScriptRequestMessage) message;

            RexProSession session = RexProSessions.getSession(specificMessage.sessionAsUUID().toString());

            try {
                Object result = session.evaluate(specificMessage.Script, specificMessage.LanguageName, specificMessage.getBindings());

                List<RexProMessage> messageList = new ArrayList<RexProMessage>();
                if (session.getChannel() == SessionRequestMessage.CHANNEL_CONSOLE) {
                    ConsoleScriptResponseMessage consoleScriptResponseMessage = new ConsoleScriptResponseMessage();
                    consoleScriptResponseMessage.Bindings = ConsoleScriptResponseMessage.convertBindingsToByteArray(session.getBindings());
                    consoleScriptResponseMessage.Flag = ConsoleScriptResponseMessage.FLAG_COMPLETE_MESSAGE;
                    consoleScriptResponseMessage.Session = specificMessage.Session;
                    consoleScriptResponseMessage.Request = specificMessage.Request;
                    List<String> consoleLines = ConsoleScriptResponseMessage.convertResultToConsoleLines(result);
                    consoleScriptResponseMessage.ConsoleLines = new String[consoleLines.size()];
                    consoleLines.toArray(consoleScriptResponseMessage.ConsoleLines);

                    messageList.add(consoleScriptResponseMessage);
                } else if (session.getChannel() == SessionRequestMessage.CHANNEL_MSGPACK) {
                    MsgPackScriptResponseMessage msgPackScriptResponseMessage = new MsgPackScriptResponseMessage();
                    msgPackScriptResponseMessage.Flag = MsgPackScriptResponseMessage.FLAG_COMPLETE_MESSAGE;
                    msgPackScriptResponseMessage.Session = specificMessage.Session;
                    msgPackScriptResponseMessage.Request = specificMessage.Request;
                    msgPackScriptResponseMessage.Results = MsgPackScriptResponseMessage.convertResultToBytes(result);
                    
                    messageList.add(msgPackScriptResponseMessage);
                }

                ctx.write(messageList);

            } catch (ScriptException se) {
                logger.warn("Could not process script [" + specificMessage.Script + "] for language ["
                        + specificMessage.LanguageName + "] on session [" + message.Session
                        + "] and request [" + message.Request + "]");

                ErrorResponseMessage errorMessage = new ErrorResponseMessage();
                errorMessage.setSessionAsUUID(RexProMessage.EMPTY_SESSION);
                errorMessage.Request = message.Request;
                errorMessage.ErrorMessage = "An error occurred while processing the script for language ["
                        + specificMessage.LanguageName + "]: " + se.getMessage();
                errorMessage.Flag = ErrorResponseMessage.FLAG_ERROR_SCRIPT_FAILURE;

                ctx.write(errorMessage);

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
}
