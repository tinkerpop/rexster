package com.tinkerpop.rexster.traversals;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.Assert;

import org.codehaus.jettison.json.JSONException;
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
