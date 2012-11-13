package com.tinkerpop.rexster.gremlin.converter;

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
import org.msgpack.type.NilValue;
import org.msgpack.type.ValueFactory;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Converts a result from Gremlin to a byte array encoded by MsgPack.
 */
public class MsgPackResultConverter implements ResultConverter<byte[]> {
    private final MessagePack msgpack = new MessagePack();

    public byte[] convert(final Object result) throws Exception {
        final BufferPacker packer = msgpack.createBufferPacker(1024);
        try {
            prepareOutput(result, packer);
            return packer.toByteArray();
        } catch (Exception e) {
            throw e;
        } finally {
            packer.close();
        }
    }

    private void prepareOutput(final Object object, final Packer packer) throws Exception {
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
                    this.prepareOutput(element.getProperty(propertyKey), packer);
                }

                packer.writeMapEnd(false);
            }

            packer.writeMapEnd(false);
        } else if (object instanceof Map) {
            final Map map = (Map) object;

            packer.writeMapBegin(map.size());
            for (Object key : map.keySet()) {
                packer.write(key == null ? null : key.toString());
                this.prepareOutput(map.get(key), packer);
            }
        } else if (object instanceof Table) {
            final Table table = (Table) object;
            final Iterator<Row> rows = table.iterator();

            final List<String> columnNames = table.getColumnNames();

            while (rows.hasNext()) {
                final Row row = rows.next();

                packer.writeMapBegin(table.size());
                for (String columnName : columnNames) {
                    packer.write(columnName);
                    prepareOutput(row.getColumn(columnName), packer);
                }

                packer.writeMapEnd(false);
            }
        } else if (object instanceof Iterable) {
            for (Object o : (Iterable) object) {
                prepareOutput(o, packer);
            }
        } else if (object instanceof Iterator) {
            final Iterator itty = (Iterator) object;
            while (itty.hasNext()) {
                prepareOutput(itty.next(), packer);
            }
        } else if (object instanceof NilValue) {
            packer.write((NilValue) object);
        } else {
            packer.write(object.toString());
        }
    }
}
