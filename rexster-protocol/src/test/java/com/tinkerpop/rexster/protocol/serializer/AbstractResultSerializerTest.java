package com.tinkerpop.rexster.protocol.serializer;

import com.tinkerpop.blueprints.impls.tg.TinkerGraph;
import com.tinkerpop.blueprints.impls.tg.TinkerGraphFactory;
import com.tinkerpop.pipes.util.structures.Table;
import com.tinkerpop.rexster.protocol.msg.ScriptResponseMessage;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Tests that results are properly serialized by a serializer class
 */
public abstract class AbstractResultSerializerTest {

    /**
     * @return
     */
    protected abstract RexProSerializer getSerializer();

    /**
     * puts the given value into a serialized response message, then
     * deserializes it and returns the result
     *
     * @param value the value to serialize
     * @return
     */
    private Object serializeAndDeserialize(Object value) throws Exception {
        ScriptResponseMessage msg = new ScriptResponseMessage();
        msg.Results.set(value);
        RexProSerializer serializer = getSerializer();

        byte[] bytes = serializer.serialize(msg, ScriptResponseMessage.class);
        return serializer.deserialize(bytes, ScriptResponseMessage.class);
    }


    @Test
    public void testNull() throws Exception {
        Object obj = serializeAndDeserialize(null);
        Assert.assertEquals(obj, null);
    }

    @Test
    public void testShort() throws Exception {
        Object obj = serializeAndDeserialize(Short.MAX_VALUE);

        Assert.assertTrue(obj instanceof Long);
        Long value = (Long) obj;
        Assert.assertTrue(value == Short.MAX_VALUE);
    }

    @Test
    public void testInt() throws Exception {
        Object obj = serializeAndDeserialize(Integer.MAX_VALUE);

        Assert.assertTrue(obj instanceof Long);
        Long value = (Long) obj;
        Assert.assertTrue(value == Integer.MAX_VALUE);
    }

    @Test
    public void testLong() throws Exception {
        Object obj = serializeAndDeserialize(Long.MAX_VALUE);

        Assert.assertTrue(obj instanceof Long);
        Long value = (Long) obj;
        Assert.assertTrue(value == Long.MAX_VALUE);
    }

    @Test
    public void testFloat() throws Exception {
        Object obj = serializeAndDeserialize(Float.MAX_VALUE);

        Assert.assertTrue(obj instanceof Double);
        Double value = (Double) obj;
        Assert.assertTrue(value == Float.MAX_VALUE);
    }

    @Test
    public void testDouble() throws Exception {
        Object obj = serializeAndDeserialize(Double.MAX_VALUE);

        Assert.assertTrue(obj instanceof Double);
        Double value = (Double) obj;
        Assert.assertTrue(value == Double.MAX_VALUE);
    }

    @Test
    public void convertTable() throws Exception {
        Table table = new Table("col1", "col2");
        table.addRow("x1", "x2");
        table.addRow("y1", "y2");

        final Object unpackedObj = serializeAndDeserialize(table);

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

        final Object unpacked = serializeAndDeserialize(g.getVertices());

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

        final Object unpacked = serializeAndDeserialize(iterable);

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

        final Object unpacked = serializeAndDeserialize(iterable);

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

        final Object unpacked = serializeAndDeserialize(iterable);
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

        final Object unpacked = serializeAndDeserialize(map);

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
