package com.tinkerpop.rexster.gremlin.converter;

import com.tinkerpop.pipes.util.Table;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class JSONResultConverterTest {

    private JSONResultConverter converterNotPaged = new JSONResultConverter(false, 0, Long.MAX_VALUE, null);
    private JSONResultConverter converterPaged = new JSONResultConverter(false, 1, 3, null);

    @Test
    public void convertNullResultReturnsNull() throws Exception {
        JSONArray results = this.converterNotPaged.convert(null);
        Assert.assertNull(results);
    }

    @Test
    public void convertJSONObjectNullResultReturnsNull() throws Exception {
        JSONArray results = this.converterNotPaged.convert(JSONObject.NULL);
        Assert.assertNotNull(results);
        Assert.assertEquals(1, results.length());
    }

    @Test
    public void convertTableNotPaged() throws Exception {
        Table table = new Table("col1", "col2");
        table.addRow("x1", "x2");
        table.addRow("y1", "y2");

        JSONArray results = this.converterNotPaged.convert(table);

        Assert.assertNotNull(results);
        Assert.assertEquals(2, results.length());

        boolean rowMatchX = false;
        boolean rowMatchY = false;
        for (int ix = 0; ix < results.length(); ix++) {
            JSONObject row = results.optJSONObject(ix);

            Assert.assertNotNull(row);
            Assert.assertTrue(row.has("col1"));
            Assert.assertTrue(row.has("col2"));

            if (row.optString("col1").equals("x1") && row.optString("col2").equals("x2")) {
                rowMatchX = true;
            }

            if (row.optString("col1").equals("y1") && row.optString("col2").equals("y2")) {
                rowMatchY = true;
            }
        }

        Assert.assertTrue(rowMatchX && rowMatchY);
    }

    @Test
    public void convertTablePaged() throws Exception {
        Table table = new Table("col1", "col2");
        table.addRow("x1", "x2");
        table.addRow("y1", "y2");
        table.addRow("z1", "z2");
        table.addRow("a1", "a2");

        JSONArray results = this.converterPaged.convert(table);

        Assert.assertNotNull(results);
        Assert.assertEquals(2, results.length());

        boolean rowMatchZ = false;
        boolean rowMatchY = false;
        for (int ix = 0; ix < results.length(); ix++) {
            JSONObject row = results.optJSONObject(ix);

            Assert.assertNotNull(row);
            Assert.assertTrue(row.has("col1"));
            Assert.assertTrue(row.has("col2"));

            if (row.optString("col1").equals("z1") && row.optString("col2").equals("z2")) {
                rowMatchZ = true;
            }

            if (row.optString("col1").equals("y1") && row.optString("col2").equals("y2")) {
                rowMatchY = true;
            }
        }

        Assert.assertTrue(rowMatchZ && rowMatchY);
    }

    @Test
    public void convertIterableNotPaged() throws Exception {

        ArrayList<FunObject> funList = new ArrayList<FunObject>();
        funList.add(new FunObject("x"));
        funList.add(new FunObject("y"));
        Iterable<FunObject> iterable = funList;

        JSONArray converted = this.converterNotPaged.convert(iterable);

        Assert.assertNotNull(converted);
        Assert.assertEquals(2, converted.length());

        Assert.assertEquals("x", converted.get(0));
        Assert.assertEquals("y", converted.get(1));
    }

    @Test
    public void convertIterablePaged() throws Exception {

        ArrayList<FunObject> funList = new ArrayList<FunObject>();
        funList.add(new FunObject("x"));
        funList.add(new FunObject("y"));
        funList.add(new FunObject("z"));
        funList.add(new FunObject("a"));
        Iterable<FunObject> iterable = funList;

        JSONArray converted = this.converterPaged.convert(iterable);

        Assert.assertNotNull(converted);
        Assert.assertEquals(2, converted.length());

        Assert.assertEquals("y", converted.get(0));
        Assert.assertEquals("z", converted.get(1));
    }

    @Test
    public void convertIteratorNotPaged() throws Exception {

        ArrayList<FunObject> funList = new ArrayList<FunObject>();
        funList.add(new FunObject("x"));
        funList.add(new FunObject("y"));
        Iterator<FunObject> iterable = funList.iterator();

        JSONArray converted = this.converterNotPaged.convert(iterable);

        Assert.assertNotNull(converted);
        Assert.assertEquals(2, converted.length());

        Assert.assertEquals("x", converted.get(0));
        Assert.assertEquals("y", converted.get(1));
    }

    @Test
    public void convertIteratorNotPagedNullElement() throws Exception {

        ArrayList<FunObject> funList = new ArrayList<FunObject>();
        funList.add(new FunObject("x"));
        funList.add(null);
        funList.add(new FunObject("y"));
        Iterator<FunObject> iterable = funList.iterator();

        JSONArray converted = this.converterNotPaged.convert(iterable);

        Assert.assertNotNull(converted);
        Assert.assertEquals(3, converted.length());

        Assert.assertEquals("x", converted.get(0));
        Assert.assertNull(converted.opt(1));
        Assert.assertEquals("y", converted.get(2));
    }

    @Test
    public void convertIteratorPaged() throws Exception {

        ArrayList<FunObject> funList = new ArrayList<FunObject>();
        funList.add(new FunObject("x"));
        funList.add(new FunObject("y"));
        funList.add(new FunObject("z"));
        funList.add(new FunObject("a"));
        Iterator<FunObject> iterable = funList.iterator();

        JSONArray converted = this.converterPaged.convert(iterable);

        Assert.assertNotNull(converted);
        Assert.assertEquals(2, converted.length());
    }

    @Test
    public void convertMap() throws Exception {
        Map<String, Object> map = new HashMap<String, Object>();
        Map<String, String> innerMap = new HashMap<String, String>();
        innerMap.put("a", "b");

        map.put("x", new FunObject("x"));
        map.put("y", "some");
        map.put("z", innerMap);

        JSONArray converted = this.converterNotPaged.convert(map);

        Assert.assertNotNull(converted);
        Assert.assertEquals(1, converted.length());

        JSONObject jsonObject = converted.getJSONObject(0);
        Assert.assertNotNull(jsonObject);
        Assert.assertEquals("some", jsonObject.optString("y"));
        Assert.assertEquals("x", jsonObject.optString("x"));

        JSONObject innerJsonObject = jsonObject.optJSONObject("z");
        Assert.assertNotNull(innerJsonObject);
        Assert.assertEquals("b", innerJsonObject.optString("a"));
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
