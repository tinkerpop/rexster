package com.tinkerpop.rexster.protocol.serializer.msgpack;

import com.tinkerpop.rexster.protocol.msg.*;
import com.tinkerpop.rexster.protocol.serializer.RexProSerializer;
import com.tinkerpop.rexster.protocol.serializer.msgpack.templates.MetaTemplate;
import com.tinkerpop.rexster.protocol.serializer.msgpack.templates.ResultsTemplate;
import com.tinkerpop.rexster.protocol.serializer.msgpack.templates.messages.*;
import org.msgpack.MessagePack;
import org.msgpack.packer.BufferPacker;
import org.msgpack.unpacker.Unpacker;

import java.io.IOException;

public class MsgPackSerializer implements RexProSerializer {

    public static final byte SERIALIZER_ID = 0;

    private static final MessagePack msgpack = new MessagePack();
    static {
        //todo: get rid of the special classes and implement their serialization in the message templates
        msgpack.register(RexProMessageMeta.class, new MetaTemplate());
        msgpack.register(RexProBindings.class, RexProBindings.SerializationTemplate.getInstance());
        msgpack.register(RexProScriptResult.class, ResultsTemplate.getInstance());

        //todo: write a bit about why this is required
        msgpack.register(ErrorResponseMessage.class, new ErrorResponseMessageTemplate());
        msgpack.register(SessionRequestMessage.class, new SessionRequestMessageTemplate());
        msgpack.register(SessionResponseMessage.class, new SessionResponseMessageTemplate());
        msgpack.register(ScriptRequestMessage.class, new ScriptRequestMessageTemplate());
        msgpack.register(ScriptResponseMessage.class, new MsgPackScriptResponseMessageTemplate());
    }

    public <Message extends RexProMessage> Message deserialize(byte[] bytes, Class<Message> messageClass) throws IOException {
        Unpacker un = msgpack.createBufferUnpacker(bytes);
        return msgpack.lookup(messageClass).read(un, null);
    }

    public <Message extends RexProMessage> byte[] serialize(Message message, Class<Message> messageClass) throws IOException {
        BufferPacker pk = msgpack.createBufferPacker();
        msgpack.lookup(messageClass).write(pk, message);
        return pk.toByteArray();
    }

    @Override
    public byte getSerializerId() {
        return SERIALIZER_ID;
    }
}
