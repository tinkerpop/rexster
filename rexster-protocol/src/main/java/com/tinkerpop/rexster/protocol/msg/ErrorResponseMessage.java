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
}
