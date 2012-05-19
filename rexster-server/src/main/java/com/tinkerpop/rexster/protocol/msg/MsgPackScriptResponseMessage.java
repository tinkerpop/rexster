package com.tinkerpop.rexster.protocol.msg;

import com.tinkerpop.rexster.gremlin.converter.MsgPackResultConverter;
import org.msgpack.annotation.Message;

import java.util.List;

@Message
public class MsgPackScriptResponseMessage extends RexProMessage {
    public static final byte FLAG_COMPLETE_MESSAGE = 0;
    public byte[] Results;
    public byte[] Bindings;

    public static byte[] convertResultToConsoleLines(Object result) throws Exception {
        MsgPackResultConverter converter = new MsgPackResultConverter();
        return converter.convert(result);
    }
}
