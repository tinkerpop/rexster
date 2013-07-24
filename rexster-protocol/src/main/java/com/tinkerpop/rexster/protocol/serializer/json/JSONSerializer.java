package com.tinkerpop.rexster.protocol.serializer.json;

import com.tinkerpop.rexster.protocol.msg.ErrorResponseMessage;
import com.tinkerpop.rexster.protocol.msg.RexProMessage;
import com.tinkerpop.rexster.protocol.msg.ScriptRequestMessage;
import com.tinkerpop.rexster.protocol.msg.ScriptResponseMessage;
import com.tinkerpop.rexster.protocol.msg.SessionRequestMessage;
import com.tinkerpop.rexster.protocol.msg.SessionResponseMessage;
import com.tinkerpop.rexster.protocol.serializer.RexProSerializer;
import com.tinkerpop.rexster.protocol.serializer.json.templates.messages.ErrorResponseMessageTemplate;
import com.tinkerpop.rexster.protocol.serializer.json.templates.messages.RexProMessageTemplate;
import com.tinkerpop.rexster.protocol.serializer.json.templates.messages.ScriptRequestMessageTemplate;
import com.tinkerpop.rexster.protocol.serializer.json.templates.messages.ScriptResponseMessageTemplate;
import com.tinkerpop.rexster.protocol.serializer.json.templates.messages.SessionRequestMessageTemplate;
import com.tinkerpop.rexster.protocol.serializer.json.templates.messages.SessionResponseMessageTemplate;
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
