package com.tinkerpop.rexster.traversals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.Assert;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.json.JSONArray;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Before;
import org.junit.Test;

import com.tinkerpop.blueprints.pgm.Edge;
import com.tinkerpop.blueprints.pgm.Vertex;
import com.tinkerpop.rexster.Tokens;

public class ElementJSONObjectTest {
	private Mockery mockery = new JUnit4Mockery(); 
	
	@Before
	public void init() {
		this.mockery = new JUnit4Mockery();
	}
	
	@Test
	public void constructorVertexElementNoPropertyKeys(){
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
	public void constructorVertexElementNoPropertyKeysShowDataTypePrimitives(){
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
	public void constructorVertexElementNoPropertyKeysShowDataTypeList(){
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
	public void constructorVertexElementNoPropertyKeysList(){
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
	public void constructorVertexElementNoPropertyKeysMap(){
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
	public void constructorVertexElementNoPropertyKeysShowDataTypeMap(){
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
	public void constructorEdgeElementNoPropertyKeys(){
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
	public void constructorEdgeElementWithPropertyKeys(){
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
			keysToAdd.add(Tokens._ID);
			keysToAdd.add(Tokens._IN_V);
			keysToAdd.add(Tokens._OUT_V);
			
			ElementJSONObject jo = new ElementJSONObject(e, keysToAdd);
			Assert.assertEquals("123", jo.getId());
			Assert.assertEquals("123", jo.getString(Tokens._ID));
			Assert.assertEquals("some-value-for-some-key", jo.getString("some-key"));
			Assert.assertEquals(Tokens.EDGE, jo.getString(Tokens._TYPE));
			Assert.assertEquals("", jo.optString(Tokens._LABEL));
			
		} catch (JSONException ex) {
			ex.printStackTrace();
			Assert.fail(ex.getMessage());
		}
	}
}
