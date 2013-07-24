package com.tinkerpop.rexster.protocol.serializer.json.templates.messages;

import com.tinkerpop.rexster.protocol.msg.SessionRequestMessage;
import org.codehaus.jackson.node.ArrayNode;

public class SessionRequestMessageTemplate extends RexProMessageTemplate<SessionRequestMessage> {

    @Override
    protected SessionRequestMessage instantiateMessage() {
        return new SessionRequestMessage();
    }

    @Override
    protected void writeMessageArray(ArrayNode array, SessionRequestMessage message) {
        super.writeMessageArray(array, message);
        array.add(message.Username);
        array.add(message.Password);
    }

    @Override
    protected SessionRequestMessage readMessageArray(ArrayNode array, SessionRequestMessage msg) {
        super.readMessageArray(array, msg);
        msg.Username = array.get(3).asText();
        msg.Password = array.get(4).asText();
        return msg;
    }

    public static SessionRequestMessageTemplate instance = new SessionRequestMessageTemplate();
    static public SessionRequestMessageTemplate getInstance() {
        return instance;
    }
}
