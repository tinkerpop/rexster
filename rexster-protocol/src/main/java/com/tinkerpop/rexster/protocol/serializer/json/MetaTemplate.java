package com.tinkerpop.rexster.protocol.serializer.json;

import com.tinkerpop.rexster.protocol.msg.RexProMessageMeta;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.*;

import java.util.Iterator;
import java.util.Map;

public class MetaTemplate implements JsonTemplate<RexProMessageMeta> {

    @Override
    public RexProMessageMeta deserialize(JsonNode json) {
        RexProMessageMeta meta = new RexProMessageMeta();
        Iterator<String> itr = json.getFieldNames();
        while (itr.hasNext()) {
            String key = itr.next();
            JsonNode val = json.get(key);
            meta.put(key, JsonConverter.fromJsonNode(val));
        }
        return meta;
    }

    @Override
    public JsonNode serialize(RexProMessageMeta src) {
        ObjectNode map = new ObjectNode(JsonNodeFactory.instance);
        for (Map.Entry<String, Object> entry: src.entrySet()) {
            map.put(entry.getKey(), JsonConverter.toJsonNode(entry.getValue()));
        }
        return map;
    }

    public static MetaTemplate instance = new MetaTemplate();
    static public MetaTemplate getInstance() {
        return instance;
    }
}
