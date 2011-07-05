package com.tinkerpop.rexster.protocol.message;

import com.tinkerpop.rexster.protocol.BitWorks;
import com.tinkerpop.rexster.protocol.MessageType;
import com.tinkerpop.rexster.protocol.RexProMessage;

import java.util.UUID;

public class ErrorResponseMessage extends RexProMessage {

    public static final byte FLAG_ERROR_MESSAGE_VALIDATION = (byte) 0;
    public static final byte FLAG_ERROR_INVALID_SESSION = (byte) 1;
    public static final byte FLAG_ERROR_SCRIPT_FAILURE = (byte) 2;

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
