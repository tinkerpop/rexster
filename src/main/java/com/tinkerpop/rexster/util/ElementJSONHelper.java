package com.tinkerpop.rexster.util;

import com.tinkerpop.blueprints.pgm.Element;
import com.tinkerpop.rexster.ElementJSONObject;
import org.codehaus.jettison.json.JSONException;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class ElementJSONHelper {

    public static Iterator<ElementJSONObject> convertIterator(final Iterator<Element> iterator, final List<String> propertyKeys, final boolean showTypes) {
        return new Iterator<ElementJSONObject>() {
            public void remove() {
                throw new UnsupportedOperationException();
            }

            public boolean hasNext() {
                return iterator.hasNext();
            }

            public ElementJSONObject next() {
                try {
                    return new ElementJSONObject(iterator.next(), propertyKeys, showTypes);
                } catch (JSONException e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
            }
        };
    }

    public static Map<ElementJSONObject, Object> convertMap(final Map<Element, Object> map, final List<String> propertyKeys, final boolean showTypes) throws JSONException {
        final Map<ElementJSONObject, Object> retMap = new HashMap<ElementJSONObject, Object>();
        for (final Map.Entry<Element, Object> entry : map.entrySet()) {
            retMap.put(new ElementJSONObject(entry.getKey(), propertyKeys, showTypes), entry.getValue());
        }
        return retMap;
    }
}
