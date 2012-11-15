package com.tinkerpop.rexster.protocol.msg;

import org.msgpack.annotation.Message;

/**
 * Represents a response to a session request with a newly defined session and available ScriptEngine
 * languages or a closed session confirmation.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
@Message
public class SessionResponseMessage extends RexProMessage {
    public String[] Languages;

    @Override
    public int estimateMessageSize() {
        return BASE_MESSAGE_SIZE + estimateLanguagesSize();
    }

    private int estimateLanguagesSize() {
        int size = 0;
        if (Languages != null) {
            for(String l : Languages) {
                size = size + l.length();
            }
        }

        return size;
    }
}
