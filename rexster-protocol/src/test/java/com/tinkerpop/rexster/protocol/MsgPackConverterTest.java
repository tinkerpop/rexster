package com.tinkerpop.rexster.protocol;

import com.tinkerpop.blueprints.impls.tg.TinkerGraph;
import com.tinkerpop.blueprints.impls.tg.TinkerGraphFactory;
import com.tinkerpop.pipes.util.structures.Table;
import com.tinkerpop.rexster.protocol.serializer.msgpack.templates.ResultsConverter;
import org.junit.Assert;
import org.junit.Test;
import org.msgpack.MessagePack;
import org.msgpack.packer.BufferPacker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MsgPackConverterTest {

    private MessagePack msgpack = new MessagePack();

    @Test
    public void testNull() throws Exception {
        BufferPacker packer = msgpack.createBufferPacker(1024);
        ResultsConverter.serializeObject(null, packer);
        byte[] results = packer.toByteArray();
        Assert.assertNotNull(results);


        Object obj = ResultsConverter.deserializeObject(msgpack.read(results));
        Assert.assertEquals(obj, null);
    }

    @Test
    public void testShort() throws Exception {
        BufferPacker packer = msgpack.createBufferPacker(1024);

        ResultsConverter.serializeObject(Short.MAX_VALUE, packer);
        byte[] results = packer.toByteArray();

        Assert.assertNotNull(results);
        Object obj = ResultsConverter.deserializeObject(msgpack.read(results));

        Assert.assertTrue(obj instanceof Long);
        Long value = (Long) obj;
        Assert.assertTrue(value == Short.MAX_VALUE);

    }

    @Test
    public void testInt() throws Exception {
        BufferPacker packer = msgpack.createBufferPacker(1024);

        ResultsConverter.serializeObject(Integer.MAX_VALUE, packer);
        byte[] results = packer.toByteArray();

        Assert.assertNotNull(results);

        Object obj = ResultsConverter.deserializeObject(msgpack.read(results));

        Assert.assertTrue(obj instanceof Long);
        Long value = (Long) obj;
        Assert.assertTrue(value == Integer.MAX_VALUE);

    }

    @Test
    public void testLong() throws Exception {
        BufferPacker packer = msgpack.createBufferPacker(1024);

        ResultsConverter.serializeObject(Long.MAX_VALUE, packer);
        byte[] results = packer.toByteArray();

        Assert.assertNotNull(results);

        Object obj = ResultsConverter.deserializeObject(msgpack.read(results));

        Assert.assertTrue(obj instanceof Long);
        Long value = (Long) obj;
        Assert.assertTrue(value == Long.MAX_VALUE);

    }

    @Test
    public void testFloat() throws Exception {
        BufferPacker packer = msgpack.createBufferPacker(1024);

        ResultsConverter.serializeObject(Float.MAX_VALUE, packer);
        byte[] results = packer.toByteArray();

        Assert.assertNotNull(results);

        Object obj = ResultsConverter.deserializeObject(msgpack.read(results));

        Assert.assertTrue(obj instanceof Double);
        Double value = (Double) obj;
        Assert.assertTrue(value == Float.MAX_VALUE);

    }

    @Test
    public void testDouble() throws Exception {
        BufferPacker packer = msgpack.createBufferPacker(1024);

        ResultsConverter.serializeObject(Double.MAX_VALUE, packer);
        byte[] results = packer.toByteArray();

        Assert.assertNotNull(results);

        Object obj = ResultsConverter.deserializeObject(msgpack.read(results));

        Assert.assertTrue(obj instanceof Double);
        Double value = (Double) obj;
        Assert.assertTrue(value == Double.MAX_VALUE);

    }

    @Test
    public void convertTable() throws Exception {
        Table table = new Table("col1", "col2");
        table.addRow("x1", "x2");
        table.addRow("y1", "y2");

        BufferPacker packer = msgpack.createBufferPacker(1024);
        ResultsConverter.serializeObject(table, packer);
        byte[] results = packer.toByteArray();

        Assert.assertNotNull(results);
        final Object unpackedObj = ResultsConverter.deserializeObject(msgpack.read(results));

        Assert.assertTrue(unpackedObj instanceof ArrayList);
        final ArrayList unpacked = (ArrayList) unpackedObj;

        Map<String, String> mapX = (Map<String, String>) unpacked.get(0);
        Assert.assertTrue(mapX.containsKey("col1"));
        Assert.assertTrue(mapX.containsKey("col2"));
        Assert.assertEquals("x1", mapX.get("col1"));
        Assert.assertEquals("x2", mapX.get("col2"));

        Map<String, String> mapY = (Map<String, String>) unpacked.get(1);
        Assert.assertTrue(mapY.containsKey("col1"));
        Assert.assertTrue(mapY.containsKey("col2"));
        Assert.assertEquals("y1", mapY.get("col1"));
        Assert.assertEquals("y2", mapY.get("col2"));
    }

    @Test
    public void convertElements() throws Exception {
        TinkerGraph g = TinkerGraphFactory.createTinkerGraph();

        BufferPacker packer = msgpack.createBufferPacker(1024);
        ResultsConverter.serializeObject(g.getVertices(), packer);
        byte[] results = packer.toByteArray();

        final Object unpacked = ResultsConverter.deserializeObject(msgpack.read(results));

        Assert.assertTrue(unpacked instanceof Iterable);

        final Iterator unpackerItty = ((Iterable) unpacked).iterator();

        int counter = 0;
        while (unpackerItty.hasNext()) {
            unpackerItty.next();
            counter++;
        }

        Assert.assertEquals(6, counter);
    }

    @Test
    public void convertIterable() throws Exception {

        ArrayList<String> stringList = new ArrayList<String>();
        stringList.add("abc");
        stringList.add("xyz");
        Iterable<String> iterable = stringList;

        BufferPacker packer = msgpack.createBufferPacker(1024);
        ResultsConverter.serializeObject(iterable, packer);
        byte[] results = packer.toByteArray();

        Assert.assertNotNull(results);
        final Object unpacked = ResultsConverter.deserializeObject(msgpack.read(results));

        Assert.assertTrue(unpacked instanceof Iterable);

        final Iterator unpackerItty = ((Iterable) unpacked).iterator();

        int counter = 0;
        boolean matchX = false;
        boolean matchY = false;

        while (unpackerItty.hasNext()) {
            final String v = (String) unpackerItty.next();
            if (v.equals("abc")) matchX = true;
            if (v.equals("xyz")) matchY = true;
            counter++;
        }

        Assert.assertEquals(2, counter);
        Assert.assertTrue(matchX && matchY);
    }

    @Test
    public void convertIterator() throws Exception {

        ArrayList<String> stringList = new ArrayList<String>();
        stringList.add("abc");
        stringList.add("xyz");
        Iterable<String> iterable = stringList;

        BufferPacker packer = msgpack.createBufferPacker(1024);
        ResultsConverter.serializeObject(iterable, packer);
        byte[] results = packer.toByteArray();

        Assert.assertNotNull(results);

        final Object unpacked = ResultsConverter.deserializeObject(msgpack.read(results));

        Assert.assertTrue(unpacked instanceof Iterable);

        final Iterator unpackerItty = ((Iterable) unpacked).iterator();

        int counter = 0;
        boolean matchX = false;
        boolean matchY = false;

        while (unpackerItty.hasNext()) {
            final String v = (String) unpackerItty.next();
            if (v.equals("abc")) matchX = true;
            if (v.equals("xyz")) matchY = true;
            counter++;
        }

        Assert.assertEquals(2, counter);
        Assert.assertTrue(matchX && matchY);
    }

    @Test
    public void convertIteratorNullElement() throws Exception {

        ArrayList<String> stringList = new ArrayList<String>();
        stringList.add("abc");
        stringList.add(null);
        stringList.add("xyz");
        Iterable<String> iterable = stringList;

        BufferPacker packer = msgpack.createBufferPacker(1024);
        ResultsConverter.serializeObject(iterable, packer);
        byte[] results = packer.toByteArray();

        Assert.assertNotNull(results);

        final Object unpacked = ResultsConverter.deserializeObject(msgpack.read(results));
        Assert.assertTrue(unpacked instanceof  Iterable);
        final Iterator unpackerItty = ((Iterable) unpacked).iterator();

        int counter = 0;
        boolean matchX = false;
        boolean matchY = false;
        boolean matchNil = false;

        while (unpackerItty.hasNext()) {
            final String v = (String) unpackerItty.next();
            if (v != null && v.equals("abc")) matchX = true;
            if (v != null && v.equals("xyz")) matchY = true;
            if (v == null) matchNil = true;
            counter++;
        }

        Assert.assertEquals(3, counter);
        Assert.assertTrue(matchX && matchY && matchNil);
    }

    @Test
    public void convertMap() throws Exception {
        Map<String, Object> map = new HashMap<String, Object>();
        Map<String, String> innerMap = new HashMap<String, String>();
        innerMap.put("a", "b");

        map.put("x", "x");
        map.put("y", "some");
        map.put("z", innerMap);

        BufferPacker packer = msgpack.createBufferPacker(1024);
        ResultsConverter.serializeObject(map, packer);
        byte[] results = packer.toByteArray();

        Assert.assertNotNull(results);

        final Object unpacked = ResultsConverter.deserializeObject(msgpack.read(results));

        Map<String, Object> unpackedMap = (Map) unpacked;
        Assert.assertTrue(unpackedMap.containsKey("x"));
        Assert.assertTrue(unpackedMap.containsKey("y"));
        Assert.assertTrue(unpackedMap.containsKey("z"));
        Assert.assertEquals("x", unpackedMap.get("x"));
        Assert.assertEquals("some", unpackedMap.get("y"));

        Object mapValue = unpackedMap.get("z");
        Assert.assertTrue(mapValue instanceof Map);
        Map<String, String> innerMapValue = (Map) mapValue;
        Assert.assertNotNull(innerMapValue);
        Assert.assertEquals("b", innerMapValue.get("a"));
    }
}
