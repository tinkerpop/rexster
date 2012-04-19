package com.tinkerpop.rexster.gremlin.converter;

import com.tinkerpop.pipes.util.structures.Table;
import org.junit.Assert;
import org.junit.Test;
import org.msgpack.MessagePack;
import org.msgpack.type.Value;
import org.msgpack.type.ValueFactory;
import org.msgpack.unpacker.Converter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.msgpack.template.Templates.tMap;
import static org.msgpack.template.Templates.TString;
import static org.msgpack.template.Templates.TValue;


public class MsgPackResultConverterTest {

    private MsgPackResultConverter converter = new MsgPackResultConverter();
    private MessagePack msgpack = new MessagePack();
    /*
    @Test
    public void convertNullResultReturnsNull() throws Exception {
        byte[] results = this.converter.convert(null);
        Assert.assertNull(results);
    }

    @Test
    public void convertJSONObjectNullResultReturnsNull() throws Exception {
        byte[] results = this.converter.convert(ValueFactory.createNilValue());
        Assert.assertNotNull(results);
        Assert.assertEquals(1, results.size());
    }

    @Test
    public void convertTable() throws Exception {
        Table table = new Table("col1", "col2");
        table.addRow("x1", "x2");
        table.addRow("y1", "y2");

        List<byte[]> results = this.converter.convert(table);

        Assert.assertNotNull(results);
        Assert.assertEquals(2, results.size());

        boolean rowMatchX = false;
        boolean rowMatchY = false;
        
        for (int ix = 0; ix < results.size(); ix++) {
            Map<String, String> row = msgpack.read(results.get(ix), tMap(TString, TString));

            Assert.assertNotNull(row);
            Assert.assertTrue(row.containsKey("col1"));
            Assert.assertTrue(row.containsKey("col2"));

            if (row.get("col1").equals("x1") && row.get("col2").equals("x2")) {
                rowMatchX = true;
            }

            if (row.get("col1").equals("y1") && row.get("col2").equals("y2")) {
                rowMatchY = true;
            }
        }

        Assert.assertTrue(rowMatchX && rowMatchY);
    }

    @Test
    public void convertIterable() throws Exception {

        ArrayList<FunObject> funList = new ArrayList<FunObject>();
        funList.add(new FunObject("x"));
        funList.add(new FunObject("y"));
        Iterable<FunObject> iterable = funList;

        List<byte[]> converted = this.converter.convert(iterable);

        Assert.assertNotNull(converted);
        Assert.assertEquals(2, converted.size());

        Assert.assertEquals("x", msgpack.read(converted.get(0), TString));
        Assert.assertEquals("y", msgpack.read(converted.get(1), TString));
    }

    @Test
    public void convertIterator() throws Exception {

        ArrayList<FunObject> funList = new ArrayList<FunObject>();
        funList.add(new FunObject("x"));
        funList.add(new FunObject("y"));
        Iterator<FunObject> iterable = funList.iterator();

        List<byte[]> converted = this.converter.convert(iterable);

        Assert.assertNotNull(converted);
        Assert.assertEquals(2, converted.size());

        Assert.assertEquals("x", msgpack.read(converted.get(0), TString));
        Assert.assertEquals("y", msgpack.read(converted.get(1), TString));
    }

    @Test
    public void convertIteratorNullElement() throws Exception {

        ArrayList<FunObject> funList = new ArrayList<FunObject>();
        funList.add(new FunObject("x"));
        funList.add(null);
        funList.add(new FunObject("y"));
        Iterator<FunObject> iterable = funList.iterator();

        List<byte[]> converted = this.converter.convert(iterable);

        Assert.assertNotNull(converted);
        Assert.assertEquals(3, converted.size());

        Assert.assertEquals("x", msgpack.read(converted.get(0), TString));
        Assert.assertTrue(msgpack.read(converted.get(1)).isNilValue());
        Assert.assertEquals("y", msgpack.read(converted.get(2), TString));

    }

    @Test
    public void convertMap() throws Exception {
        Map<String, Object> map = new HashMap<String, Object>();
        Map<String, String> innerMap = new HashMap<String, String>();
        innerMap.put("a", "b");

        map.put("x", new FunObject("x"));
        map.put("y", "some");
        map.put("z", innerMap);

        List<byte[]> converted = this.converter.convert(map);

        Assert.assertNotNull(converted);
        Assert.assertEquals(1, converted.size());

        byte[] msgPacked = converted.get(0);
        Map<String, Value> mapValue = msgpack.read(msgPacked, tMap(TString, TValue));

        Assert.assertEquals("some", mapValue.get("y").asRawValue().getString());
        Assert.assertEquals("x", mapValue.get("x").asRawValue().getString());

        Map innerMapValue = new Converter(mapValue.get("z").asMapValue()).read(tMap(TString, TString));
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
    */
}
