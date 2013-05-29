package com.tinkerpop.rexster.protocol.serializer.msgpack.templates.messages;

import com.tinkerpop.rexster.protocol.msg.SessionRequestMessage;
import org.msgpack.packer.Packer;
import org.msgpack.unpacker.Unpacker;

import java.io.IOException;

public class SessionRequestMessageTemplate extends RexProMessageTemplate<SessionRequestMessage> {

    protected int messageArraySize() {
        return super.messageArraySize() + 3;
    }

    protected SessionRequestMessage instantiateMessage() {
        return new SessionRequestMessage();
    }

    protected void writeMessageArray(final Packer pk, final SessionRequestMessage msg) throws IOException {
        super.writeMessageArray(pk, msg);
        pk.write(msg.Channel);
        pk.write(msg.Username);
        pk.write(msg.Password);
    }

    protected SessionRequestMessage readMessageArray(final Unpacker un, final SessionRequestMessage msg) throws IOException {
        SessionRequestMessage message = super.readMessageArray(un, msg);
        message.Channel = un.readInt();
        message.Username = un.readString();
        message.Password = un.readString();
        return message;
    }

}
