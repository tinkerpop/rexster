package com.tinkerpop.rexster.protocol.msg;

import java.util.List;
import java.util.UUID;

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class MessageUtil {
    public static ErrorResponseMessage createErrorResponse(final byte[] request, final byte[] session,
                                                           final byte flag, final String errorMessage) {
        final ErrorResponseMessage msg = new ErrorResponseMessage();
        msg.Request = request;
        msg.Session = session;
        msg.ErrorMessage = errorMessage;
        msg.Flag = flag;

        return msg;
    }

    public static SessionResponseMessage createNewSession(byte[] request, final List<String> languages) {
        final UUID sessionKey = UUID.randomUUID();

        final SessionResponseMessage responseMessage = new SessionResponseMessage();
        responseMessage.setSessionAsUUID(sessionKey);
        responseMessage.Request = request;
        responseMessage.Flag = MessageFlag.SESSION_RESPONSE_NO_FLAG;
        responseMessage.Languages = new String[languages.size()];
        languages.toArray(responseMessage.Languages);

        return responseMessage;
    }

    public static SessionResponseMessage createEmptySession(byte[] request) {
        final SessionResponseMessage responseMessage = new SessionResponseMessage();
        responseMessage.setSessionAsUUID(RexProMessage.EMPTY_SESSION);
        responseMessage.Request = request;
        responseMessage.Languages = new String[0];
        responseMessage.Flag = MessageFlag.SESSION_RESPONSE_NO_FLAG;

        return responseMessage;
    }
}
