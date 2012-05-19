package com.tinkerpop.rexster.protocol.msg;

import org.msgpack.annotation.Message;

@Message
public class SessionRequestMessage extends RexProMessage {
    public static final int DEFAULT_CHUNK_SIZE = 8192;

    public static final byte CHANNEL_NONE = 0;

    public static final byte CHANNEL_CONSOLE = 1;
    public static final byte CHANNEL_MSGPACK = 2;

    /**
     * Starts a session.
     */
    public static final byte FLAG_NEW_SESSION = 0;

    /**
     * Destroy a session.
     */
    public static final byte FLAG_KILL_SESSION = 1;

    public byte Channel;
    public String Username;
    public String Password;
}
