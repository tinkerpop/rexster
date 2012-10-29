package com.tinkerpop.rexster.gremlin.converter;

import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.util.io.graphson.GraphSONMode;
import com.tinkerpop.blueprints.util.io.graphson.GraphSONUtility;
import com.tinkerpop.pipes.util.structures.Row;
import com.tinkerpop.pipes.util.structures.Table;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Converts a result from Gremlin to a JSONArray using GraphSON format.
 */
public class JSONResultConverter implements ResultConverter<JSONArray> {

    private GraphSONMode mode = GraphSONMode.NORMAL;
    private long offsetStart = 0L;
    private long offsetEnd = Long.MAX_VALUE;
    private final Set<String> returnKeys;


    public JSONResultConverter(final GraphSONMode mode, final long offsetStart, final long offsetEnd,
                               final Set<String> returnKeys) {
        this.mode = mode;
        this.offsetEnd = offsetEnd;
        this.offsetStart = offsetStart;
        this.returnKeys = returnKeys;
    }

    public JSONArray convert(Object result) throws Exception {
        JSONArray results = new JSONArray();
        if (result == null) {
            // for example a script like g.clear()
            results = null;

        } else if (result instanceof Table) {
            Table table = (Table) result;
            Iterator<Row> rows = table.iterator();

            List<String> columnNames = table.getColumnNames();
            long counter = 0;

            while (rows.hasNext()) {
                Row row = rows.next();
                if (counter >= this.offsetStart && counter < this.offsetEnd) {
                    Map<String, Object> map = new HashMap<String, Object>();
                    for (String columnName : columnNames) {
                        map.put(columnName, prepareOutput(row.getColumn(columnName)));
                    }

                    results.put(new JSONObject(map));
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
            Iterator itty = (Iterator) result;

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

    private Object prepareOutput(Object object) throws Exception {
        if (object == null) {
            return null;
        }
        if (object instanceof Element) {
            return GraphSONUtility.jsonFromElement((Element) object, returnKeys, this.mode);
        } else if (object instanceof Map) {
            JSONObject jsonObject = new JSONObject();
            Map map = (Map) object;
            for (Object key : map.keySet()) {
                // force an error here by passing in a null key to the JSONObject.  That way a good error message
                // gets back to the user.
                jsonObject.put(key == null ? null : key.toString(), this.prepareOutput(map.get(key)));
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
