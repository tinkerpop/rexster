package com.tinkerpop.rexster.protocol.serializer.json;

import com.tinkerpop.rexster.protocol.msg.RexProBindings;
import org.codehaus.jackson.JsonNode;

import java.util.Map;

public class BindingsTemplate implements JsonTemplate<RexProBindings> {
    @Override
    public RexProBindings deserialize(JsonNode json) {
        return new RexProBindings((Map<String, Object>) JsonConverter.fromJsonNode(json));
    }

    @Override
    public JsonNode serialize(RexProBindings src) {
        return JsonConverter.toJsonNode(src);
    }


    public static BindingsTemplate instance = new BindingsTemplate();
    static public BindingsTemplate getInstance() {
        return instance;
    }
}
