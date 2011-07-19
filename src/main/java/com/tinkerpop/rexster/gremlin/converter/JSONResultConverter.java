package com.tinkerpop.rexster.gremlin.converter;

import com.tinkerpop.blueprints.pgm.Element;
import com.tinkerpop.blueprints.pgm.util.json.JSONWriter;
import com.tinkerpop.gremlin.pipes.util.Table;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.io.Writer;
import java.util.*;

public class JSONResultConverter implements ResultConverter<JSONArray>{

    private boolean showTypes = false;
    private long offsetStart = 0L;
    private long offsetEnd = Long.MAX_VALUE;
    private List<String> returnKeys = new ArrayList<String>();


    public JSONResultConverter(boolean showTypes, long offsetStart, long offsetEnd, List<String> returnKeys) {
        this.showTypes = showTypes;
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
            Iterator<Table.Row> rows = table.iterator();

            List<String> columnNames = table.getColumnNames();
            long counter = 0;

            while (rows.hasNext()) {
                Table.Row row = rows.next();
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
                if (counter >= this.offsetStart && counter < this.offsetEnd) {
                    results.put(prepareOutput(itty.next()));
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
        if (object instanceof Element) {
            if (returnKeys == null) {
                return JSONWriter.createJSONElement((Element) object, null, showTypes);
            } else {
                return JSONWriter.createJSONElement((Element) object, returnKeys, showTypes);
            }
        } else if (object instanceof Map) {
            JSONObject jsonObject = new JSONObject();
            Map map = (Map) object;
            for (Object key : map.keySet()) {
                jsonObject.put(key.toString(), this.prepareOutput(map.get(key)));
            }

            return jsonObject;
        } else if (object instanceof Table || object instanceof Iterable || object instanceof Iterator) {
            return this.convert(object);
        } else if (object instanceof Number || object instanceof Boolean) {
            return object;
        } else {
            return object.toString();
        }
    }
}
