package com.tinkerpop.rexster.protocol;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.UUID;

/**
 * Helper class for for common byte operations.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 * @author Blake Eggleston (bdeggleston.github.com)
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
