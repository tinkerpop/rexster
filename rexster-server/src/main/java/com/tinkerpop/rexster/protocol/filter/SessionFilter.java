package com.tinkerpop.rexster.protocol.filter;

import com.tinkerpop.rexster.RexsterApplication;
import com.tinkerpop.rexster.protocol.EngineController;
import com.tinkerpop.rexster.protocol.RexProSessions;
import com.tinkerpop.rexster.protocol.msg.ErrorResponseMessage;
import com.tinkerpop.rexster.protocol.msg.RexProMessage;
import com.tinkerpop.rexster.protocol.msg.SessionRequestMessage;
import com.tinkerpop.rexster.protocol.msg.SessionResponseMessage;
import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.UUID;

public class SessionFilter extends BaseFilter {

    private RexsterApplication rexsterApplication;

    public SessionFilter(RexsterApplication rexsterApplication) {
        this.rexsterApplication = rexsterApplication;
    }

    public NextAction handleRead(final FilterChainContext ctx) throws IOException {
        final RexProMessage message = ctx.getMessage();

        if (message instanceof SessionRequestMessage) {
            SessionRequestMessage specificMessage = (SessionRequestMessage) message;

            if (specificMessage.Flag == SessionRequestMessage.FLAG_NEW_SESSION) {
                UUID sessionKey = UUID.randomUUID();

                // construct a session with the right channel
                RexProSessions.ensureSessionExists(sessionKey.toString(), this.rexsterApplication,
                        specificMessage.Channel, SessionRequestMessage.DEFAULT_CHUNK_SIZE);

                EngineController engineController = EngineController.getInstance();
                ArrayList<String> langs = new ArrayList<String>();
                Iterator<String> itty = engineController.getAvailableEngineLanguages();
                while (itty.hasNext()) {
                    langs.add(itty.next());
                }

                SessionResponseMessage responseMessage = new SessionResponseMessage();
                responseMessage.setSessionAsUUID(sessionKey);
                responseMessage.Request = specificMessage.Request;
                responseMessage.Flag = (byte) 0;
                responseMessage.Languages = new String[langs.size()];
                langs.toArray(responseMessage.Languages);

                ctx.write(responseMessage);

            } else if (specificMessage.Flag == SessionRequestMessage.FLAG_KILL_SESSION) {
                RexProSessions.destroySession(specificMessage.sessionAsUUID().toString());
                SessionResponseMessage responseMessage = new SessionResponseMessage();
                responseMessage.setSessionAsUUID(RexProMessage.EMPTY_SESSION);
                responseMessage.Request = specificMessage.Request;
                responseMessage.Languages = new String[0];
                responseMessage.Flag = (byte) 0;

                ctx.write(responseMessage);
            } else {
                // there is no session to this message...that's a problem
                ErrorResponseMessage errorMessage = new ErrorResponseMessage();
                errorMessage.setSessionAsUUID(RexProMessage.EMPTY_SESSION);
                errorMessage.Request = specificMessage.Request;
                errorMessage.ErrorMessage = "The message has an invalid flag.";
                errorMessage.Flag = ErrorResponseMessage.FLAG_ERROR_MESSAGE_VALIDATION;

                ctx.write(errorMessage);
            }

            // nothing left to do...session was created
            return ctx.getStopAction();
        }

        if (!message.hasSession()) {
            // there is no session to this message...that's a problem
            ErrorResponseMessage errorMessage = new ErrorResponseMessage();
            errorMessage.setSessionAsUUID(RexProMessage.EMPTY_SESSION);
            errorMessage.Request = message.Request;
            errorMessage.ErrorMessage = "The message does not specify a session.";
            errorMessage.Flag = ErrorResponseMessage.FLAG_ERROR_MESSAGE_VALIDATION;

            ctx.write(errorMessage);

            return ctx.getStopAction();
        }

        if (!RexProSessions.hasSessionKey(message.sessionAsUUID().toString())) {
            // the message is assigned a session that does not exist on the server
            ErrorResponseMessage errorMessage = new ErrorResponseMessage();
            errorMessage.setSessionAsUUID(RexProMessage.EMPTY_SESSION);
            errorMessage.Request = message.Request;
            errorMessage.ErrorMessage = "The session on the request does not exist or has otherwise expired.";
            errorMessage.Flag = ErrorResponseMessage.FLAG_ERROR_INVALID_SESSION;

            ctx.write(errorMessage);

            return ctx.getStopAction();
        }

        return ctx.getInvokeAction();
    }
}
