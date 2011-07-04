package com.tinkerpop.rexster.protocol;

import javax.script.SimpleBindings;
import java.io.Serializable;
import java.util.Map;

public class RexsterBindings extends SimpleBindings {

    public Object put(String key, Object value) {
        if (!(value instanceof Serializable)) {
            throw new IllegalArgumentException("RexsterBindings can only accept values that are Serializable.");
        }

        return super.put(key, value);
    }

    public void putAll(Map<? extends String, ? extends Object> toMerge) {
        for (Object value : toMerge.values()) {
            if (!(value instanceof Serializable)) {
                throw new IllegalArgumentException("RexsterBindings can only accept values that are Serializable. At least one value in the map is not Serializable.");
            }
        }

        super.putAll(toMerge);
    }
}
