package com.tinkerpop.rexster.util;

import com.tinkerpop.blueprints.pgm.Element;
import com.tinkerpop.rexster.ElementJSONObject;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

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

    /**
     * Flattens a Map into an array of entries where the element JSON object has its map value as an key/value in the JSON object.
     *
     * @param map          the map to flatten
     * @param valueKey     tbe key for the elements value
     * @param propertyKeys the property keys to return for the element
     * @param showTypes    whether to show the data types of the JSON object entries
     * @return a JSON array of the the flattened map
     * @throws JSONException if an exception happens during JSON generation
     */
    public static JSONArray convertMap(final Map<Element, Object> map, final String valueKey, final List<String> propertyKeys, final boolean showTypes) throws JSONException {
        final JSONArray retArray = new JSONArray();
        for (final Map.Entry<Element, Object> entry : map.entrySet()) {
            final JSONObject object = new ElementJSONObject(entry.getKey(), propertyKeys, showTypes);
            object.put(valueKey, entry.getValue());
            retArray.put(object);
        }
        return retArray;
    }
}
