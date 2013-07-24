package com.tinkerpop.rexster.protocol.serializer.msgpack.templates.messages;

import com.tinkerpop.rexster.protocol.msg.RexProMessage;
import com.tinkerpop.rexster.protocol.serializer.msgpack.templates.MetaTemplate;
import org.msgpack.packer.Packer;
import org.msgpack.template.AbstractTemplate;
import org.msgpack.unpacker.Unpacker;

import java.io.IOException;

/**
 * Base serializer for rexpro messages
 *
 * @author Blake Eggleston (bdeggleston.github.com)
 */
public abstract class RexProMessageTemplate<Message extends RexProMessage> extends AbstractTemplate<Message> {

    @Override
    public void write(Packer pk, Message v) throws IOException {
        write(pk, v, false);
    }

    /**
     * Extend this in subclasses with their contribution to the
     * message length
     *
     * @return message array length
     */
    protected int messageArraySize() {
        return 3;
    }

    /**
     * @return an instance of the message type for this template implementation
     */
    protected abstract Message instantiateMessage();

    protected void writeMessageArray(final Packer pk, final Message msg) throws IOException {
        pk.write(msg.Session);
        pk.write(msg.Request);
        MetaTemplate.getInstance().write(pk, msg.Meta);
    }

    public void write(final Packer pk, final Message v, final boolean required) throws IOException {
        pk.writeArrayBegin(messageArraySize());
        writeMessageArray(pk, v);
        pk.writeArrayEnd();
    }

    @Override
    public Message read(Unpacker u, Message to) throws IOException {
        return read(u, to, false);
    }

    protected Message readMessageArray(final Unpacker un, final Message msg) throws IOException {
        msg.Session = un.trySkipNil()?null:un.readByteArray();
        msg.Request = un.trySkipNil()?null:un.readByteArray();
        msg.Meta = MetaTemplate.getInstance().read(un, null);
        return msg;
    }

    public Message read(final Unpacker u, final Message to, final boolean required) throws IOException {
        u.readArrayBegin();
        Message msg = readMessageArray(u, to==null?instantiateMessage():to);
        u.readArrayEnd();
        return msg;
    }

}
