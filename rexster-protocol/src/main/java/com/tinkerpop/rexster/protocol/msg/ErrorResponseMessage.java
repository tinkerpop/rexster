package com.tinkerpop.rexster.protocol.msg;

import org.msgpack.annotation.Message;

/**
 * Represents an Error message.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 * @author Blake Eggleston (bdeggleston.github.com)
 */
@Message
public class ErrorResponseMessage extends RexProMessage {
    public String ErrorMessage;

    public static final Integer INVALID_MESSAGE_ERROR = 0;
    public static final Integer INVALID_SESSION_ERROR = 1;
    public static final Integer SCRIPT_FAILURE_ERROR = 2;
    public static final Integer AUTH_FAILURE_ERROR = 3;
    public static final Integer GRAPH_CONFIG_ERROR = 4;
    public static final Integer CHANNEL_CONFIG_ERROR = 5;
    public static final Integer RESULT_SERIALIZATION_ERROR = 6;
    public static final Integer UNKNOWN_ERROR = 7;

    protected static final String FLAG_META_KEY = "flag";
    protected RexProMessageMetaField[] getMetaFields() {
        RexProMessageMetaField[] fields = {
            //indicates this session should be destroyed
            RexProMessageMetaField.define(FLAG_META_KEY, true, Integer.class)
        };
        return fields;
    }

    public Integer metaGetFlag() {
        Object value = Meta.get(FLAG_META_KEY);
        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof Long) {
            return ((Long) value).intValue();
        } else {
            return null;
        }
    }

    public void metaSetFlag(Integer val) {
        Meta.put(FLAG_META_KEY, val);
    }
}
