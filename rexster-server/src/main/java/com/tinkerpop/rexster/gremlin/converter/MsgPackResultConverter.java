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
        final MessagePack msgpack = new MessagePack();

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final Packer packer = msgpack.createPacker(out);

        if (result == null) {
            packer.write(ValueFactory.createNilValue());
        } else if (result instanceof Table) {
            final Table table = (Table) result;
            final Iterator<Row> rows = table.iterator();

            final List<String> columnNames = table.getColumnNames();

            while (rows.hasNext()) {
                final Row row = rows.next();
                final Map<String, Object> map = new HashMap<String, Object>();
                for (String columnName : columnNames) {
                    map.put(columnName, prepareOutput(row.getColumn(columnName)));
                }

                packer.write(map);
            }
        } else if (result instanceof Iterable) {
            for (Object o : (Iterable) result) {
                packer.write(prepareOutput(o));
            }
        } else if (result instanceof Iterator) {
            final Iterator itty = (Iterator) result;

            while (itty.hasNext()) {
                Object current = itty.next();
                packer.write(prepareOutput(current));
            }
        } else {
            packer.write(prepareOutput(result));
        }

        return out.toByteArray();
    }

    private Object prepareOutput(Object object) throws Exception {
        if (object == null) {
            return null;
        }
        if (object instanceof Element) {
            final Map<String,Object> elementMap = new HashMap<String, Object>();
            final Element element = (Element) object;
            elementMap.put("id", element.getId());
            
            if (element instanceof Edge) {
                final Edge edge = (Edge) element;
                elementMap.put("type", Tokens.EDGE);
                elementMap.put("in", edge.getVertex(Direction.IN).getId());
                elementMap.put("out", edge.getVertex(Direction.OUT).getId());
                elementMap.put("label", edge.getLabel());
            } else {
                elementMap.put("type", Tokens.VERTEX);
            }

            final Map<String, Object> elementProperties = new HashMap<String, Object>();
            final Iterator<String> itty = element.getPropertyKeys().iterator();
            while (itty.hasNext()) {
                final String propertyKey = itty.next();
                elementProperties.put(propertyKey, this.prepareOutput(element.getProperty(propertyKey)));
            }

            elementMap.put("properties", elementProperties);
                    
            return elementMap;
        } else if (object instanceof Map) {
            final Map<String, Object> msgPackMap = new HashMap<String, Object>();
            final Map map = (Map) object;
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
