package com.tinkerpop.rexster.protocol.serializer.json.templates;

import com.tinkerpop.rexster.protocol.msg.SessionResponseMessage;
import com.tinkerpop.rexster.protocol.serializer.json.JsonConverter;
import org.codehaus.jackson.node.ArrayNode;

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
        msg.Languages = (String[]) JsonConverter.fromJsonNode(array);
        return msg;
    }
}
