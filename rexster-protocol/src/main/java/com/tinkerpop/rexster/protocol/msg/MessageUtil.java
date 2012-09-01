package com.tinkerpop.rexster.protocol.msg;

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
}
