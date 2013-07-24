package com.tinkerpop.rexster.protocol.serializer.json.templates.messages;

import com.tinkerpop.rexster.protocol.msg.SessionResponseMessage;
import com.tinkerpop.rexster.protocol.serializer.json.templates.JsonConverter;
import org.codehaus.jackson.node.ArrayNode;

import java.util.ArrayList;

public class SessionResponseMessageTemplate extends RexProMessageTemplate<SessionResponseMessage> {

    @Override
    protected SessionResponseMessage instantiateMessage() {
        return new SessionResponseMessage();
    }

    @Override
    protected void writeMessageArray(ArrayNode array, SessionResponseMessage message) {
        super.writeMessageArray(array, message);
        array.add(JsonConverter.toJsonNode(message.Languages));
    }

    @Override
    protected SessionResponseMessage readMessageArray(ArrayNode array, SessionResponseMessage msg) {
        super.readMessageArray(array, msg);
        ArrayList<String> languages = (ArrayList<String>) JsonConverter.fromJsonNode(array.get(3));
        msg.Languages = languages.toArray(new String[languages.size()]);
        return msg;
    }

    public static SessionResponseMessageTemplate instance = new SessionResponseMessageTemplate();
    static public SessionResponseMessageTemplate getInstance() {
        return instance;
    }
}
