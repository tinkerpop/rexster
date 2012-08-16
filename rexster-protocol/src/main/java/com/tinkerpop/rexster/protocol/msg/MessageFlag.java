package com.tinkerpop.rexster.protocol.msg;

/**
 * Flags for the various messages.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class MessageFlag {
    public static final byte SCRIPT_RESPONSE_COMPLETE_MESSAGE = 0;

    public static final byte ERROR_MESSAGE_VALIDATION = 0;
    public static final byte ERROR_INVALID_SESSION = 1;
    public static final byte ERROR_SCRIPT_FAILURE = 2;
    public static final byte ERROR_AUTHENTICATION_FAILURE = 3;

    public static final byte SCRIPT_REQUEST_IN_SESSION = 0;
    public static final byte SCRIPT_REQUEST_NO_SESSION = 1;

    public static final byte SESSION_REQUEST_NEW_SESSION = 0;
    public static final byte SESSION_REQUEST_KILL_SESSION = 1;
}
