package com.tinkerpop.rexster.protocol.serializer.msgpack.templates;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Element;
import com.tinkerpop.pipes.util.structures.Row;
import com.tinkerpop.pipes.util.structures.Table;
import com.tinkerpop.rexster.Tokens;
import org.msgpack.packer.Packer;
import org.msgpack.type.ArrayValue;
import org.msgpack.type.MapValue;
import org.msgpack.type.NilValue;
import org.msgpack.type.Value;
import org.msgpack.type.ValueFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Universally useful msgpack object serialization and deserialization functions
 *
 * @author Blake Eggleston (bdeggleston.github.com)
 */
public class ResultsConverter {

    static Object serializeElementId(final Element element) {
        final Object id = element.getId();
        if (id.getClass().isPrimitive()) {
            return id;
        } else {
            return id.toString();
        }
    }

    static public void serializeObject(final Object object, final Packer packer) throws Exception {
        if (object == null) {
            packer.write(ValueFactory.createNilValue());
        } else if (object instanceof String || object instanceof Number || object instanceof Boolean) {
            packer.write(object);
        } else if (object instanceof Element) {
            try {
                final Element element = (Element) object;
                final Set<String> propertyKeys = element.getPropertyKeys();
                final int propertySize = propertyKeys.size();
                final boolean isVertex = !(element instanceof Edge);
                final int elementSize = (isVertex ? 2 : 5) + ((propertySize > 0) ? 1 : 0);

                packer.writeMapBegin(elementSize);
                packer.write(Tokens._ID);
                packer.write(serializeElementId(element));

                if (isVertex) {
                    packer.write(Tokens._TYPE);
                    packer.write(Tokens.VERTEX);
                } else {
                    final Edge edge = (Edge) element;
                    packer.write(Tokens._TYPE);
                    packer.write(Tokens.EDGE);
                    packer.write(Tokens._IN_V);
                    packer.write(serializeElementId(edge.getVertex(Direction.IN)));
                    packer.write(Tokens._OUT_V);
                    packer.write(serializeElementId(edge.getVertex(Direction.OUT)));
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
            } catch (Exception e) {
                // if a transaction gets closed and the element goes out of scope it may not serialize.  in these
                // cases the vertex will just be written as null.  this can happen during binding serialization.
                // specific case is doing this:
                // v = g.addVertex()
                // g.rollback()
                // in some graphs v will go out of scope, yet it is still on the bindings as a Vertex object.
                packer.writeNil();
            }
        } else if (object instanceof Map) {
            final Map map = (Map) object;
            packer.writeMapBegin(map.size());
            for (Object key : map.keySet()) {
                if(key instanceof Element) {
                    // restructure element -> x maps
                    // this is typical in Tree and Map returns where the key is an object value instead of a
                    // primitive.  MsgPack can't process keys that way so the data needs to be restructured
                    // so that it doesn't end up simply being toString'd
                    final Element element = (Element) key;
                    writeMapKey(element.getId(), packer);
                    final HashMap<String, Object> m = new HashMap<String, Object>();
                    m.put(Tokens._KEY, element);
                    m.put(Tokens._VALUE, map.get(key));
                    serializeObject(m, packer);
                } else {
                    writeMapKey(key, packer);
                    serializeObject(map.get(key), packer);
                }
            }
            packer.writeMapEnd();
        } else if (object instanceof Table) {
            final Table table = (Table) object;
            final Iterator<Row> rows = table.iterator();

            packer.writeArrayBegin(table.size());
            while (rows.hasNext()) {
                serializeObject(rows.next(), packer);
            }
            packer.writeArrayEnd();

        } else if (object instanceof Row) {
            final Row row = (Row) object;
            final List<String> columnNames = row.getColumnNames();

            packer.writeMapBegin(columnNames.size());
            for (String columnName : columnNames) {
                packer.write(columnName);
                serializeObject(row.getColumn(columnName), packer);
            }
            packer.writeMapEnd(false);

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

    private static void writeMapKey(final Object key, final Packer packer) throws IOException {
        // map keys must be something serializable by MsgPack.  don't get too fancy
        if (key instanceof String || key instanceof Number) {
            packer.write(key);
        } else {
            packer.write(key.toString());
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
            o = v.asIntegerValue().getLong();
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
        } else if (v.isNilValue()) {
            o = null;
        } else {
            // includes raw value
            o = v.asRawValue().getString();
        }
        return o;
    }
}
