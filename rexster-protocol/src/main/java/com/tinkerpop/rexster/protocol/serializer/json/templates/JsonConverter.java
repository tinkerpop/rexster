package com.tinkerpop.rexster.protocol.serializer.json.templates;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.BooleanNode;
import org.codehaus.jackson.node.DoubleNode;
import org.codehaus.jackson.node.IntNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.LongNode;
import org.codehaus.jackson.node.NullNode;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.node.TextNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author Blake Eggleston (bdeggleston.github.com)
 */
public class JsonConverter {

    /**
     * Recursively converts objects to json nodes
     * @param obj
     * @return
     */
    public static JsonNode toJsonNode(Object obj) {
        if (obj == null) {
            return NullNode.getInstance();
        } else if (obj instanceof Map) {
            ObjectNode map = new ObjectNode(JsonNodeFactory.instance);
            for (Map.Entry entry: ((Map<Object, Object>) obj).entrySet()) {
                map.put(entry.getKey().toString(), toJsonNode(entry.getValue()));
            }
            return map;
        } else if (obj instanceof Iterable) {
            ArrayNode array = new ArrayNode(JsonNodeFactory.instance);
            for (Object o: (Iterable) obj) {
                array.add(toJsonNode(o));
            }
            return array;
        } else if (obj instanceof Object[]) {
            ArrayNode array = new ArrayNode(JsonNodeFactory.instance);
            for (Object o: (Object[]) obj) {
                array.add(toJsonNode(o));
            }
            return array;
        } else if (obj instanceof Integer) {
            return new IntNode((Integer) obj);
        } else if (obj instanceof Long) {
            return new LongNode((Long) obj);
        } else if (obj instanceof Double) {
            return new DoubleNode((Double) obj);
        } else if (obj instanceof Float) {
            return new DoubleNode((Float) obj);
        } else if (obj instanceof Boolean) {
            return BooleanNode.valueOf((Boolean) obj);
        } else {
            return new TextNode(obj.toString());
        }
    }

    /**
     * Recursively converts json nodes from json nodes to objects
     * @param node
     * @return
     */
    public static Object fromJsonNode(JsonNode node) {
        if (node.isNull()) {
            return null;
        } else if (node.isObject()) {
            Map<String, Object> map = new HashMap<String, Object>();
            ObjectNode objectNode = (ObjectNode) node;
            Iterator<String> iterator = objectNode.getFieldNames();
            while (iterator.hasNext()) {
                String key = iterator.next();
                map.put(key, fromJsonNode(objectNode.get(key)));
            }
            return map;
        } else if (node.isArray()) {
            ArrayNode arrayNode = (ArrayNode) node;
            ArrayList<Object> array = new ArrayList<Object>();
            for (int i=0; i<arrayNode.size(); i++) {
                array.add(fromJsonNode(arrayNode.get(i)));
            }
            return array;
        } else if (node.isFloatingPointNumber()) {
            return node.asDouble();
        } else if (node.isIntegralNumber()) {
            return node.asLong();
        } else if (node.isBoolean()) {
            return node.asBoolean();
        } else {
            return node.asText();
        }
    }
}
