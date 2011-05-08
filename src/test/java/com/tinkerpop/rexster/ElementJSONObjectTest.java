package com.tinkerpop.rexster;

import com.tinkerpop.blueprints.pgm.Edge;
import com.tinkerpop.blueprints.pgm.Vertex;
import junit.framework.Assert;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

public class ElementJSONObjectTest {
    private Mockery mockery = new JUnit4Mockery();

    @Before
    public void init() {
        this.mockery = new JUnit4Mockery();
    }

    @Test
    public void constructorVertexElementNoPropertyKeys() {
        final Vertex v = this.mockery.mock(Vertex.class);
        final Set<String> keys = new HashSet<String>();
        keys.add("some-key");

        this.mockery.checking(new Expectations() {{
            allowing(v).getId();
            will(returnValue("123"));
            allowing(v).getPropertyKeys();
            will(returnValue(keys));
            allowing(v).getProperty("some-key");
            will(returnValue("some-value-for-some-key"));
        }});

        try {
            ElementJSONObject jo = new ElementJSONObject(v);
            Assert.assertEquals("123", jo.getId());
            Assert.assertEquals("123", jo.getString(Tokens._ID));
            Assert.assertEquals("some-value-for-some-key", jo.getString("some-key"));
            Assert.assertEquals(Tokens.VERTEX, jo.getString(Tokens._TYPE));

        } catch (JSONException ex) {
            ex.printStackTrace();
            Assert.fail(ex.getMessage());
        }
    }

    @Test
    public void constructorVertexElementWithPropertyKeys() {
        final Vertex v = this.mockery.mock(Vertex.class);
        final Set<String> keys = new HashSet<String>();
        keys.add("some-key");
        keys.add("other-key");

        this.mockery.checking(new Expectations() {{
            allowing(v).getId();
            will(returnValue("123"));
            allowing(v).getPropertyKeys();
            will(returnValue(keys));
            allowing(v).getProperty("some-key");
            will(returnValue("some-value-for-some-key"));
            allowing(v).getProperty("other-key");
            will(returnValue("other-value-for-some-key"));
        }});

        try {
            List<String> returnKeys = new ArrayList<String>();
            returnKeys.add("some-key");

            // always show meta data even with return key restrictions
            ElementJSONObject jo = new ElementJSONObject(v, returnKeys);
            Assert.assertEquals("123", jo.getId());
            Assert.assertEquals("123", jo.getString(Tokens._ID));
            Assert.assertEquals("some-value-for-some-key", jo.getString("some-key"));
            Assert.assertEquals(Tokens.VERTEX, jo.getString(Tokens._TYPE));

        } catch (JSONException ex) {
            ex.printStackTrace();
            Assert.fail(ex.getMessage());
        }
    }

    @Test
    public void constructorVertexElementNoPropertyKeysShowDataTypePrimitives() {
        final Vertex v = this.mockery.mock(Vertex.class);
        final Set<String> keys = new HashSet<String>();
        keys.add("some-key");
        keys.add("some-float-key");
        keys.add("some-double-key");
        keys.add("some-long-key");
        keys.add("some-integer-key");

        this.mockery.checking(new Expectations() {{

            allowing(v).getId();
            will(returnValue("123"));
            allowing(v).getPropertyKeys();
            will(returnValue(keys));
            oneOf(v).getProperty("some-key");
            will(returnValue("some-value-for-some-key"));
            oneOf(v).getProperty("some-float-key");
            will(returnValue(10.5f));
            oneOf(v).getProperty("some-double-key");
            will(returnValue(20.5d));
            oneOf(v).getProperty("some-integer-key");
            will(returnValue(10));
            oneOf(v).getProperty("some-long-key");
            will(returnValue(100l));
        }});

        try {
            ElementJSONObject jo = new ElementJSONObject(v, null, true);
            Assert.assertEquals("123", jo.getId());

            JSONObject idWithDataType = jo.getJSONObject(Tokens._ID);
            Assert.assertNotNull(idWithDataType);
            Assert.assertEquals("123", idWithDataType.getString("value"));
            Assert.assertEquals("string", idWithDataType.getString("type"));

            JSONObject propWithDataType = jo.getJSONObject("some-key");
            Assert.assertNotNull(propWithDataType);
            Assert.assertEquals("some-value-for-some-key", propWithDataType.getString("value"));
            Assert.assertEquals("string", propWithDataType.getString("type"));

            JSONObject propWithFloatDataType = jo.getJSONObject("some-float-key");
            Assert.assertNotNull(propWithFloatDataType);
            Assert.assertEquals("10.5", propWithFloatDataType.getString("value"));
            Assert.assertEquals("float", propWithFloatDataType.getString("type"));

            JSONObject propWithDoubleDataType = jo.getJSONObject("some-double-key");
            Assert.assertNotNull(propWithDoubleDataType);
            Assert.assertEquals("20.5", propWithDoubleDataType.getString("value"));
            Assert.assertEquals("double", propWithDoubleDataType.getString("type"));

            JSONObject propWithIntegerDataType = jo.getJSONObject("some-integer-key");
            Assert.assertNotNull(propWithIntegerDataType);
            Assert.assertEquals("10", propWithIntegerDataType.getString("value"));
            Assert.assertEquals("integer", propWithIntegerDataType.getString("type"));

            JSONObject propWithLongDataType = jo.getJSONObject("some-long-key");
            Assert.assertNotNull(propWithLongDataType);
            Assert.assertEquals("100", propWithLongDataType.getString("value"));
            Assert.assertEquals("long", propWithLongDataType.getString("type"));

            Assert.assertEquals(Tokens.VERTEX, jo.getString(Tokens._TYPE));

        } catch (JSONException ex) {
            ex.printStackTrace();
            Assert.fail(ex.getMessage());
        }
    }

    @Test
    public void constructorVertexElementNoPropertyKeysShowDataTypeStringArray() {
        final Vertex v = this.mockery.mock(Vertex.class);
        final Set<String> keys = new HashSet<String>();
        keys.add("some-list-key");

        this.mockery.checking(new Expectations() {{

            String[] list = new String[3];
            list[0] = "one";
            list[1] = "two";
            list[2] = "three";

            allowing(v).getId();
            will(returnValue("123"));
            allowing(v).getPropertyKeys();
            will(returnValue(keys));
            oneOf(v).getProperty("some-list-key");
            will(returnValue(list));
        }});

        try {
            ElementJSONObject jo = new ElementJSONObject(v, null, true);

            JSONObject propWithListDataType = jo.getJSONObject("some-list-key");
            Assert.assertNotNull(propWithListDataType);
            Assert.assertEquals("list", propWithListDataType.getString("type"));

            JSONArray jsonList = propWithListDataType.getJSONArray("value");
            Assert.assertNotNull(jsonList);
            Assert.assertEquals(3, jsonList.length());

            JSONObject stringOne = jsonList.getJSONObject(0);
            Assert.assertNotNull(stringOne);
            Assert.assertEquals("one", stringOne.get("value"));
            Assert.assertEquals("string", stringOne.get("type"));

            JSONObject stringTwo = jsonList.getJSONObject(1);
            Assert.assertNotNull(stringTwo);
            Assert.assertEquals("two", stringTwo.get("value"));
            Assert.assertEquals("string", stringTwo.get("type"));

            JSONObject stringThree = jsonList.getJSONObject(2);
            Assert.assertNotNull(stringThree);
            Assert.assertEquals("three", stringThree.get("value"));
            Assert.assertEquals("string", stringThree.get("type"));


        } catch (JSONException ex) {
            ex.printStackTrace();
            Assert.fail(ex.getMessage());
        }
    }

    @Test
    public void constructorVertexElementNoPropertyKeysShowDataTypeIntArray() {
        final Vertex v = this.mockery.mock(Vertex.class);
        final Set<String> keys = new HashSet<String>();
        keys.add("some-list-key");

        this.mockery.checking(new Expectations() {{

            int[] list = new int[3];
            list[0] = 1;
            list[1] = 2;
            list[2] = 3;

            allowing(v).getId();
            will(returnValue("123"));
            allowing(v).getPropertyKeys();
            will(returnValue(keys));
            oneOf(v).getProperty("some-list-key");
            will(returnValue(list));
        }});

        try {
            ElementJSONObject jo = new ElementJSONObject(v, null, true);

            JSONObject propWithListDataType = jo.getJSONObject("some-list-key");
            Assert.assertNotNull(propWithListDataType);
            Assert.assertEquals("list", propWithListDataType.getString("type"));

            JSONArray jsonList = propWithListDataType.getJSONArray("value");
            Assert.assertNotNull(jsonList);
            Assert.assertEquals(3, jsonList.length());

            JSONObject intOne = jsonList.getJSONObject(0);
            Assert.assertNotNull(intOne);
            Assert.assertEquals(1, intOne.get("value"));
            Assert.assertEquals("integer", intOne.get("type"));

            JSONObject intTwo = jsonList.getJSONObject(1);
            Assert.assertNotNull(intTwo);
            Assert.assertEquals(2, intTwo.get("value"));
            Assert.assertEquals("integer", intTwo.get("type"));

            JSONObject intThree = jsonList.getJSONObject(2);
            Assert.assertNotNull(intThree);
            Assert.assertEquals(3, intThree.get("value"));
            Assert.assertEquals("integer", intThree.get("type"));


        } catch (JSONException ex) {
            ex.printStackTrace();
            Assert.fail(ex.getMessage());
        }
    }

    @Test
    public void constructorVertexElementNoPropertyKeysShowDataTypeFloatArray() {
        final Vertex v = this.mockery.mock(Vertex.class);
        final Set<String> keys = new HashSet<String>();
        keys.add("some-list-key");

        this.mockery.checking(new Expectations() {{

            float[] list = new float[3];
            list[0] = 1f;
            list[1] = 2f;
            list[2] = 3f;

            allowing(v).getId();
            will(returnValue("123"));
            allowing(v).getPropertyKeys();
            will(returnValue(keys));
            oneOf(v).getProperty("some-list-key");
            will(returnValue(list));
        }});

        try {
            ElementJSONObject jo = new ElementJSONObject(v, null, true);

            JSONObject propWithListDataType = jo.getJSONObject("some-list-key");
            Assert.assertNotNull(propWithListDataType);
            Assert.assertEquals("list", propWithListDataType.getString("type"));

            JSONArray jsonList = propWithListDataType.getJSONArray("value");
            Assert.assertNotNull(jsonList);
            Assert.assertEquals(3, jsonList.length());

            JSONObject floatOne = jsonList.getJSONObject(0);
            Assert.assertNotNull(floatOne);
            Assert.assertEquals(1f, floatOne.get("value"));
            Assert.assertEquals("float", floatOne.get("type"));

            JSONObject floatTwo = jsonList.getJSONObject(1);
            Assert.assertNotNull(floatTwo);
            Assert.assertEquals(2f, floatTwo.get("value"));
            Assert.assertEquals("float", floatTwo.get("type"));

            JSONObject floatThree = jsonList.getJSONObject(2);
            Assert.assertNotNull(floatThree);
            Assert.assertEquals(3f, floatThree.get("value"));
            Assert.assertEquals("float", floatThree.get("type"));


        } catch (JSONException ex) {
            ex.printStackTrace();
            Assert.fail(ex.getMessage());
        }
    }

    @Test
    public void constructorVertexElementNoPropertyKeysShowDataTypeDoubleArray() {
        final Vertex v = this.mockery.mock(Vertex.class);
        final Set<String> keys = new HashSet<String>();
        keys.add("some-list-key");

        this.mockery.checking(new Expectations() {{

            double[] list = new double[3];
            list[0] = 1.23;
            list[1] = 2.34;
            list[2] = 3.45;

            allowing(v).getId();
            will(returnValue("123"));
            allowing(v).getPropertyKeys();
            will(returnValue(keys));
            oneOf(v).getProperty("some-list-key");
            will(returnValue(list));
        }});

        try {
            ElementJSONObject jo = new ElementJSONObject(v, null, true);

            JSONObject propWithListDataType = jo.getJSONObject("some-list-key");
            Assert.assertNotNull(propWithListDataType);
            Assert.assertEquals("list", propWithListDataType.getString("type"));

            JSONArray jsonList = propWithListDataType.getJSONArray("value");
            Assert.assertNotNull(jsonList);
            Assert.assertEquals(3, jsonList.length());

            JSONObject doubleOne = jsonList.getJSONObject(0);
            Assert.assertNotNull(doubleOne);
            Assert.assertEquals(1.23, doubleOne.get("value"));
            Assert.assertEquals("double", doubleOne.get("type"));

            JSONObject doubleTwo = jsonList.getJSONObject(1);
            Assert.assertNotNull(doubleTwo);
            Assert.assertEquals(2.34, doubleTwo.get("value"));
            Assert.assertEquals("double", doubleTwo.get("type"));

            JSONObject doubleThree = jsonList.getJSONObject(2);
            Assert.assertNotNull(doubleThree);
            Assert.assertEquals(3.45, doubleThree.get("value"));
            Assert.assertEquals("double", doubleThree.get("type"));


        } catch (JSONException ex) {
            ex.printStackTrace();
            Assert.fail(ex.getMessage());
        }
    }

    @Test
    public void constructorVertexElementNoPropertyKeysShowDataTypeLongArray() {
        final Vertex v = this.mockery.mock(Vertex.class);
        final Set<String> keys = new HashSet<String>();
        keys.add("some-list-key");

        this.mockery.checking(new Expectations() {{

            long[] list = new long[3];
            list[0] = 1l;
            list[1] = 2l;
            list[2] = 3l;

            allowing(v).getId();
            will(returnValue("123"));
            allowing(v).getPropertyKeys();
            will(returnValue(keys));
            oneOf(v).getProperty("some-list-key");
            will(returnValue(list));
        }});

        try {
            ElementJSONObject jo = new ElementJSONObject(v, null, true);

            JSONObject propWithListDataType = jo.getJSONObject("some-list-key");
            Assert.assertNotNull(propWithListDataType);
            Assert.assertEquals("list", propWithListDataType.getString("type"));

            JSONArray jsonList = propWithListDataType.getJSONArray("value");
            Assert.assertNotNull(jsonList);
            Assert.assertEquals(3, jsonList.length());

            JSONObject longOne = jsonList.getJSONObject(0);
            Assert.assertNotNull(longOne);
            Assert.assertEquals(1l, longOne.get("value"));
            Assert.assertEquals("long", longOne.get("type"));

            JSONObject longTwo = jsonList.getJSONObject(1);
            Assert.assertNotNull(longTwo);
            Assert.assertEquals(2l, longTwo.get("value"));
            Assert.assertEquals("long", longTwo.get("type"));

            JSONObject longThree = jsonList.getJSONObject(2);
            Assert.assertNotNull(longThree);
            Assert.assertEquals(3l, longThree.get("value"));
            Assert.assertEquals("long", longThree.get("type"));


        } catch (JSONException ex) {
            ex.printStackTrace();
            Assert.fail(ex.getMessage());
        }
    }

    @Test
    public void constructorVertexElementNoPropertyKeysShowDataTypeList() {
        final Vertex v = this.mockery.mock(Vertex.class);
        final Set<String> keys = new HashSet<String>();
        keys.add("some-list-key");

        this.mockery.checking(new Expectations() {{

            ArrayList list = new ArrayList();
            list.add("one");
            list.add(2);
            list.add(200.5f);

            allowing(v).getId();
            will(returnValue("123"));
            allowing(v).getPropertyKeys();
            will(returnValue(keys));
            oneOf(v).getProperty("some-list-key");
            will(returnValue(list));
        }});

        try {
            ElementJSONObject jo = new ElementJSONObject(v, null, true);

            JSONObject propWithListDataType = jo.getJSONObject("some-list-key");
            Assert.assertNotNull(propWithListDataType);
            Assert.assertEquals("list", propWithListDataType.getString("type"));

            JSONArray jsonList = propWithListDataType.getJSONArray("value");
            Assert.assertNotNull(jsonList);
            Assert.assertEquals(3, jsonList.length());

            JSONObject stringOne = jsonList.getJSONObject(0);
            Assert.assertNotNull(stringOne);
            Assert.assertEquals("one", stringOne.get("value"));
            Assert.assertEquals("string", stringOne.get("type"));

            JSONObject intTwo = jsonList.getJSONObject(1);
            Assert.assertNotNull(intTwo);
            Assert.assertEquals(2, intTwo.get("value"));
            Assert.assertEquals("integer", intTwo.get("type"));

            JSONObject floatThree = jsonList.getJSONObject(2);
            Assert.assertNotNull(floatThree);
            Assert.assertEquals(200.5f, floatThree.get("value"));
            Assert.assertEquals("float", floatThree.get("type"));


        } catch (JSONException ex) {
            ex.printStackTrace();
            Assert.fail(ex.getMessage());
        }
    }

    @Test
    public void constructorVertexElementNoPropertyKeysList() {
        final Vertex v = this.mockery.mock(Vertex.class);
        final Set<String> keys = new HashSet<String>();
        keys.add("some-list-key");

        this.mockery.checking(new Expectations() {{

            ArrayList list = new ArrayList();
            list.add("one");
            list.add(2);
            list.add(200.5f);

            allowing(v).getId();
            will(returnValue("123"));
            allowing(v).getPropertyKeys();
            will(returnValue(keys));
            oneOf(v).getProperty("some-list-key");
            will(returnValue(list));
        }});

        try {
            ElementJSONObject jo = new ElementJSONObject(v, null, false);

            JSONArray jsonList = jo.getJSONArray("some-list-key");
            Assert.assertNotNull(jsonList);
            Assert.assertEquals(3, jsonList.length());
            Assert.assertEquals("one", jsonList.get(0));
            Assert.assertEquals(2, jsonList.get(1));
            Assert.assertEquals(200.5f, jsonList.get(2));


        } catch (JSONException ex) {
            ex.printStackTrace();
            Assert.fail(ex.getMessage());
        }
    }

    @Test
    public void constructorVertexElementNoPropertyKeysMap() {
        final Vertex v = this.mockery.mock(Vertex.class);
        final Set<String> keys = new HashSet<String>();
        keys.add("some-map-key");

        this.mockery.checking(new Expectations() {{

            HashMap map = new HashMap();
            map.put("1", "one");
            map.put("2", 2);
            map.put("3", 200.5f);

            allowing(v).getId();
            will(returnValue("123"));
            allowing(v).getPropertyKeys();
            will(returnValue(keys));
            oneOf(v).getProperty("some-map-key");
            will(returnValue(map));
        }});

        try {
            ElementJSONObject jo = new ElementJSONObject(v, null, false);

            JSONObject jsonObject = jo.getJSONObject("some-map-key");
            Assert.assertNotNull(jsonObject);
            Assert.assertEquals("one", jsonObject.get("1"));
            Assert.assertEquals(2, jsonObject.get("2"));
            Assert.assertEquals(200.5f, jsonObject.get("3"));


        } catch (JSONException ex) {
            ex.printStackTrace();
            Assert.fail(ex.getMessage());
        }
    }

    @Test
    public void constructorVertexElementNoPropertyKeysSimpleObject() {
        final Vertex v = this.mockery.mock(Vertex.class);
        final Set<String> keys = new HashSet<String>();
        keys.add("some-simple-key");

        this.mockery.checking(new Expectations() {{

            SimpleObject simple = new SimpleObject();
            simple.setSomeString("one");
            simple.setSomeInt(2);
            simple.setSomeFloat(200.5f);

            allowing(v).getId();
            will(returnValue("123"));
            allowing(v).getPropertyKeys();
            will(returnValue(keys));
            oneOf(v).getProperty("some-simple-key");
            will(returnValue(simple));
        }});

        try {
            ElementJSONObject jo = new ElementJSONObject(v, null, false);
            Assert.assertNotNull("one,200.5,2", jo.get("some-simple-key"));
        } catch (JSONException ex) {
            ex.printStackTrace();
            Assert.fail(ex.getMessage());
        }
    }

    @Test
    public void constructorVertexElementNoPropertyKeysShowDataTypeMap() {
        final Vertex v = this.mockery.mock(Vertex.class);
        final Set<String> keys = new HashSet<String>();
        keys.add("some-map-key");

        this.mockery.checking(new Expectations() {{

            HashMap map = new HashMap();
            map.put("1", "one");
            map.put("2", 2);
            map.put("3", 200.5f);

            allowing(v).getId();
            will(returnValue("123"));
            allowing(v).getPropertyKeys();
            will(returnValue(keys));
            oneOf(v).getProperty("some-map-key");
            will(returnValue(map));
        }});

        try {
            ElementJSONObject jo = new ElementJSONObject(v, null, true);

            JSONObject jsonObject = jo.getJSONObject("some-map-key");
            Assert.assertNotNull(jsonObject);
            Assert.assertEquals("map", jsonObject.get("type"));

            JSONObject jsonMap = jsonObject.getJSONObject("value");

            JSONObject jsonOne = jsonMap.getJSONObject("1");
            Assert.assertNotNull(jsonOne);
            Assert.assertEquals("one", jsonOne.get("value"));
            Assert.assertEquals("string", jsonOne.get("type"));

            JSONObject jsonTwo = jsonMap.getJSONObject("2");
            Assert.assertNotNull(jsonTwo);
            Assert.assertEquals(2, jsonTwo.get("value"));
            Assert.assertEquals("integer", jsonTwo.get("type"));

            JSONObject jsonThree = jsonMap.getJSONObject("3");
            Assert.assertNotNull(jsonThree);
            Assert.assertEquals(200.5f, jsonThree.get("value"));
            Assert.assertEquals("float", jsonThree.get("type"));


        } catch (JSONException ex) {
            ex.printStackTrace();
            Assert.fail(ex.getMessage());
        }
    }

    @Test
    public void constructorEdgeElementNoPropertyKeys() {
        final Edge e = this.mockery.mock(Edge.class);
        final Vertex vIn = this.mockery.mock(Vertex.class, "vIn");
        final Vertex vOut = this.mockery.mock(Vertex.class, "vOut");
        final Set<String> keys = new HashSet<String>();
        keys.add("some-key");

        this.mockery.checking(new Expectations() {{
            allowing(e).getId();
            will(returnValue("123"));
            allowing(e).getPropertyKeys();
            will(returnValue(keys));
            allowing(e).getProperty("some-key");
            will(returnValue("some-value-for-some-key"));
            allowing(e).getLabel();
            will(returnValue("label-value"));
            allowing(vIn).getId();
            will(returnValue("345"));
            allowing(vOut).getId();
            will(returnValue("567"));
            allowing(e).getInVertex();
            will(returnValue(vIn));
            allowing(e).getOutVertex();
            will(returnValue(vOut));
        }});

        try {
            ElementJSONObject jo = new ElementJSONObject(e);
            Assert.assertEquals("123", jo.getId());
            Assert.assertEquals("123", jo.getString(Tokens._ID));
            Assert.assertEquals("some-value-for-some-key", jo.getString("some-key"));
            Assert.assertEquals(Tokens.EDGE, jo.getString(Tokens._TYPE));
            Assert.assertEquals("label-value", jo.getString(Tokens._LABEL));
            Assert.assertEquals("345", jo.getString(Tokens._IN_V));
            Assert.assertEquals("567", jo.getString(Tokens._OUT_V));

        } catch (JSONException ex) {
            ex.printStackTrace();
            Assert.fail(ex.getMessage());
        }
    }

    @Test
    public void constructorEdgeElementWithPropertyKeys() {
        final Edge e = this.mockery.mock(Edge.class);
        final Vertex vIn = this.mockery.mock(Vertex.class, "vIn");
        final Vertex vOut = this.mockery.mock(Vertex.class, "vOut");
        final Set<String> keys = new HashSet<String>();
        keys.add("some-key");

        this.mockery.checking(new Expectations() {{
            allowing(e).getId();
            will(returnValue("123"));
            allowing(e).getPropertyKeys();
            will(returnValue(keys));
            allowing(e).getProperty("some-key");
            will(returnValue("some-value-for-some-key"));
            allowing(e).getLabel();
            will(returnValue("label-value"));
            allowing(vIn).getId();
            will(returnValue("345"));
            allowing(vOut).getId();
            will(returnValue("567"));
            allowing(e).getInVertex();
            will(returnValue(vIn));
            allowing(e).getOutVertex();
            will(returnValue(vOut));
        }});

        try {
            List<String> keysToAdd = new ArrayList<String>();
            keysToAdd.add("some-key");

            // all meta data is always returned.
            ElementJSONObject jo = new ElementJSONObject(e, keysToAdd);
            Assert.assertEquals("123", jo.getId());
            Assert.assertEquals("123", jo.getString(Tokens._ID));
            Assert.assertEquals("some-value-for-some-key", jo.getString("some-key"));
            Assert.assertEquals(Tokens.EDGE, jo.getString(Tokens._TYPE));
            Assert.assertEquals("label-value", jo.optString(Tokens._LABEL));
            Assert.assertEquals("345", jo.optString(Tokens._IN_V));
            Assert.assertEquals("567", jo.optString(Tokens._OUT_V));

        } catch (JSONException ex) {
            ex.printStackTrace();
            Assert.fail(ex.getMessage());
        }
    }

    private class SimpleObject {
        private String someString;
        private int someInt;
        private float someFloat;

        public String getSomeString() {
            return someString;
        }

        public void setSomeString(String someString) {
            this.someString = someString;
        }

        public int getSomeInt() {
            return someInt;
        }

        public void setSomeInt(int someInt) {
            this.someInt = someInt;
        }

        public float getSomeFloat() {
            return someFloat;
        }

        public void setSomeFloat(float someFloat) {
            this.someFloat = someFloat;
        }

        @Override
        public String toString() {
            return this.someString + "," + this.someFloat + "," + this.someInt;
        }

    }
}
