package com.tinkerpop.rexster.protocol.serializer.msgpack.templates.messages;

import com.tinkerpop.rexster.protocol.msg.GraphSONScriptResponseMessage;
import com.tinkerpop.rexster.protocol.serializer.msgpack.templates.BindingsTemplate;
import org.msgpack.packer.Packer;
import org.msgpack.unpacker.Unpacker;

import java.io.IOException;

/**
 */
public class GraphSONScriptResponseMessageTemplate extends RexProMessageTemplate<GraphSONScriptResponseMessage> {

    @Override
    protected GraphSONScriptResponseMessage instantiateMessage() {
        return new GraphSONScriptResponseMessage();
    }

    @Override
    protected void writeMessageArray(final Packer pk, final GraphSONScriptResponseMessage msg) throws IOException {
        super.writeMessageArray(pk, msg);
        pk.write(msg.Results);
        BindingsTemplate.getInstance().write(pk, msg.Bindings);
    }

    @Override
    protected GraphSONScriptResponseMessage readMessageArray(final Unpacker un, final GraphSONScriptResponseMessage msg) throws IOException {
        GraphSONScriptResponseMessage message = super.readMessageArray(un, msg);
        message.Results = un.readString();
        message.Bindings = BindingsTemplate.getInstance().read(un, null);
        return message;
    }
}
