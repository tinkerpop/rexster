package com.tinkerpop.rexster.traversals;

import com.tinkerpop.blueprints.pgm.Element;
import org.json.simple.JSONObject;

import java.util.List;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class ElementJSONObject extends JSONObject {

    private final Object id;

    public ElementJSONObject(Element element) {
        this.id = element.getId();
        for (String key : element.getPropertyKeys()) {
            this.put(key, element.getProperty(key));
        }
    }

    public ElementJSONObject(Element element, List<String> propertyKeys) {
        this.id = element.getId();
        for (String key : propertyKeys) {
            Object temp = element.getProperty(key);
            if (null != temp) {
                this.put(key, temp);
            }
        }
    }

    public int hashCode() {
        return this.id.hashCode();
    }

    public Object getId() {
        return this.id;
    }

    public boolean equals(Object object) {
        if (object instanceof ElementJSONObject)
            return ((ElementJSONObject) object).getId().equals(this.id);
        else
            return false;
    }
}

