package com.tinkerpop.rexster.protocol;

import java.io.IOException;
import java.util.UUID;

public class ScriptResponseMessage extends RexProMessage {

    public static final byte FLAG_COMPLETE_MESSAGE = 0;
    public static final byte FLAG_FRAGMENT_MORE = 1;
    public static final byte FLAG_FRAMENT_FINAL = 2;

    public ScriptResponseMessage(RexProMessage message) {
        super(message.getVersion(), message.getType(), message.getFlag(),
                message.getSession(), message.getRequest(), message.getBody());

        if (this.getType() != MessageType.SCRIPT_RESPONSE) {
            throw new IllegalArgumentException("The message is not of type SCRIPT_RESPONSE");
        }
    }

    public ScriptResponseMessage(UUID sessionKey, byte flag, byte[] body) throws IOException {
        super(RexProMessage.CURRENT_VERSION, MessageType.SCRIPT_RESPONSE, flag,
                BitWorks.convertUUIDToByteArray(sessionKey),
                BitWorks.convertUUIDToByteArray(UUID.randomUUID()),
                body);
    }
}
