package com.tinkerpop.rexster.protocol.message;

import com.tinkerpop.rexster.protocol.BitWorks;

import java.util.UUID;

public class ErrorResponseMessage extends RexProMessage {

    public static final byte FLAG_ERROR_MESSAGE_VALIDATION = (byte) 0;
    public static final byte FLAG_ERROR_INVALID_SESSION = (byte) 1;
    public static final byte FLAG_ERROR_SCRIPT_FAILURE = (byte) 2;
    public static final byte FLAG_ERROR_AUTHENTICATION_FAILURE = (byte) 3;

    public ErrorResponseMessage(RexProMessage message) {
        super(message.getVersion(), message.getType(), message.getFlag(),
                message.getSession(), message.getRequest(), message.getBody());

        if (this.getType() != MessageType.ERROR) {
            throw new IllegalArgumentException("The message is not of type ERROR");
        }
    }

    public ErrorResponseMessage(UUID session, UUID request, byte flag, String message) {
        super(RexProMessage.CURRENT_VERSION, MessageType.ERROR, flag,
                BitWorks.convertUUIDToByteArray(session),
                BitWorks.convertUUIDToByteArray(request),
                message.getBytes());
    }

    public String getErrorMessage() {
        return new String(this.body);
    }
}
