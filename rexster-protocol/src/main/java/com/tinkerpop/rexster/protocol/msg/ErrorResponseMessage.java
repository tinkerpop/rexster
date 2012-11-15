package com.tinkerpop.rexster.protocol.msg;

import org.msgpack.annotation.Message;

/**
 * Represents an Error message.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
@Message
public class ErrorResponseMessage extends RexProMessage {
    public String ErrorMessage;

    @Override
    public int estimateMessageSize() {
        return BASE_MESSAGE_SIZE + (ErrorMessage == null ? 0 : ErrorMessage.length());
    }
}
