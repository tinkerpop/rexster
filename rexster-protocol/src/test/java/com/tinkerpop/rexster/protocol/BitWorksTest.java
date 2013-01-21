package com.tinkerpop.rexster.protocol;

import org.junit.Assert;
import org.junit.Test;
import org.msgpack.MessagePack;
import org.msgpack.packer.Packer;

import javax.script.Bindings;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
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

    @Test
    public void convertByteArrayToRexsterBindings() throws Exception {
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        final Map<String, Object> mapOfBindings = new HashMap<String, Object>();
        final MessagePack msgpack = new MessagePack();
        final Packer packer = msgpack.createPacker(stream);

        mapOfBindings.put("s", "xxx");
        mapOfBindings.put("i", 1);
        mapOfBindings.put("d", 100.987d);
        mapOfBindings.put("f", 200.50f);
        mapOfBindings.put("b", true);

        packer.write(mapOfBindings);
        byte[] b = stream.toByteArray();

        final Bindings bindings = BitWorks.convertBytesToBindings(b);

        Assert.assertNotNull(bindings);
        Assert.assertEquals("xxx", bindings.get("s"));
        Assert.assertEquals(1, bindings.get("i"));
        Assert.assertEquals(100.987d, bindings.get("d"));

        // this converts to double <shrug>
        Assert.assertEquals(200.50d, bindings.get("f"));
        Assert.assertEquals(true, bindings.get("b"));
    }

}
