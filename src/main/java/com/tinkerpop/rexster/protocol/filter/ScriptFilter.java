package com.tinkerpop.rexster.protocol.filter;

import com.tinkerpop.rexster.protocol.RexProSession;
import com.tinkerpop.rexster.protocol.RexProSessions;
import com.tinkerpop.rexster.protocol.message.ConsoleScriptResponseMessage;
import com.tinkerpop.rexster.protocol.message.ErrorResponseMessage;
import com.tinkerpop.rexster.protocol.message.MessageType;
import com.tinkerpop.rexster.protocol.message.RexProMessage;
import com.tinkerpop.rexster.protocol.message.ScriptRequestMessage;
import com.tinkerpop.rexster.protocol.message.ScriptResponseMessage;
import com.tinkerpop.rexster.protocol.message.SessionRequestMessage;
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

        if (message.getType() == MessageType.SCRIPT_REQUEST) {
            ScriptRequestMessage specificMessage = new ScriptRequestMessage(message);

            RexProSession session = RexProSessions.getSession(specificMessage.getSessionAsUUID().toString());

            try {
                Object result = session.evaluate(specificMessage.getScript(), specificMessage.getLanguageName(), specificMessage.getBindings());

                List<RexProMessage> messageList = new ArrayList<RexProMessage>();
                if (session.getChannel() == SessionRequestMessage.CHANNEL_CONSOLE) {
                    messageList.add(new ConsoleScriptResponseMessage(specificMessage.getSessionAsUUID(),
                            ScriptResponseMessage.FLAG_COMPLETE_MESSAGE, result, session.getBindings()));
                }

                ctx.write(messageList);

            } catch (ScriptException se) {
                logger.warn("Could not process script [" + specificMessage.getScript() + "] for language ["
                        + specificMessage.getLanguageName() + "] on session [" + message.getSessionAsUUID()
                        + "] and request [" + message.getRequestAsUUID() + "]");

                ctx.write(new ErrorResponseMessage(message.getSessionAsUUID(), message.getRequestAsUUID(),
                        ErrorResponseMessage.FLAG_ERROR_SCRIPT_FAILURE,
                        "An error occurred while processing the script for language [" + specificMessage.getLanguageName() + "]: " + se.getMessage()));

                return ctx.getStopAction();
            } catch (IOException ioe) {
                logger.error(ioe);
            }

            return ctx.getStopAction();
        }

        return ctx.getInvokeAction();
    }
}
