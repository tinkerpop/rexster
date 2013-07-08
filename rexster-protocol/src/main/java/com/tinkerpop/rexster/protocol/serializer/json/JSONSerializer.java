package com.tinkerpop.rexster.protocol.serializer.json;

import com.tinkerpop.rexster.protocol.msg.RexProMessage;
import com.tinkerpop.rexster.protocol.serializer.RexProSerializer;

import java.io.IOException;

public class JSONSerializer implements RexProSerializer {
    public <Message extends RexProMessage> Message deserialize(byte[] bytes, Class<Message> messageClass) throws IOException {
        return null;
    }

    public <Message extends RexProMessage> byte[] serialize(Message message, Class<Message> messageClass) throws IOException {
        return null;
    }

    @Override
    public byte serializerID() {
        return 1;
    }
}
