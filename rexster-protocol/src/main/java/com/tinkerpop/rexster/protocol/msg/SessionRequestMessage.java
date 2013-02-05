package com.tinkerpop.rexster.protocol.msg;

import org.msgpack.annotation.Message;

/**
 * Represents a request to open or close a session.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 * @author Blake Eggleston (bdeggleston.github.com)
 */
@Message
public class SessionRequestMessage extends RexProMessage {
    public static final byte CHANNEL_NONE = 0;
    public static final byte CHANNEL_CONSOLE = 1;
    public static final byte CHANNEL_MSGPACK = 2;
    public static final byte CHANNEL_GRAPHSON = 3;

    protected static final String KILL_SESSION_META_KEY = "killSession";
    protected RexProMessageMetaField[] getMetaFields() {
        RexProMessageMetaField[] fields = {
            //indicates this session should be destroyed
            RexProMessageMetaField.define(KILL_SESSION_META_KEY, false, false, Boolean.class)
        };
        return fields;
    }

    public byte Channel;
    public String Username;
    public String Password;

    @Override
    public int estimateMessageSize() {
        return BASE_MESSAGE_SIZE + 1
                + (Username == null ? 0 : Username.length())
                + (Password == null ? 0 :Password.length());
    }

    public void metaSetKillSession(Boolean val) {
        Meta.put(KILL_SESSION_META_KEY, val);
    }

    public Boolean metaGetKillSession() {
        return (Boolean) Meta.get(KILL_SESSION_META_KEY);
    }
}
