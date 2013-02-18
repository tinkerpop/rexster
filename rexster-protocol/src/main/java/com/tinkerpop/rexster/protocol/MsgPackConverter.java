package com.tinkerpop.rexster.protocol;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.pipes.util.structures.Row;
import com.tinkerpop.pipes.util.structures.Table;
import com.tinkerpop.rexster.Tokens;
import org.msgpack.MessagePack;
import org.msgpack.packer.BufferPacker;
import org.msgpack.packer.Packer;
import org.msgpack.type.*;

import java.util.*;

/**
 * Universally useful msgpack object serialization and deserialization functions
 */
public class MsgPackConverter {

    static public void serializeObject(final Object object, final Packer packer) throws Exception {
        if (object == null) {
            packer.write(ValueFactory.createNilValue());
        } else if (object instanceof String || object instanceof Number || object instanceof Boolean) {
            packer.write(object);
        } else if (object instanceof Element) {
            final Element element = (Element) object;
            final Set<String> propertyKeys = element.getPropertyKeys();
            final int propertySize = propertyKeys.size();
            final boolean isVertex = element instanceof Vertex;
            final int elementSize = (isVertex ? 2 : 5) + ((propertySize > 0) ? 1 : 0);

            packer.writeMapBegin(elementSize);
            packer.write(Tokens._ID);
            final Object id = element.getId();
            if (id.getClass().isPrimitive()) {
                packer.write(id);
            } else {
                packer.write(id.toString());
            }

            if (isVertex) {
                packer.write(Tokens._TYPE);
                packer.write(Tokens.VERTEX);
            } else {
                final Edge edge = (Edge) element;
                packer.write(Tokens._TYPE);
                packer.write(Tokens.EDGE);
                packer.write(Tokens._IN_V);
                packer.write(edge.getVertex(Direction.IN).getId());
                packer.write(Tokens._OUT_V);
                packer.write(edge.getVertex(Direction.OUT).getId());
                packer.write(Tokens._LABEL);
                packer.write(edge.getLabel());
            }

            if (propertyKeys.size() > 0) {
                packer.write(Tokens._PROPERTIES);
                packer.writeMapBegin(propertySize);

                final Iterator<String> itty = propertyKeys.iterator();
                while (itty.hasNext()) {
                    final String propertyKey = itty.next();
                    packer.write(propertyKey);
                    serializeObject(element.getProperty(propertyKey), packer);
                }

                packer.writeMapEnd(false);
            }
            packer.writeMapEnd(false);
        } else if (object instanceof Map) {
            final Map map = (Map) object;

            packer.writeMapBegin(map.size());
            for (Object key : map.keySet()) {
                packer.write(key);
                serializeObject(map.get(key), packer);
            }
            packer.writeMapEnd();
        } else if (object instanceof Table) {
            final Table table = (Table) object;
            final Iterator<Row> rows = table.iterator();

            final List<String> columnNames = table.getColumnNames();

            while (rows.hasNext()) {
                final Row row = rows.next();

                packer.writeMapBegin(table.size());
                for (String columnName : columnNames) {
                    packer.write(columnName);
                    serializeObject(row.getColumn(columnName), packer);
                }

                packer.writeMapEnd(false);
            }
        } else if (object instanceof Iterable) {
            Collection contents;
            if (object instanceof Collection) {
                contents = (Collection) object;
            } else {
                contents = iterateToList(((Iterable) object).iterator());
            }

            packer.writeArrayBegin(contents.size());
            for (Object o : contents) {
                serializeObject(o, packer);
            }
            packer.writeArrayEnd();

        } else if (object instanceof Iterator) {
            //we need to know the array size before beginning serialization
            final Collection contents = iterateToList((Iterator) object);

            packer.writeArrayBegin(contents.size());
            for (Object o : contents) {
                serializeObject(o, packer);
            }
            packer.writeArrayEnd();

        } else if (object instanceof NilValue) {
            packer.write((NilValue) object);
        } else {
            packer.write(object.toString());
        }

    }

    private static ArrayList<Object> iterateToList(final Iterator itty) {
        final ArrayList<Object> contents = new ArrayList<Object>();

        while (itty.hasNext()) {
            contents.add(itty.next());
        }
        return contents;
    }

    public static Object deserializeObject(final Value v) {
        Object o;

        //check for null first to avoid NullPointerException
        if (v == null) {
            o = null;
        } else if (v.isBooleanValue()) {
            o = v.asBooleanValue().getBoolean();
        } else if (v.isFloatValue()) {
            o = v.asFloatValue().getDouble();
        } else if (v.isIntegerValue()) {
            o = v.asIntegerValue().getInt();
        } else if (v.isArrayValue()) {
            final ArrayValue src = v.asArrayValue();
            final ArrayList<Object> dst = new ArrayList<Object>(src.size());
            for (int i = 0; i < src.size(); i++) {
                final Object val = deserializeObject(src.get(i));
                dst.add(i, val);
            }
            o = dst;
        } else if (v.isMapValue()) {
            final MapValue src = v.asMapValue();
            final HashMap<Object, Object> dst = new HashMap<Object, Object>(src.size());
            for (Map.Entry<Value, Value> entry : src.entrySet()) {
                final Object key = deserializeObject(entry.getKey());
                final Object val = deserializeObject(entry.getValue());
                dst.put(key, val);
            }
            o = dst;
        } else {
            // includes raw value
            o = v.asRawValue().getString();
        }
        return o;
    }
}
