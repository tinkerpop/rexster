package com.tinkerpop.rexster.protocol.msg;

import com.tinkerpop.rexster.client.RexProException;

import java.util.List;
import java.util.UUID;

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 * @author Blake Eggleston (bdeggleston.github.com)
 */
public class MessageUtil {
    public static ErrorResponseMessage createErrorResponse(final byte[] request, final byte[] session,
                                                           final Integer flag, final String errorMessage) {
        final ErrorResponseMessage msg = new ErrorResponseMessage();
        msg.Request = request;
        msg.Session = session;
        msg.ErrorMessage = errorMessage;

        msg.metaSetFlag(flag);
        try {
            msg.validateMetaData();
        } catch (RexProException ex) {
            //
        }
        return msg;
    }

    public static SessionResponseMessage createNewSession(byte[] request, final List<String> languages) {
        final UUID sessionKey = UUID.randomUUID();

        final SessionResponseMessage responseMessage = new SessionResponseMessage();
        responseMessage.setSessionAsUUID(sessionKey);
        responseMessage.Request = request;
        responseMessage.Languages = new String[languages.size()];
        languages.toArray(responseMessage.Languages);

        return responseMessage;
    }

    public static SessionResponseMessage createEmptySession(byte[] request) {
        final SessionResponseMessage responseMessage = new SessionResponseMessage();
        responseMessage.setSessionAsUUID(RexProMessage.EMPTY_SESSION);
        responseMessage.Request = request;
        responseMessage.Languages = new String[0];

        return responseMessage;
    }
}
