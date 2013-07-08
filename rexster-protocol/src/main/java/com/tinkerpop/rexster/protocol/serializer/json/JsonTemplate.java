package com.tinkerpop.rexster.protocol.serializer.json;

import org.codehaus.jackson.JsonNode;

/**
 *
 * @param <T>
 */
public interface JsonTemplate<T> {
    public T deserialize(JsonNode json);
    public JsonNode serialize(T src);
}
