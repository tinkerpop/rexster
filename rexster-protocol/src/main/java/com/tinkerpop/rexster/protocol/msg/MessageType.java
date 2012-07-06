package com.tinkerpop.rexster.protocol.msg;

public class MessageType {
    public static final byte ERROR = 0;
    public static final byte SESSION_REQUEST = 1;
    public static final byte SESSION_RESPONSE = 2;
    public static final byte SCRIPT_REQUEST = 3;
    public static final byte CONSOLE_SCRIPT_RESPONSE = 4;
    public static final byte MSGPACK_SCRIPT_RESPONSE = 5;
}
