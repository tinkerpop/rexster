package com.tinkerpop.rexster.protocol.serializer.json;

import org.codehaus.jackson.JsonNode;

/**
 * @author Blake Eggleston (bdeggleston.github.com)
 */
public interface JsonTemplate<T> {
    public T deserialize(JsonNode json);
    public JsonNode serialize(T src);
}
