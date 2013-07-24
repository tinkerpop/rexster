package com.tinkerpop.rexster.protocol.serializer.json.templates.messages;

import com.tinkerpop.rexster.gremlin.converter.SerializedResultConverter;
import com.tinkerpop.rexster.protocol.msg.ScriptResponseMessage;
import com.tinkerpop.rexster.protocol.serializer.json.templates.BindingsTemplate;
import com.tinkerpop.rexster.protocol.serializer.json.templates.JsonConverter;
import org.codehaus.jackson.node.ArrayNode;

public class ScriptResponseMessageTemplate extends RexProMessageTemplate<ScriptResponseMessage> {

    @Override
    protected ScriptResponseMessage instantiateMessage() {
        return new ScriptResponseMessage();
    }

    @Override
    protected void writeMessageArray(ArrayNode array, ScriptResponseMessage message) {
        super.writeMessageArray(array, message);
        array.add(JsonConverter.toJsonNode(SerializedResultConverter.convert(message.Results.get())));
        array.add(BindingsTemplate.getInstance().serialize(message.Bindings));
    }

    @Override
    protected ScriptResponseMessage readMessageArray(ArrayNode array, ScriptResponseMessage msg) {
        super.readMessageArray(array, msg);
        msg.Results.set(JsonConverter.fromJsonNode(array.get(3)));
        msg.Bindings = BindingsTemplate.getInstance().deserialize(array.get(4));
        return msg;
    }

    public static ScriptResponseMessageTemplate instance = new ScriptResponseMessageTemplate();
    static public ScriptResponseMessageTemplate getInstance() {
        return instance;
    }
}
