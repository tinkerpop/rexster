package com.tinkerpop.rexster.protocol;

import javax.script.Bindings;
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

    public static byte[] convertRexsterBindingsToByteArray(RexsterBindings bindings) throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        for (String key : bindings.keySet()) {
            stream.write(ByteBuffer.allocate(4).putInt(key.length()).array());
            stream.write(key.getBytes());

            Object objectToSerialize = bindings.get(key);
            if (objectToSerialize instanceof Serializable) {
                byte[] objectBytes = getBytes(objectToSerialize);
                stream.write(objectBytes);
            }
        }

        return stream.toByteArray();
    }

    public static RexsterBindings convertByteArrayToRexsterBindings(byte[] bytes) throws IOException, ClassNotFoundException {
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        RexsterBindings bindings = new RexsterBindings();

        while (bb.hasRemaining()) {
            int lenOfKeyBinding = bb.getInt();
            byte[] bindingKeyItem = new byte[lenOfKeyBinding];
            bb.get(bindingKeyItem);

            String key = new String(bindingKeyItem);

            int lenOfBinding = bb.getInt();
            byte[] bindingItem = new byte[lenOfBinding];
            bb.get(bindingItem);
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bindingItem);
            ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);

            Object o = objectInputStream.readObject();

            bindings.put(key, o);
        }

        return bindings;
    }

    public static byte[] getBytes(Object result) throws IOException {

        if (result == null) {
            return null;
        } else if (result instanceof Serializable) {
            ByteArrayOutputStream byteOuputStream = new ByteArrayOutputStream();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteOuputStream);
            objectOutputStream.writeObject(result);
            objectOutputStream.close();

            ByteBuffer bb = ByteBuffer.allocate(4 + byteOuputStream.size());
            bb.putInt(byteOuputStream.size());
            bb.put(byteOuputStream.toByteArray());

            return bb.array();
        } else {
            return result.toString().getBytes();
        }
    }
}
