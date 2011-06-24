package com.tinkerpop.rexster.protocol;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.UUID;

public class ScriptRequestMessage extends RexProMessage {

    public ScriptRequestMessage(RexProMessage message) {
        super(message.getVersion(), message.getType(), message.getFlag(),
                message.getSession(), message.getRequest(), message.getBody());

        if (this.getType() != MessageType.SCRIPT_REQUEST) {
            throw new IllegalArgumentException("The message is not of type SCRIPT_REQUEST");
        }
    }

    public ScriptRequestMessage(UUID sessionKey, String languageName, String script) throws IOException {
        super(RexProMessage.CURRENT_VERSION, MessageType.SCRIPT_REQUEST, (byte) 0,
                BitWorks.convertUUIDToByteArray(sessionKey),
                BitWorks.convertUUIDToByteArray(UUID.randomUUID()),
                BitWorks.convertStringsToByteArray(languageName, script));
    }

    public String getLanguageName() {
        ByteBuffer buffer = ByteBuffer.wrap(this.body);
        int languageLength = buffer.getInt();

        byte[] languageBytes = new byte[languageLength];
        buffer.get(languageBytes);
        return new String(languageBytes);
    }

    public String getScript() {
        ByteBuffer buffer = ByteBuffer.wrap(this.body);
        int languageLength = buffer.getInt();

        int languageSegmentOffset = languageLength + 4;
        buffer.position(languageSegmentOffset);

        int scriptLength = buffer.getInt();

        byte[] scriptBytes = new byte[scriptLength];
        buffer.get(scriptBytes);
        return new String(scriptBytes);
    }
}
