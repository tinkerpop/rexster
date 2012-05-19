package com.tinkerpop.rexster.gremlin.converter;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Element;
import com.tinkerpop.pipes.util.structures.Row;
import com.tinkerpop.pipes.util.structures.Table;
import com.tinkerpop.rexster.Tokens;
import org.msgpack.MessagePack;
import org.msgpack.packer.Packer;
import org.msgpack.type.NilValue;
import org.msgpack.type.ValueFactory;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class MsgPackResultConverter implements ResultConverter<byte[]> {

    public byte[] convert(final Object result) throws Exception {
        MessagePack msgpack = new MessagePack();
        List<byte[]> results = new ArrayList<byte[]>();
        if (result == null) {
            // for example a script like g.clear()
            results = null;

        } else if (result instanceof Table) {
            Table table = (Table) result;
            Iterator<Row> rows = table.iterator();

            List<String> columnNames = table.getColumnNames();
            while (rows.hasNext()) {
                Row row = rows.next();
                Map<String, Object> map = new HashMap<String, Object>();
                for (String columnName : columnNames) {
                    map.put(columnName, prepareOutput(row.getColumn(columnName)));
                }

                results.add(msgpack.write(map));
            }
        } else if (result instanceof Iterable) {
            for (Object o : (Iterable) result) {
                results.add(msgpack.write(prepareOutput(o)));
            }
        } else if (result instanceof Iterator) {
            Iterator itty = (Iterator) result;

            while (itty.hasNext()) {
                Object current = itty.next();
                results.add(msgpack.write(prepareOutput(current)));
            }
        } else {
            results.add(msgpack.write(prepareOutput(result)));
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Packer packer = msgpack.createPacker(out);

        for (byte[] someResult : results) {
            packer.write(someResult, 0, someResult.length);
        }

        return out.toByteArray();
    }

    private Object prepareOutput(Object object) throws Exception {
        if (object == null) {
            return null;
        }
        if (object instanceof Element) {
            Map<String,Object> elementMap = new HashMap<String, Object>();
            Element element = (Element) object;
            elementMap.put("id", element.getId());
            
            if (element instanceof Edge) {
                Edge edge = (Edge) element;
                elementMap.put("type", Tokens.EDGE);
                elementMap.put("in", edge.getVertex(Direction.IN).getId());
                elementMap.put("out", edge.getVertex(Direction.OUT).getId());
                elementMap.put("label", edge.getLabel());
            } else {
                elementMap.put("type", Tokens.VERTEX);
            }

            Map<String, Object> elementProperties = new HashMap<String, Object>(); 
            Iterator<String> itty = element.getPropertyKeys().iterator();
            while (itty.hasNext()) {
                String propertyKey = itty.next();
                elementProperties.put(propertyKey, this.prepareOutput(element.getProperty(propertyKey)));
            }

            elementMap.put("properties", elementProperties);
                    
            return elementMap;
        } else if (object instanceof Map) {
            Map<String, Object> msgPackMap = new HashMap<String, Object>();
            Map map = (Map) object;
            for (Object key : map.keySet()) {
                // TODO: rethink this key conversion
                msgPackMap.put(key == null ? null : key.toString(), this.prepareOutput(map.get(key)));
            }

            return msgPackMap;
        } else if (object instanceof Table || object instanceof Iterable || object instanceof Iterator) {
            return this.convert(object);
        } else if (object instanceof Number || object instanceof Boolean) {
            return object;
        } else if (object instanceof NilValue) {
            return ValueFactory.createNilValue();
        } else {
            return object.toString();
        }
    }
}
