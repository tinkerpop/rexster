package com.tinkerpop.rexster.gremlin.converter;

import com.tinkerpop.blueprints.impls.tg.TinkerGraph;
import com.tinkerpop.blueprints.impls.tg.TinkerGraphFactory;
import com.tinkerpop.pipes.util.structures.Table;
import com.tinkerpop.rexster.protocol.serializer.msgpack.templates.ResultsConverter;
import org.junit.Assert;
import org.junit.Test;
import org.msgpack.MessagePack;
import org.msgpack.template.Template;
import org.msgpack.type.MapValue;
import org.msgpack.type.Value;
import org.msgpack.type.ValueFactory;
import org.msgpack.unpacker.BufferUnpacker;
import org.msgpack.unpacker.Converter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static org.msgpack.template.Templates.TString;
import static org.msgpack.template.Templates.TValue;
import static org.msgpack.template.Templates.tMap;


public class MsgPackResultConverterTest {

    private MsgPackResultConverter converter = new MsgPackResultConverter();
    private MessagePack msgpack = new MessagePack();

    @Test
    public void convertNullResultReturnsNull() throws Exception {
        byte[] results = this.converter.convert(null);
        Assert.assertNotNull(results);
        Assert.assertTrue(msgpack.read(results).isNilValue());
    }

    @Test
    public void convertJSONObjectNullResultReturnsNull() throws Exception {
        byte[] results = this.converter.convert(ValueFactory.createNilValue());
        Assert.assertNotNull(results);
        Assert.assertTrue(msgpack.read(results).isNilValue());
    }

    @Test
    public void convertTable() throws Exception {
        Table table = new Table("col1", "col2");
        table.addRow("x1", "x2");
        table.addRow("y1", "y2");

        byte[] results = this.converter.convert(table);

        Assert.assertNotNull(results);


        final Object unpackedObj = ResultsConverter.deserializeObject(this.msgpack.read(results));
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
        byte[] converted = this.converter.convert(g.getVertices());

        final BufferUnpacker unpacker = msgpack.createBufferUnpacker(converted);
        final Object unpacked = unpacker.readValue();

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

        ArrayList<FunObject> funList = new ArrayList<FunObject>();
        funList.add(new FunObject("x"));
        funList.add(new FunObject("y"));
        Iterable<FunObject> iterable = funList;

        byte[] converted = this.converter.convert(iterable);

        Assert.assertNotNull(converted);

        final BufferUnpacker unpacker = msgpack.createBufferUnpacker(converted);
        final Object unpacked = unpacker.readValue();

        Assert.assertTrue(unpacked instanceof Iterable);

        final Iterator unpackerItty = ((Iterable) unpacked).iterator();

        int counter = 0;
        boolean matchX = false;
        boolean matchY = false;

        while (unpackerItty.hasNext()) {
            final Value v = (Value) unpackerItty.next();
            if (v.asRawValue().getString().equals("x")) {
                matchX = true;
            }

            if (v.asRawValue().getString().equals("y")) {
                matchY = true;
            }

            counter++;
        }

        Assert.assertEquals(2, counter);
        Assert.assertTrue(matchX && matchY);
    }

    @Test
    public void convertIterator() throws Exception {

        ArrayList<FunObject> funList = new ArrayList<FunObject>();
        funList.add(new FunObject("x"));
        funList.add(new FunObject("y"));
        Iterator<FunObject> iterable = funList.iterator();

        byte[] converted = this.converter.convert(iterable);

        final BufferUnpacker unpacker = msgpack.createBufferUnpacker(converted);
        final Object unpacked = unpacker.readValue();

        Assert.assertTrue(unpacked instanceof Iterable);

        final Iterator unpackerItty = ((Iterable) unpacked).iterator();

        int counter = 0;
        boolean matchX = false;
        boolean matchY = false;

        while (unpackerItty.hasNext()) {
            final Value v = (Value) unpackerItty.next();
            if (v.asRawValue().getString().equals("x")) {
                matchX = true;
            }

            if (v.asRawValue().getString().equals("y")) {
                matchY = true;
            }

            counter++;
        }

        Assert.assertEquals(2, counter);
        Assert.assertTrue(matchX && matchY);
    }

    @Test
    public void convertIteratorNullElement() throws Exception {

        ArrayList<FunObject> funList = new ArrayList<FunObject>();
        funList.add(new FunObject("x"));
        funList.add(null);
        funList.add(new FunObject("y"));
        Iterator<FunObject> iterable = funList.iterator();

        byte[] converted = this.converter.convert(iterable);

        final BufferUnpacker unpacker = msgpack.createBufferUnpacker(converted);
        final Object unpacked = unpacker.readValue();
        final Iterator unpackerItty = ((Iterable) unpacked).iterator();

        int counter = 0;
        boolean matchX = false;
        boolean matchY = false;
        boolean matchNil = false;

        while (unpackerItty.hasNext()) {
            final Value v = (Value) unpackerItty.next();
            if (v.isRawValue() && v.asRawValue().getString().equals("x")) {
                matchX = true;
            }

            if (v.isRawValue() && v.asRawValue().getString().equals("y")) {
                matchY = true;
            }

            if (v.isNilValue()) {
                matchNil = true;
            }

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

        map.put("x", new FunObject("x"));
        map.put("y", "some");
        map.put("z", innerMap);

        byte[] converted = this.converter.convert(map);

        Assert.assertNotNull(converted);

        final BufferUnpacker unpacker = msgpack.createBufferUnpacker(converted);
        final Template<Map<String, Value>> mapTmpl = tMap(TString, TValue);

        Map<String, Value> unpackedMap = unpacker.read(mapTmpl);
        Assert.assertTrue(unpackedMap.containsKey("x"));
        Assert.assertTrue(unpackedMap.containsKey("y"));
        Assert.assertTrue(unpackedMap.containsKey("z"));
        Assert.assertEquals("x", unpackedMap.get("x").asRawValue().getString());
        Assert.assertEquals("some", unpackedMap.get("y").asRawValue().getString());

        MapValue mapValue = unpackedMap.get("z").asMapValue();
        Map innerMapValue = new Converter(mapValue).read(tMap(TString, TString));
        Assert.assertNotNull(innerMapValue);
        Assert.assertEquals("b", innerMapValue.get("a"));
    }

    private class FunObject {
        private String val;

        public FunObject(String val) {
            this.val = val;
        }

        public String toString() {
            return this.val;
        }
    }
}
