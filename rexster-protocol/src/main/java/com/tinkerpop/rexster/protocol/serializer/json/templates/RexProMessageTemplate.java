package com.tinkerpop.rexster.protocol.serializer.json.templates;

import com.tinkerpop.rexster.protocol.msg.RexProMessage;
import com.tinkerpop.rexster.protocol.serializer.json.MetaTemplate;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.TextNode;

import java.util.UUID;

public abstract class RexProMessageTemplate<Message extends RexProMessage> {

    protected abstract Message instantiateMessage();

    protected void writeMessageArray(ArrayNode array, Message message) {
        array.add(new TextNode(message.sessionAsUUID().toString()));
        array.add(new TextNode(message.requestAsUUID().toString()));
        array.add(MetaTemplate.getInstance().serialize(message.Meta));
    }

    protected Message readMessageArray(final ArrayNode array, final Message msg) {
        msg.setSessionAsUUID(UUID.fromString(array.get(0).getTextValue()));
        msg.setRequestAsUUID(UUID.fromString(array.get(1).getTextValue()));
        msg.Meta = MetaTemplate.getInstance().deserialize(array.get(2));
        return msg;
    }

    public JsonNode serialize(Message message) {
        ArrayNode msgArray = new ArrayNode(JsonNodeFactory.instance);
        writeMessageArray(msgArray, message);
        return msgArray;
    }

    public Message deserialize(ArrayNode json) {
        ArrayNode msgArray = json;
        return readMessageArray(msgArray, instantiateMessage());
    }
}
