package com.tinkerpop.rexster.protocol.serializer.msgpack.templates.messages;

import com.tinkerpop.rexster.protocol.msg.SessionResponseMessage;
import org.msgpack.packer.Packer;
import org.msgpack.unpacker.Unpacker;

import java.io.IOException;

/**
 * @author Blake Eggleston (bdeggleston.github.com)
 */
public class SessionResponseMessageTemplate extends RexProMessageTemplate<SessionResponseMessage> {

    protected int messageArraySize() {
        return super.messageArraySize() + 1;
    }

    protected SessionResponseMessage instantiateMessage() {
        return new SessionResponseMessage();
    }

    protected void writeMessageArray(final Packer pk, final SessionResponseMessage msg) throws IOException {
        super.writeMessageArray(pk, msg);
        pk.writeArrayBegin(msg.Languages.length);
        for (String lang : msg.Languages) {
            pk.write(lang);
        }
        pk.writeArrayEnd();
    }

    protected SessionResponseMessage readMessageArray(final Unpacker un, final SessionResponseMessage msg) throws IOException {
        SessionResponseMessage message = super.readMessageArray(un, msg);
        if (!un.trySkipNil()){
            message.Languages = new String[un.readArrayBegin()];
            for (int i=0; i<message.Languages.length; i++) {
                message.Languages[i] = un.readString();
            }
            un.readArrayEnd();
        }
        return message;
    }
}
