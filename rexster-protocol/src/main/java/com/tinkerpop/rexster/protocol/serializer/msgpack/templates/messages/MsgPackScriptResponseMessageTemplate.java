package com.tinkerpop.rexster.protocol.serializer.msgpack.templates.messages;

import com.tinkerpop.rexster.protocol.msg.ScriptResponseMessage;
import com.tinkerpop.rexster.protocol.serializer.msgpack.templates.BindingsTemplate;
import com.tinkerpop.rexster.protocol.serializer.msgpack.templates.ResultsTemplate;
import org.msgpack.packer.Packer;
import org.msgpack.unpacker.Unpacker;

import java.io.IOException;

/**
 * @author Blake Eggleston (bdeggleston.github.com)
 */
public class MsgPackScriptResponseMessageTemplate extends RexProMessageTemplate<ScriptResponseMessage> {

    protected int messageArraySize() {
        return super.messageArraySize() + 2;
    }

    @Override
    protected ScriptResponseMessage instantiateMessage() {
        return new ScriptResponseMessage();
    }

    @Override
    protected ScriptResponseMessage readMessageArray(final Unpacker un, final ScriptResponseMessage msg) throws IOException {
        ScriptResponseMessage message = super.readMessageArray(un, msg);

        message.Results = ResultsTemplate.getInstance().read(un, null);
        message.Bindings = BindingsTemplate.getInstance().read(un, null);
        return message;
    }

    @Override
    protected void writeMessageArray(final Packer pk, final ScriptResponseMessage msg) throws IOException {
        super.writeMessageArray(pk, msg);
        ResultsTemplate.getInstance().write(pk, msg.Results);
        BindingsTemplate.getInstance().write(pk, msg.Bindings);
    }
}
