package com.tinkerpop.rexster.protocol.serializer.msgpack.templates.messages;

import com.tinkerpop.rexster.protocol.msg.MsgPackScriptResponseMessage;

/**
 * Created with IntelliJ IDEA.
 * User: bdeggleston
 * Date: 6/8/13
 * Time: 9:01 PM
 * To change this template use File | Settings | File Templates.
 */
public class MsgPackScriptResponseMessageTemplate extends RexProMessageTemplate<MsgPackScriptResponseMessage> {

    @Override
    protected MsgPackScriptResponseMessage instantiateMessage() {
        return new MsgPackScriptResponseMessage();
    }
}
