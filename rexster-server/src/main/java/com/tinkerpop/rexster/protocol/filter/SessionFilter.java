package com.tinkerpop.rexster.protocol.filter;

import com.tinkerpop.rexster.RexsterApplication;
import com.tinkerpop.rexster.protocol.EngineController;
import com.tinkerpop.rexster.protocol.RexProSessions;
import com.tinkerpop.rexster.protocol.message.ErrorResponseMessage;
import com.tinkerpop.rexster.protocol.message.MessageType;
import com.tinkerpop.rexster.protocol.message.RexProMessage;
import com.tinkerpop.rexster.protocol.message.SessionRequestMessage;
import com.tinkerpop.rexster.protocol.message.SessionResponseMessage;
import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;

import java.io.IOException;
import java.util.UUID;

public class SessionFilter extends BaseFilter {

    private final RexsterApplication rexsterApplication;

    public SessionFilter(final RexsterApplication rexsterApplication) {
        this.rexsterApplication = rexsterApplication;
    }

    public NextAction handleRead(final FilterChainContext ctx) throws IOException {
        final RexProMessage message = ctx.getMessage();

        if (message.getType() == MessageType.SESSION_REQUEST) {
            SessionRequestMessage specificMessage = new SessionRequestMessage(message);

            if (specificMessage.getFlag() == SessionRequestMessage.FLAG_NEW_SESSION) {
                UUID sessionKey = UUID.randomUUID();

                // construct a session with the right channel
                RexProSessions.ensureSessionExists(sessionKey.toString(), this.rexsterApplication,
                        specificMessage.getChannel(), specificMessage.getChunkSize());

                EngineController engineController = EngineController.getInstance();

                ctx.write(new SessionResponseMessage(sessionKey, specificMessage.getRequestAsUUID(),
                        engineController.getAvailableEngineLanguages()));

            } else if (specificMessage.getFlag() == SessionRequestMessage.FLAG_KILL_SESSION) {
                RexProSessions.destroySession(specificMessage.getSessionAsUUID().toString());
                ctx.write(new SessionResponseMessage(RexProMessage.EMPTY_SESSION, specificMessage.getRequestAsUUID(), null));
            } else {
                // there is no session to this message...that's a problem
                ctx.write(new ErrorResponseMessage(RexProMessage.EMPTY_SESSION, message.getRequestAsUUID(),
                        ErrorResponseMessage.FLAG_ERROR_MESSAGE_VALIDATION,
                        "The message has an invalid flag."));
            }

            // nothing left to do...session was created
            return ctx.getStopAction();
        }

        if (!message.hasSession()) {
            // there is no session to this message...that's a problem
            ctx.write(new ErrorResponseMessage(RexProMessage.EMPTY_SESSION, message.getRequestAsUUID(),
                    ErrorResponseMessage.FLAG_ERROR_MESSAGE_VALIDATION,
                    "The message does not specify a session."));

            return ctx.getStopAction();
        }

        if (!RexProSessions.hasSessionKey(message.getSessionAsUUID().toString())) {
            // the message is assigned a session that does not exist on the server
            ctx.write(new ErrorResponseMessage(RexProMessage.EMPTY_SESSION, message.getRequestAsUUID(),
                    ErrorResponseMessage.FLAG_ERROR_INVALID_SESSION,
                    "The session on the request does not exist or has otherwise expired."));

            return ctx.getStopAction();
        }

        return ctx.getInvokeAction();
    }
}
