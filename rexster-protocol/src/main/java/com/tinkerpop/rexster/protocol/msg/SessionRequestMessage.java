package com.tinkerpop.rexster.protocol.msg;

import org.msgpack.annotation.Message;

/**
 * Represents a request to open or close a session.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
@Message
public class SessionRequestMessage extends RexProMessage {
    public static final byte CHANNEL_NONE = 0;
    public static final byte CHANNEL_CONSOLE = 1;
    public static final byte CHANNEL_MSGPACK = 2;
    public static final byte CHANNEL_GRAPHSON = 3;

    public byte Channel;
    public String Username;
    public String Password;
}
