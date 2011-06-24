package com.tinkerpop.rexster.protocol;

import java.util.UUID;

public class SessionRequestMessage extends RexProMessage {

    public SessionRequestMessage(RexProMessage message) {
        super(message.getVersion(), message.getType(), message.getFlag(),
                message.getSession(), message.getRequest(), message.getBody());

        if (this.getType() != MessageType.SESSION_REQUEST) {
            throw new IllegalArgumentException("The message is not of type SESSION_REQUEST");
        }
    }

    public SessionRequestMessage() {
        super(RexProMessage.CURRENT_VERSION, MessageType.SESSION_REQUEST, (byte) 0,
                BitWorks.convertUUIDToByteArray(EMPTY_SESSION),
                BitWorks.convertUUIDToByteArray(UUID.randomUUID()),
                new byte[0]);
    }
}
