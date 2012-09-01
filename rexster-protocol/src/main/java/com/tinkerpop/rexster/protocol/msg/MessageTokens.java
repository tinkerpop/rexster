package com.tinkerpop.rexster.protocol.msg;

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public final class MessageTokens {
    public static final String ERROR_INVALID_TAG = "The message has an invalid flag.";
    public static final String ERROR_SESSION_NOT_SPECIFIED = "The message does not specify a session.";
    public static final String ERROR_SESSION_INVALID = "The session on the request does not exist or has otherwise expired.";
    public static final String ERROR_IN_SCRIPT_PROCESSING = "An error occurred while processing the script for language [%s]: %s";
    public static final String ERROR_UNEXPECTED_MESSAGE_TYPE = "Message did not match an expected type.";
}
