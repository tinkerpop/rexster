package com.tinkerpop.rexster.protocol;

import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.ByteBuffer;
import java.util.UUID;

public class BitWorksTest {
    @Test
    public void convertUUIDToByteArray() {
        long msb = 10l;
        long lsb = 20l;

        UUID uuid = new UUID(msb, lsb);

        byte[] bytes = BitWorks.convertUUIDToByteArray(uuid);
        ByteBuffer buffer = ByteBuffer.wrap(bytes);

        Assert.assertEquals(msb, buffer.getLong());
        Assert.assertEquals(lsb, buffer.getLong());
    }

    @Test
    public void convertByteArrayToUUID() {
        long msb = 10l;
        long lsb = 20l;

        ByteBuffer buffer = ByteBuffer.wrap(new byte[16]);
        buffer.putLong(msb);
        buffer.putLong(lsb);

        UUID uuid = BitWorks.convertByteArrayToUUID(buffer.array());

        Assert.assertEquals(msb, uuid.getMostSignificantBits());
        Assert.assertEquals(lsb, uuid.getLeastSignificantBits());
    }

    @Test
    public void convertStringsToByteArray() throws IOException {
        String x = "something";
        String y = "anything";

        byte[] bytes = BitWorks.convertStringsToByteArray(x, y);

        ByteBuffer buffer = ByteBuffer.wrap(bytes);

        Assert.assertEquals(x.length(), buffer.getInt());
        byte[] xBytes = new byte[x.length()];
        buffer.get(xBytes);
        Assert.assertEquals(x, new String(xBytes));

        Assert.assertEquals(y.length(), buffer.getInt());
        byte[] yBytes = new byte[y.length()];
        buffer.get(yBytes);
        Assert.assertEquals(y, new String(yBytes));
    }

    @Test
    public void getBytesWithLengthNull() throws IOException {
        Assert.assertNull(BitWorks.getBytesWithLength(null));
    }

    @Test
    public void getBytesWithLengthValid() throws IOException, ClassNotFoundException {
        String convertMe = "convertMe";
        byte[] bytes = BitWorks.getBytesWithLength(convertMe);

        Assert.assertNotNull(bytes);
        Assert.assertEquals(20, bytes.length);

        ByteBuffer bb = ByteBuffer.wrap(bytes);
        Assert.assertEquals(16, bb.getInt());

        byte[] stringPartInBytes = new byte[16];
        bb.get(stringPartInBytes);

        ByteArrayInputStream bais = new ByteArrayInputStream(stringPartInBytes);
        ObjectInputStream ois = new ObjectInputStream(bais);
        String stringPart = (String) ois.readObject();

        Assert.assertEquals(convertMe, new String(stringPart));
    }
}
