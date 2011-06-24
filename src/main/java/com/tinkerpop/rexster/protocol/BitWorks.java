package com.tinkerpop.rexster.protocol;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.UUID;

public class BitWorks {
    public static byte[] convertUUIDToByteArray(UUID uuid) {
        ByteBuffer bb = ByteBuffer.allocate(16);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return bb.array();
    }

    public static UUID convertByteArrayToUUID(byte[] bytes) {
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        long mostSignificantBits = bb.getLong();
        long leastSignificantBits = bb.getLong();

        return new UUID(mostSignificantBits, leastSignificantBits);
    }

    public static byte[] convertStringsToByteArray(String... values) throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        for (String value : values) {
            stream.write(ByteBuffer.allocate(4).putInt(value.length()).array());
            stream.write(value.getBytes());
        }

        return stream.toByteArray();
    }
}
