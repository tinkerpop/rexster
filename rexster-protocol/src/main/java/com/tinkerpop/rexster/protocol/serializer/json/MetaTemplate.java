package com.tinkerpop.rexster.protocol.serializer.json;

import com.google.gson.*;
import com.tinkerpop.rexster.protocol.msg.RexProMessageMeta;

import java.lang.reflect.Type;
import java.util.Map;

public class MetaTemplate implements JsonSerializer<RexProMessageMeta>, JsonDeserializer<RexProMessageMeta> {

    @Override
    public RexProMessageMeta deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        RexProMessageMeta meta = new RexProMessageMeta();
        if (!(json instanceof JsonObject)) {
            return meta;
        }
        JsonObject obj = (JsonObject) json;
        for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
            JsonPrimitive p = (JsonPrimitive) entry.getValue();
            if (p == null) {
                meta.put(entry.getKey(), null);
            } else if (p.isBoolean()) {
                meta.put(entry.getKey(), p.getAsBoolean());
            } else if (p.isNumber()) {
                Number n = p.getAsNumber();
                if (n instanceof Float || n instanceof Double) {
                    meta.put(entry.getKey(), p.getAsDouble());
                } else {
                    meta.put(entry.getKey(), p.getAsLong());
                }
            } else if (p.isString()) {
                meta.put(entry.getKey(), p.getAsString());
            }
        }
        return meta;
    }

    @Override
    public JsonElement serialize(RexProMessageMeta src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject map = new JsonObject();
        for (Map.Entry<String, Object> entry: src.entrySet()) {
            if (entry.getValue() instanceof String) {
                map.add(entry.getKey(), new JsonPrimitive((String)entry.getValue()));
            } else if (entry.getValue() instanceof Number) {
                map.add(entry.getKey(), new JsonPrimitive((Number)entry.getValue()));
            } else if (entry.getValue() instanceof Character) {
                map.add(entry.getKey(), new JsonPrimitive((Character)entry.getValue()));
            } else if (entry.getValue() instanceof Boolean) {
                map.add(entry.getKey(), new JsonPrimitive((Boolean)entry.getValue()));
            }
        }
        return map;
    }

    public static MetaTemplate instance = new MetaTemplate();
    static public MetaTemplate getInstance() {
        return instance;
    }
}
