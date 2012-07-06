package com.tinkerpop.rexster.protocol.msg;

import org.msgpack.annotation.Message;

@Message
public class SessionResponseMessage extends RexProMessage {
    public String[] Languages;
}
