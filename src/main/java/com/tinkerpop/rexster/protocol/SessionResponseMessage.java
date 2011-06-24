package com.tinkerpop.rexster.protocol;

import java.util.UUID;

public class SessionResponseMessage extends RexProMessage{
    public SessionResponseMessage(RexProMessage message) {
        super(message.getVersion(), message.getType(), message.getFlag(),
                message.getSession(), message.getRequest(), message.getBody());
    }

    public SessionResponseMessage(UUID sessionKey, UUID request) {
        super(RexProMessage.CURRENT_VERSION, MessageType.SESSION_RESPONSE, (byte) 0,
                BitWorks.convertUUIDToByteArray(sessionKey),
                BitWorks.convertUUIDToByteArray(request),
                new byte[0]);
    }
}
