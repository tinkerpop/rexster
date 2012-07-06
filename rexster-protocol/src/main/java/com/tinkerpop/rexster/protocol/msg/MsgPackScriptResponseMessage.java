package com.tinkerpop.rexster.protocol.msg;

import com.tinkerpop.rexster.gremlin.converter.MsgPackResultConverter;
import org.msgpack.annotation.Message;

@Message
public class MsgPackScriptResponseMessage extends RexProMessage {
    private static final MsgPackResultConverter converter = new MsgPackResultConverter();
    public static final byte FLAG_COMPLETE_MESSAGE = 0;
    public byte[] Results;
    public byte[] Bindings;

    public static byte[] convertResultToBytes(Object result) throws Exception {
        return converter.convert(result);
    }
}
