package com.tinkerpop.rexster.protocol;

import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;

import java.io.IOException;
import java.util.UUID;

public class SessionFilter extends BaseFilter {
    public NextAction handleRead(final FilterChainContext ctx) throws IOException {
        final RexProMessage message = ctx.getMessage();

        if (message.getType() == MessageType.SESSION_REQUEST) {
            SessionRequestMessage specificMessage = new SessionRequestMessage(message);

            // TODO: bother checking for empty UUID?  prolly should even if it is just ignored.

            UUID sessionKey = UUID.randomUUID();
            RexProSessions.getSession(sessionKey);

            ctx.write(new SessionResponseMessage(sessionKey, specificMessage.getRequestAsUUID()));

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

        if (!RexProSessions.hasSessionKey(message.getSessionAsUUID())) {
            // the message is assigned a session that does not exist on the server
            ctx.write(new ErrorResponseMessage(RexProMessage.EMPTY_SESSION, message.getRequestAsUUID(),
                    ErrorResponseMessage.FLAG_ERROR_INVALID_SESSION,
                    "The session on the request does not exist or has otherwise expired."));

            return ctx.getStopAction();
        }

        return ctx.getInvokeAction();
    }
}
