package com.tinkerpop.rexster.protocol.serializer.json.templates;

import com.google.gson.*;
import com.tinkerpop.rexster.protocol.msg.RexProMessage;
import com.tinkerpop.rexster.protocol.serializer.json.MetaTemplate;

import java.lang.reflect.Type;
import java.util.UUID;

public abstract class RexProMessageTemplate<Message extends RexProMessage> implements JsonSerializer<Message>, JsonDeserializer<Message> {

    protected abstract Message instantiateMessage();

    protected void writeMessageArray(JsonArray array, Message message, Type typeOfSrc, JsonSerializationContext context) {
        array.add(new JsonPrimitive(message.sessionAsUUID().toString()));
        array.add(new JsonPrimitive(message.requestAsUUID().toString()));
        array.add(MetaTemplate.getInstance().serialize(message.Meta, typeOfSrc, context));
    }

    protected Message readMessageArray(final JsonArray array, final Message msg, Type typeOfT, JsonDeserializationContext context) {
        msg.setSessionAsUUID(UUID.fromString(array.getAsString()));
        msg.setRequestAsUUID(UUID.fromString(array.getAsString()));
        msg.Meta = MetaTemplate.getInstance().deserialize(array.getAsJsonObject(), typeOfT, context);
        return msg;
    }

    @Override
    public JsonElement serialize(Message message, Type typeOfSrc, JsonSerializationContext context) {
        JsonArray msgArray = new JsonArray();
        writeMessageArray(msgArray, message, typeOfSrc, context);
        return msgArray;
    }

    @Override
    public Message deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonArray msgArray = json.getAsJsonArray();
        return readMessageArray(msgArray, instantiateMessage(), typeOfT, context);
    }
}
