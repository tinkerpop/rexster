package com.tinkerpop.rexster.gremlin.converter;

import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.util.io.graphson.GraphSONMode;
import com.tinkerpop.blueprints.util.io.graphson.GraphSONUtility;
import com.tinkerpop.pipes.util.structures.Row;
import com.tinkerpop.pipes.util.structures.Table;
import com.tinkerpop.rexster.Tokens;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Converts a result from Gremlin to a JSONArray using GraphSON format.
 *
 * @author Blake Eggleston (bdeggleston.github.com)
 */
public class JSONResultConverter implements ResultConverter<JSONArray> {

    private final GraphSONMode mode;
    private final long offsetStart;
    private final long offsetEnd;
    private final Set<String> returnKeys;


    public JSONResultConverter(final GraphSONMode mode, final long offsetStart, final long offsetEnd,
                               final Set<String> returnKeys) {
        this.mode = mode;
        this.offsetEnd = offsetEnd;
        this.offsetStart = offsetStart;
        this.returnKeys = returnKeys;
    }

    public JSONArray convert(final Object result) throws Exception {
        JSONArray results = new JSONArray();
        if (result == null) {
            // for example a script like g.clear()
            results = null;

        } else if (result instanceof Table) {
            final Table table = (Table) result;
            final Iterator<Row> rows = table.iterator();

            long counter = 0;

            while (rows.hasNext()) {
                final Row row = rows.next();
                if (counter >= this.offsetStart && counter < this.offsetEnd) {
                    results.put(prepareOutput(row));
                }

                if (counter >= this.offsetEnd) {
                    break;
                }

                counter++;
            }
        } else if (result instanceof Iterable) {
            long counter = 0;
            for (Object o : (Iterable) result) {
                if (counter >= this.offsetStart && counter < this.offsetEnd) {
                    results.put(prepareOutput(o));
                }

                if (counter >= this.offsetEnd) {
                    break;
                }

                counter++;
            }
        } else if (result instanceof Iterator) {
            final Iterator itty = (Iterator) result;

            long counter = 0;
            while (itty.hasNext()) {
                Object current = itty.next();
                if (counter >= this.offsetStart && counter < this.offsetEnd) {
                    results.put(prepareOutput(current));
                }

                if (counter >= this.offsetEnd) {
                    break;
                }

                counter++;
            }
        } else {
            results.put(prepareOutput(result));
        }

        return results;
    }

    private Object prepareOutput(final Object object) throws Exception {
        if (object == null) {
            return null;
        }
        if (object instanceof Element) {
            return GraphSONUtility.jsonFromElement((Element) object, returnKeys, this.mode);
        } else if (object instanceof Row) {
            final Row row = (Row) object;
            final List<String> columnNames = row.getColumnNames();
            final Map<String, Object> map = new HashMap<String, Object>();
            for (String columnName : columnNames) {
                map.put(columnName, prepareOutput(row.getColumn(columnName)));
            }

            return new JSONObject(map);
        } else if (object instanceof Map) {
            final JSONObject jsonObject = new JSONObject();
            final Map map = (Map) object;
            for (Object key : map.keySet()) {
                // force an error here by passing in a null key to the JSONObject.  That way a good error message
                // gets back to the user.
                if (key instanceof Element) {
                    final Element element = (Element) key;
                    final HashMap<String, Object> m = new HashMap<String, Object>();
                    m.put(Tokens._KEY, this.prepareOutput(element));
                    m.put(Tokens._VALUE, this.prepareOutput(map.get(key)));

                    jsonObject.put(element.getId().toString(), new JSONObject(m));
                } else {
                    jsonObject.put(key == null ? null : key.toString(), this.prepareOutput(map.get(key)));
                }
            }

            return jsonObject;
        } else if (object instanceof Table || object instanceof Iterable || object instanceof Iterator) {
            return this.convert(object);
        } else if (object instanceof Number || object instanceof Boolean) {
            return object;
        } else if (object == JSONObject.NULL) {
            return JSONObject.NULL;
        } else {
            return object.toString();
        }
    }
}
