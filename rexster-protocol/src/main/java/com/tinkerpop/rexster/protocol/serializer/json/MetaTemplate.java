package com.tinkerpop.rexster.protocol.serializer.json;

import com.tinkerpop.rexster.protocol.msg.RexProMessageMeta;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.*;

import java.util.Iterator;
import java.util.Map;

public class MetaTemplate {

    public RexProMessageMeta deserialize(JsonNode json) {
        RexProMessageMeta meta = new RexProMessageMeta();
        Iterator<String> itr = json.getFieldNames();
        while (itr.hasNext()) {
            String key = itr.next();
            JsonNode val = json.get(key);
            if (val == null) {
                meta.put(key, null);
            } else if (val.isBoolean()) {
                meta.put(key, val.asBoolean());
            } else if (val.isFloatingPointNumber()) {
                meta.put(key, val.asDouble());
            } else if (val.isIntegralNumber()) {
                meta.put(key, val.asLong());
            } else if (val.isTextual()) {
                meta.put(key, val.asText());
            }
        }
        return meta;
    }

    public JsonNode serialize(RexProMessageMeta src) {
        ObjectNode map = new ObjectNode(JsonNodeFactory.instance);
        for (Map.Entry<String, Object> entry: src.entrySet()) {
            if (entry.getValue() instanceof String) {
                map.put(entry.getKey(), new TextNode((String) entry.getValue()));
            } else if (entry.getValue() instanceof Integer || entry.getValue() instanceof Long) {
                map.put(entry.getKey(), new LongNode((Long) entry.getValue()));
            } else if (entry.getValue() instanceof Float || entry.getValue() instanceof Double) {
                map.put(entry.getKey(), new DoubleNode((Double) entry.getValue()));
            } else if (entry.getValue() instanceof Boolean) {
                map.put(entry.getKey(), BooleanNode.valueOf((Boolean) entry.getValue()));
            }
        }
        return map;
    }

    public static MetaTemplate instance = new MetaTemplate();
    static public MetaTemplate getInstance() {
        return instance;
    }
}
