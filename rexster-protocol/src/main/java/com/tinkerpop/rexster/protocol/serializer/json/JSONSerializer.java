package com.tinkerpop.rexster.protocol.serializer.json;

import com.tinkerpop.rexster.protocol.msg.*;
import com.tinkerpop.rexster.protocol.serializer.RexProSerializer;
import com.tinkerpop.rexster.protocol.serializer.json.templates.*;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;

/**
 * @author Blake Eggleston (bdeggleston.github.com)
 */
public class JSONSerializer implements RexProSerializer {

    public static final byte SERIALIZER_ID = 1;

    private static ObjectMapper mapper = new ObjectMapper();

    public <Message extends RexProMessage> Message deserialize(byte[] bytes, Class<Message> messageClass) throws IOException {

        RexProMessageTemplate template = null;
        if (messageClass == ErrorResponseMessage.class) {
            template = ErrorResponseMessageTemplate.getInstance();
        } else if (messageClass == ScriptRequestMessage.class) {
            template = ScriptRequestMessageTemplate.getInstance();
        } else if (messageClass == ScriptResponseMessage.class) {
            template = ScriptResponseMessageTemplate.getInstance();
        } else if (messageClass == SessionRequestMessage.class) {
            template = SessionRequestMessageTemplate.getInstance();
        } else if (messageClass == SessionResponseMessage.class) {
            template = SessionResponseMessageTemplate.getInstance();
        }

        return (Message) template.deserialize(mapper.readTree(bytes));
    }

    public <Message extends RexProMessage> byte[] serialize(Message message, Class<Message> messageClass) throws IOException {

        RexProMessageTemplate template = null;
        if (messageClass == ErrorResponseMessage.class) {
            template = ErrorResponseMessageTemplate.getInstance();
        } else if (messageClass == ScriptRequestMessage.class) {
            template = ScriptRequestMessageTemplate.getInstance();
        } else if (messageClass == ScriptResponseMessage.class) {
            template = ScriptResponseMessageTemplate.getInstance();
        } else if (messageClass == SessionRequestMessage.class) {
            template = SessionRequestMessageTemplate.getInstance();
        } else if (messageClass == SessionResponseMessage.class) {
            template = SessionResponseMessageTemplate.getInstance();
        }
        JsonNode serialized = template.serialize(message);
        return mapper.writeValueAsBytes(serialized);
    }

    @Override
    public byte getSerializerId() {
        return SERIALIZER_ID;
    }
}
