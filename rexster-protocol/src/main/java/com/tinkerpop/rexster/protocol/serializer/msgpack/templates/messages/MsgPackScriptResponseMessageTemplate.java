package com.tinkerpop.rexster.protocol.serializer.msgpack.templates.messages;

import com.tinkerpop.rexster.protocol.msg.MsgPackScriptResponseMessage;
import com.tinkerpop.rexster.protocol.serializer.msgpack.templates.BindingsTemplate;
import com.tinkerpop.rexster.protocol.serializer.msgpack.templates.ResultsTemplate;
import org.msgpack.packer.Packer;
import org.msgpack.unpacker.Unpacker;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: bdeggleston
 * Date: 6/8/13
 * Time: 9:01 PM
 * To change this template use File | Settings | File Templates.
 */
public class MsgPackScriptResponseMessageTemplate extends RexProMessageTemplate<MsgPackScriptResponseMessage> {

    protected int messageArraySize() {
        return super.messageArraySize() + 2;
    }

    @Override
    protected MsgPackScriptResponseMessage instantiateMessage() {
        return new MsgPackScriptResponseMessage();
    }

    @Override
    protected MsgPackScriptResponseMessage readMessageArray(final Unpacker un, final MsgPackScriptResponseMessage msg) throws IOException {
        MsgPackScriptResponseMessage message = super.readMessageArray(un, msg);

        message.Results = ResultsTemplate.getInstance().read(un, null);
        message.Bindings = BindingsTemplate.getInstance().read(un, null);
        return message;
    }

    @Override
    protected void writeMessageArray(final Packer pk, final MsgPackScriptResponseMessage msg) throws IOException {
        super.writeMessageArray(pk, msg);
        ResultsTemplate.getInstance().write(pk, msg.Results);
        BindingsTemplate.getInstance().write(pk, msg.Bindings);
    }
}
