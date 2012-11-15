package com.tinkerpop.rexster.protocol.msg;

import com.tinkerpop.rexster.gremlin.converter.MsgPackResultConverter;
import org.msgpack.annotation.Message;

/**
 * Represents a response to a script request that formats results to MsgPack format.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
@Message
public class MsgPackScriptResponseMessage extends RexProMessage {
    private static final MsgPackResultConverter converter = new MsgPackResultConverter();

    public byte[] Results;
    public byte[] Bindings;

    public static byte[] convertResultToBytes(final Object result) throws Exception {
        return converter.convert(result);
    }

    @Override
    public int estimateMessageSize() {
        return BASE_MESSAGE_SIZE + (Results == null ? 0 : Results.length) + (Bindings == null ? 0 : Bindings.length);
    }
}
