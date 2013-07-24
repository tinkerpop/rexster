package com.tinkerpop.rexster.protocol.serializer.json.templates.messages;

import com.tinkerpop.rexster.protocol.msg.RexProMessage;
import com.tinkerpop.rexster.protocol.serializer.json.templates.JsonConverter;
import com.tinkerpop.rexster.protocol.serializer.json.templates.JsonTemplate;
import com.tinkerpop.rexster.protocol.serializer.json.templates.MetaTemplate;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;

import java.util.UUID;

public abstract class RexProMessageTemplate<Message extends RexProMessage> implements JsonTemplate<Message> {

    protected abstract Message instantiateMessage();

    protected void writeMessageArray(ArrayNode array, Message message) {
        array.add(JsonConverter.toJsonNode(message.Session==null?null:message.sessionAsUUID().toString()));
        array.add(JsonConverter.toJsonNode(message.Request==null?null:message.requestAsUUID().toString()));
        array.add(MetaTemplate.getInstance().serialize(message.Meta));
    }

    protected Message readMessageArray(final ArrayNode array, final Message msg) {
        if (!array.get(0).isNull()) {
            msg.setSessionAsUUID(UUID.fromString(array.get(0).getTextValue()));
        }
        if (!array.get(1).isNull()) {
            msg.setRequestAsUUID(UUID.fromString(array.get(1).getTextValue()));
        }
        msg.Meta = MetaTemplate.getInstance().deserialize(array.get(2));
        return msg;
    }

    public JsonNode serialize(Message message) {
        ArrayNode msgArray = new ArrayNode(JsonNodeFactory.instance);
        writeMessageArray(msgArray, message);
        return msgArray;
    }

    public Message deserialize(JsonNode json) {
        ArrayNode msgArray = (ArrayNode) json;
        return readMessageArray(msgArray, instantiateMessage());
    }
}
