package com.tinkerpop.rexster.protocol.message;

import com.tinkerpop.rexster.protocol.BitWorks;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public class SessionResponseMessage extends RexProMessage {
    public SessionResponseMessage(RexProMessage message) {
        super(message.getVersion(), message.getType(), message.getFlag(),
                message.getSession(), message.getRequest(), message.getBody());

        if (this.getType() != MessageType.SESSION_RESPONSE) {
            throw new IllegalArgumentException("The message is not of type SESSION_RESPONSE");
        }
    }

    public SessionResponseMessage(UUID sessionKey, UUID request, Iterator<String> languages) {
        super(RexProMessage.CURRENT_VERSION, MessageType.SESSION_RESPONSE, (byte) 0,
                BitWorks.convertUUIDToByteArray(sessionKey),
                BitWorks.convertUUIDToByteArray(request),
                convertLanguagesToBytes(languages));
    }

    private static byte[] convertLanguagesToBytes(Iterator<String> languages) {

        if (languages != null && languages.hasNext()) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            try {
                while (languages.hasNext()) {
                    baos.write(BitWorks.convertStringsToByteArray(languages.next()));
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }

            return baos.toByteArray();
        } else {
            return new byte[0];
        }
    }

    public List<String> getLanguages() {
        ArrayList<String> languages = new ArrayList<String>();

        if (this.body != null && this.body.length > 0) {
            ByteBuffer buffer = ByteBuffer.wrap(this.body);
            while (buffer.hasRemaining()) {
                byte[] langBytes = new byte[buffer.getInt()];
                buffer.get(langBytes);
                languages.add(new String(langBytes));
            }
        }

        return languages;
    }
}
