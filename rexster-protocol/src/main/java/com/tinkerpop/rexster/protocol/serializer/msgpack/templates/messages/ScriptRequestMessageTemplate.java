package com.tinkerpop.rexster.protocol.serializer.msgpack.templates.messages;

import com.tinkerpop.rexster.protocol.msg.ScriptRequestMessage;
import com.tinkerpop.rexster.protocol.serializer.msgpack.templates.BindingsTemplate;
import org.msgpack.packer.Packer;
import org.msgpack.unpacker.Unpacker;

import java.io.IOException;

/**
 * @author Blake Eggleston (bdeggleston.github.com)
 */
public class ScriptRequestMessageTemplate extends RexProMessageTemplate<ScriptRequestMessage> {
    protected int messageArraySize() {
        return super.messageArraySize() + 3;
    }

    protected ScriptRequestMessage instantiateMessage() {
        return new ScriptRequestMessage();
    }

    protected void writeMessageArray(final Packer pk, final ScriptRequestMessage msg) throws IOException {
        super.writeMessageArray(pk, msg);
        pk.write(msg.LanguageName);
        pk.write(msg.Script);
        BindingsTemplate.getInstance().write(pk, msg.Bindings);
    }

    protected ScriptRequestMessage readMessageArray(final Unpacker un, final ScriptRequestMessage msg) throws IOException {
        ScriptRequestMessage message = super.readMessageArray(un, msg);
        message.LanguageName = un.trySkipNil()?null:un.readString();
        message.Script = un.trySkipNil()?null:un.readString();
        message.Bindings = BindingsTemplate.getInstance().read(un, null);
        return message;
    }
}
