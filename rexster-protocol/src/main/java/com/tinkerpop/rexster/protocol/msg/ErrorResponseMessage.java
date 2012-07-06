package com.tinkerpop.rexster.protocol.msg;

import org.msgpack.annotation.Message;

@Message
public class ErrorResponseMessage extends RexProMessage {
    public static final byte FLAG_ERROR_MESSAGE_VALIDATION = (byte) 0;
    public static final byte FLAG_ERROR_INVALID_SESSION = (byte) 1;
    public static final byte FLAG_ERROR_SCRIPT_FAILURE = (byte) 2;
    public static final byte FLAG_ERROR_AUTHENTICATION_FAILURE = (byte) 3;

    public String ErrorMessage;
}
