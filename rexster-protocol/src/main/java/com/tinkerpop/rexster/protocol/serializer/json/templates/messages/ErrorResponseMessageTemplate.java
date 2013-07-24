package com.tinkerpop.rexster.protocol.serializer.json.templates.messages;

import com.tinkerpop.rexster.protocol.msg.ErrorResponseMessage;
import org.codehaus.jackson.node.ArrayNode;

public class ErrorResponseMessageTemplate extends RexProMessageTemplate<ErrorResponseMessage> {

    @Override
    protected ErrorResponseMessage instantiateMessage() {
        return new ErrorResponseMessage();
    }

    @Override
    protected void writeMessageArray(ArrayNode array, ErrorResponseMessage message) {
        super.writeMessageArray(array, message);
        array.add(message.ErrorMessage);
    }

    @Override
    protected ErrorResponseMessage readMessageArray(ArrayNode array, ErrorResponseMessage msg) {
        super.readMessageArray(array, msg);
        msg.ErrorMessage = array.get(3).asText();
        return msg;
    }

    public static ErrorResponseMessageTemplate instance = new ErrorResponseMessageTemplate();
    static public ErrorResponseMessageTemplate getInstance() {
        return instance;
    }
}
