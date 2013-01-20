package com.tinkerpop.rexster.protocol;

import org.msgpack.MessagePack;
import org.msgpack.packer.Packer;
import org.msgpack.template.Template;
import org.msgpack.type.Value;
import org.msgpack.unpacker.Unpacker;

import javax.script.Bindings;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.msgpack.template.Templates.TValue;
import static org.msgpack.template.Templates.tMap;
import static org.msgpack.template.Templates.TString;

/**
 * Helper class for for common byte operations.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class BitWorks {
    private static final MessagePack msgpack = new MessagePack();

    /**
     * Converts a UUID to bytes.
     */
    public static byte[] convertUUIDToByteArray(final UUID uuid) {
        final ByteBuffer bb = ByteBuffer.allocate(16);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return bb.array();
    }

    /**
     * Converts a byte array to a UUID.
     */
    public static UUID convertByteArrayToUUID(final byte[] bytes) {
        final ByteBuffer bb = ByteBuffer.wrap(bytes);
        final long mostSignificantBits = bb.getLong();
        final long leastSignificantBits = bb.getLong();

        return new UUID(mostSignificantBits, leastSignificantBits);
    }

    /**
     * Convert a series of strings to a byte array where each string is prefixed with 4 bytes that
     * represent the length of the string.
     */
    public static byte[] convertStringsToByteArray(final String... values) throws IOException {
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();

        try {
            for (String value : values) {
                final byte[] valueAsBytes = value.getBytes(Charset.forName("UTF-8"));
                stream.write(ByteBuffer.allocate(4).putInt(valueAsBytes.length).array());
                stream.write(valueAsBytes);
            }

            return stream.toByteArray();
        } finally {
            stream.close();
        }
    }

    public static byte[] convertBindingsToByteArray(final Bindings bindings) throws IOException {
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();

        try {
            final Map<String, Object> mapOfBindings = new HashMap<String, Object>();
            final Packer packer = msgpack.createPacker(stream);

            for (String key : bindings.keySet()) {
                final Object objectToSerialize = bindings.get(key);
                if (objectToSerialize instanceof String
                        || objectToSerialize instanceof Integer
                        || objectToSerialize instanceof Double
                        || objectToSerialize instanceof Float
                        || objectToSerialize instanceof Boolean
                        || objectToSerialize instanceof Long) {
                    mapOfBindings.put(key, objectToSerialize);
                }
            }

            packer.write(mapOfBindings);

            return stream.toByteArray();
        } finally {
            stream.close();
        }
    }

    public static RexsterBindings convertBytesToBindings(final byte[] bytes) throws IOException {

        final ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        final MessagePack msgpack = new MessagePack();
        final Unpacker unpacker = msgpack.createUnpacker(in);

        final Template<Map<String, Value>> mapTmpl = tMap(TString, TValue);

        final Map<String, Value> dstMap = unpacker.read(mapTmpl);
        final RexsterBindings bindings = new RexsterBindings();

        for (Map.Entry<String,Value> entry : dstMap.entrySet()) {
            final Value v = entry.getValue();
            Object o;
            if (v.isBooleanValue()) {
                o = v.asBooleanValue().getBoolean();
            } else if (v.isFloatValue()) {
                o = v.asFloatValue().getDouble();
            } else if (v.isIntegerValue()) {
                o = v.asIntegerValue().getInt();
            } else {
                // includes raw value
                o = v.asRawValue().getString();
            }

            bindings.put(entry.getKey(), o);
        }

        return bindings;
    }

    static byte[] getBytesWithLength(final Object result) throws IOException {
        if (result == null) {
            return null;
        }

        final ByteArrayOutputStream byteOuputStream = new ByteArrayOutputStream();
        final ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteOuputStream);

        try {
            objectOutputStream.writeObject(result);

            final ByteBuffer bb = ByteBuffer.allocate(4 + byteOuputStream.size());
            bb.putInt(byteOuputStream.size());
            bb.put(byteOuputStream.toByteArray());
            return bb.array();
        } finally {
            objectOutputStream.close();
        }
    }
}
