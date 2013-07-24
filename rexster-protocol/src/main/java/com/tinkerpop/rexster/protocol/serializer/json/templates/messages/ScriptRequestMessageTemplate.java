package com.tinkerpop.rexster.protocol.serializer.json.templates.messages;

import com.tinkerpop.rexster.protocol.msg.ScriptRequestMessage;
import com.tinkerpop.rexster.protocol.serializer.json.templates.BindingsTemplate;
import com.tinkerpop.rexster.protocol.serializer.json.templates.JsonConverter;
import org.codehaus.jackson.node.ArrayNode;

public class ScriptRequestMessageTemplate extends RexProMessageTemplate<ScriptRequestMessage> {

    @Override
    protected ScriptRequestMessage instantiateMessage() {
        return new ScriptRequestMessage();
    }

    @Override
    protected void writeMessageArray(ArrayNode array, ScriptRequestMessage message) {
        super.writeMessageArray(array, message);
        array.add(JsonConverter.toJsonNode(message.LanguageName));
        array.add(JsonConverter.toJsonNode(message.Script));
        array.add(BindingsTemplate.getInstance().serialize(message.Bindings));
    }

    @Override
    protected ScriptRequestMessage readMessageArray(ArrayNode array, ScriptRequestMessage msg) {
        super.readMessageArray(array, msg);
        msg.LanguageName = array.get(3).asText();
        msg.Script = array.get(4).asText();
        msg.Bindings = BindingsTemplate.getInstance().deserialize(array.get(5));
        return msg;
    }

    public static ScriptRequestMessageTemplate instance = new ScriptRequestMessageTemplate();
    static public ScriptRequestMessageTemplate getInstance() {
        return instance;
    }
}
