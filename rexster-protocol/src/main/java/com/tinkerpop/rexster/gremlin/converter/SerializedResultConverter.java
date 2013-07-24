package com.tinkerpop.rexster.gremlin.converter;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Element;
import com.tinkerpop.pipes.util.structures.Row;
import com.tinkerpop.pipes.util.structures.Table;
import com.tinkerpop.rexster.Tokens;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Converts graph results into a serializable format
 *
 * @author Blake Eggleston (bdeggleston.github.com)
 */
public class SerializedResultConverter {

    static Object serializeElementId(final Element element) {
        final Object id = element.getId();
        if (id.getClass().isPrimitive()) {
            return id;
        } else {
            return id.toString();
        }
    }

    public static Object convert(Object object) {
        if (object == null) {
            return null;
        } else if (object instanceof String || object instanceof Number || object instanceof Boolean) {
            return object;
        } else if (object instanceof Element) {
            try {
                final Element element = (Element) object;
                final Set<String> propertyKeys = element.getPropertyKeys();
                final boolean isVertex = !(element instanceof Edge);

                HashMap<Object, Object> outMap = new HashMap<Object, Object>();
                outMap.put(Tokens._ID, serializeElementId(element));

                if (isVertex) {
                    outMap.put(Tokens._TYPE, Tokens.VERTEX);
                } else {
                    final Edge edge = (Edge) element;
                    outMap.put(Tokens._TYPE, Tokens.EDGE);
                    outMap.put(Tokens._IN_V, serializeElementId(edge.getVertex(Direction.IN)));
                    outMap.put(Tokens._OUT_V, serializeElementId(edge.getVertex(Direction.OUT)));
                    outMap.put(Tokens._LABEL, edge.getLabel());
                }

                if (propertyKeys.size() > 0) {
                    HashMap<Object, Object> propertyMap = new HashMap<Object, Object>();

                    final Iterator<String> itty = propertyKeys.iterator();
                    while (itty.hasNext()) {
                        final String propertyKey = itty.next();
                        propertyMap.put(propertyKey, convert(element.getProperty(propertyKey)));
                    }
                    outMap.put(Tokens._PROPERTIES, propertyMap);
                }

                return outMap;
            } catch (Exception e) {
                // if a transaction gets closed and the element goes out of scope it may not serialize.  in these
                // cases the vertex will just be written as null.  this can happen during binding serialization.
                // specific case is doing this:
                // v = g.addVertex()
                // g.rollback()
                // in some graphs v will go out of scope, yet it is still on the bindings as a Vertex object.
                return null;
            }
        } else if (object instanceof Map) {
            final Map map = (Map) object;
            HashMap<Object, Object> outMap = new HashMap<Object, Object>();
            for (Object key : map.keySet()) {
                if(key instanceof Element) {
                    // restructure element -> x maps
                    // this is typical in Tree and Map returns where the key is an object value instead of a
                    // primitive.  MsgPack can't process keys that way so the data needs to be restructured
                    // so that it doesn't end up simply being toString'd
                    final Element element = (Element) key;
                    final HashMap<String, Object> m = new HashMap<String, Object>();
                    m.put(Tokens._KEY, element);
                    m.put(Tokens._VALUE, map.get(key));
                    outMap.put(element.getId().toString(), convert(m));
                } else {
                    outMap.put(key, convert(map.get(key)));
                }
            }
            return outMap;
        } else if (object instanceof Table) {
            final Table table = (Table) object;
            final Iterator<Row> rows = table.iterator();

            ArrayList<Object> outArray = new ArrayList<Object>();
            while (rows.hasNext()) {
                outArray.add(convert(rows.next()));
            }
            return outArray;

        } else if (object instanceof Row) {
            final Row row = (Row) object;
            final List<String> columnNames = row.getColumnNames();

            HashMap<Object, Object> outMap = new HashMap<Object, Object>();
            for (String columnName : columnNames) {
                outMap.put(columnName, convert(row.getColumn(columnName)));
            }
            return outMap;

        } else if (object instanceof Iterable) {
            final ArrayList<Object> outArray = new ArrayList<Object>();
            for (Object o : (Iterable) object) {
                outArray.add(convert(o));
            }
            return outArray;

        } else if (object instanceof Iterator) {
            //we need to know the array size before beginning serialization
            final ArrayList<Object> outArray = new ArrayList<Object>();
            Iterator itty = (Iterator) object;
            while (itty.hasNext()) {
                outArray.add(convert(itty.next()));
            }
            return outArray;

        } else {
            return object.toString();
        }
    }
}
