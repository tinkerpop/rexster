package com.tinkerpop.rexster.protocol;

import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Index;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.rexster.Tokens;

import javax.script.Bindings;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.UUID;

/**
 * Helper class for for common byte operations.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class BitWorks {

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

    public static byte[] convertSerializableBindingsToByteArray(final Bindings bindings) throws IOException {
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();

        try {
            for (String key : bindings.keySet()) {
                // don't serialize the rexster key...doesn't make sense to send that back to the client
                // as it is a server side resource
                if (!key.equals(Tokens.REXPRO_REXSTER_CONTEXT)) {
                    final Object objectToSerialize = bindings.get(key);
                    if (objectToSerialize instanceof Serializable
                            && !(objectToSerialize instanceof Graph)
                            && !(objectToSerialize instanceof Edge)
                            && !(objectToSerialize instanceof Vertex)
                            && !(objectToSerialize instanceof Index)) {
                        stream.write(ByteBuffer.allocate(4).putInt(key.length()).array());
                        stream.write(key.getBytes());

                        final byte[] objectBytes = getFilteredBytesWithLength(objectToSerialize);
                        stream.write(objectBytes);
                    }
                }
            }

            return stream.toByteArray();
        } finally {
            stream.close();
        }
    }

    public static RexsterBindings convertByteArrayToRexsterBindings(final byte[] bytes) throws IOException, ClassNotFoundException {
        final ByteBuffer bb = ByteBuffer.wrap(bytes);
        final RexsterBindings bindings = new RexsterBindings();

        while (bb.hasRemaining()) {
            final int lenOfKeyBinding = bb.getInt();
            final byte[] bindingKeyItem = new byte[lenOfKeyBinding];
            bb.get(bindingKeyItem);

            final String key = new String(bindingKeyItem);

            final int lenOfBinding = bb.getInt();
            final byte[] bindingItem = new byte[lenOfBinding];
            bb.get(bindingItem);

            final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bindingItem);
            final ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);

            try {
                final Object o = objectInputStream.readObject();
                bindings.put(key, o);
            } finally {
                objectInputStream.close();
            }
        }

        return bindings;
    }

    static byte[] getFilteredBytesWithLength(final Object result) throws IOException {

        if (result instanceof Serializable
                && !(result instanceof Graph)
                && !(result instanceof Edge)
                && !(result instanceof Vertex)
                && !(result instanceof Index)) {

            try {
                return getBytesWithLength(result);
            } catch (NotSerializableException nse) {
                // com.sun.script.javascript.ExternalScriptable throws this sometimes...just toString() it
                return getBytesWithLength(result.toString());
            }
        } else {
            return getBytesWithLength(result.toString());
        }
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
