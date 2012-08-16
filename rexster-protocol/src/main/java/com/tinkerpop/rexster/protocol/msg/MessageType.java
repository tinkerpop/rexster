package com.tinkerpop.rexster.protocol.msg;

/**
 * Values that represent standard message types for RexPro.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class MessageType {
    /**
     * Represents an Error message.
     */
    public static final byte ERROR = 0;

    /**
     * Represents a request to open or close a session.
     */
    public static final byte SESSION_REQUEST = 1;

    /**
     * Represents a response to a session request with a newly defined session and available ScriptEngine
     * languages or a closed session confirmation..
     */
    public static final byte SESSION_RESPONSE = 2;

    /**
     * Represents a request to process a script.
     */
    public static final byte SCRIPT_REQUEST = 3;

    /**
     * Represents a response to a script request for use in a console where each results is serialized to a
     * string value in an array of strings.
     */
    public static final byte CONSOLE_SCRIPT_RESPONSE = 4;

    /**
     * Represents a response to a script request that formats results to MsgPack format.
     */
    public static final byte MSGPACK_SCRIPT_RESPONSE = 5;
}
