package com.tinkerpop.rexster.protocol.serializer.msgpack.templates.messages;

import com.tinkerpop.rexster.protocol.msg.ConsoleScriptResponseMessage;
import com.tinkerpop.rexster.protocol.serializer.msgpack.templates.BindingsTemplate;
import org.msgpack.packer.Packer;
import org.msgpack.unpacker.Unpacker;

import java.io.IOException;

/**
 */
public class ConsoleScriptResponseMessageTemplate extends RexProMessageTemplate<ConsoleScriptResponseMessage> {

    @Override
    protected ConsoleScriptResponseMessage instantiateMessage() {
        return new ConsoleScriptResponseMessage();
    }

    @Override
    protected void writeMessageArray(final Packer pk, final ConsoleScriptResponseMessage msg) throws IOException {
        super.writeMessageArray(pk, msg);
        pk.writeArrayBegin(msg.ConsoleLines.length);
        for (String line : msg.ConsoleLines) {
            pk.write(line);
        }
        pk.writeArrayEnd();
        BindingsTemplate.getInstance().write(pk, msg.Bindings);
    }

    @Override
    protected ConsoleScriptResponseMessage readMessageArray(final Unpacker un, final ConsoleScriptResponseMessage msg) throws IOException {
        ConsoleScriptResponseMessage message = super.readMessageArray(un, msg);
        if (un.trySkipNil()) {
            int numLines = un.readArrayBegin();
            message.ConsoleLines = new String[numLines];
            for (int i=0; i<numLines; i++) {
                message.ConsoleLines[i] = un.readString();
            }
            un.readArrayEnd();
        }
        message.Bindings = BindingsTemplate.getInstance().read(un, null);
        return message;
    }
}
