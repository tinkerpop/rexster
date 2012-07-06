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
            byte[] valueAsBytes = value.getBytes(Charset.forName("UTF-8"));
            stream.write(ByteBuffer.allocate(4).putInt(valueAsBytes.length).array());
            stream.write(valueAsBytes);
        }

        return stream.toByteArray();
    }

    public static byte[] convertSerializableBindingsToByteArray(Bindings bindings) throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        for (String key : bindings.keySet()) {
            // don't serialize the rexster key...doesn't make sense to send that back to the client
            // as it is a server side resource
            if (!key.equals(Tokens.REXPRO_REXSTER_CONTEXT)) {
                Object objectToSerialize = bindings.get(key);
                if (objectToSerialize instanceof Serializable
                        && !(objectToSerialize instanceof Graph)
                        && !(objectToSerialize instanceof Edge)
                        && !(objectToSerialize instanceof Vertex)
                        && !(objectToSerialize instanceof Index)) {
                    stream.write(ByteBuffer.allocate(4).putInt(key.length()).array());
                    stream.write(key.getBytes());

                    byte[] objectBytes = getFilteredBytesWithLength(objectToSerialize);
                    stream.write(objectBytes);
                }
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

    public static byte[] getFilteredBytesWithLength(Object result) throws IOException {

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

    public static byte[] getBytesWithLength(Object result) throws IOException {
        if (result == null) {
            return null;
        }

        ByteArrayOutputStream byteOuputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteOuputStream);
        objectOutputStream.writeObject(result);
        objectOutputStream.close();

        ByteBuffer bb = ByteBuffer.allocate(4 + byteOuputStream.size());
        bb.putInt(byteOuputStream.size());
        bb.put(byteOuputStream.toByteArray());

        return bb.array();
    }
}
