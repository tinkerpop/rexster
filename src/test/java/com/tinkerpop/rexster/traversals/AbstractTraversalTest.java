package com.tinkerpop.rexster.traversals;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.UriInfo;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Assert;
import org.junit.Test;

import com.tinkerpop.blueprints.pgm.Graph;
import com.tinkerpop.rexster.MapResultObjectCache;
import com.tinkerpop.rexster.ResultObjectCache;
import com.tinkerpop.rexster.RexsterApplicationGraph;
import com.tinkerpop.rexster.RexsterResourceContext;
import com.tinkerpop.rexster.Tokens;

/**
 * @author: Marko A. Rodriguez (http://markorodriguez.com)
 */
public class AbstractTraversalTest {
	
	private Mockery mockery = new JUnit4Mockery(); 
	private final String baseUri = "http://localhost/mock";

	@Test(expected = TraversalException.class)
    public void evaluateNullContext() throws TraversalException {
        AbstractTraversal at = new MockAbstractTraversal();
        at.evaluate(null);
    }
	
	@Test(expected = TraversalException.class)
    public void evaluateNullApplicationGraph() throws TraversalException {
        AbstractTraversal at = new MockAbstractTraversal();
        RexsterResourceContext ctx = createStandardContext();
        ctx.setRexsterApplicationGraph(null);
        at.evaluate(ctx);
    }
	
	@Test(expected = TraversalException.class)
    public void evaluateNullGraph() throws TraversalException {
        AbstractTraversal at = new MockAbstractTraversal();
        RexsterResourceContext ctx = createStandardContext();
        ctx.setRexsterApplicationGraph(new RexsterApplicationGraph("mock", null));
        at.evaluate(ctx);
    }
	
	@Test(expected = TraversalException.class)
    public void evaluateNullResultObject() throws TraversalException {
        AbstractTraversal at = new MockAbstractTraversal();
        RexsterResourceContext ctx = createStandardContext();
        ctx.setResultObject(null);
        at.evaluate(ctx);
    }
	
	@Test(expected = TraversalException.class)
    public void evaluateNullRequestObject() throws TraversalException {
        AbstractTraversal at = new MockAbstractTraversal();
        RexsterResourceContext ctx = createStandardContext();
        ctx.setRequestObject(null);
        at.evaluate(ctx);
    }
	
	@Test
	public void evaluateCachedResult() {
		JSONObject requestObject = new JSONObject();
		MockAbstractTraversal mock = new MockAbstractTraversal(true, true);
		JSONObject resultObject = null;
		
		try {
			requestObject.put(Tokens.ALLOW_CACHED, true);
			requestObject.put("junk", "morejunk");
			RexsterResourceContext ctx = this.createStandardContext();
			ctx.setRequestObject(requestObject);
			
			resultObject = mock.evaluate(ctx);
		} catch (JSONException ex) {
			Assert.fail(ex.getMessage());
		} catch (TraversalException tex) {
			Assert.fail(tex.getMessage());
		}
		
		Assert.assertFalse(mock.isTraverseCalled());
		Assert.assertEquals(this.baseUri + "[]", mock.getCachedRequestUri());
		this.assertSuccessResultObject(resultObject, true);
	}
	
	@Test
	public void evaluateCachedResultWithParameteredUri() {
		JSONObject requestObject = new JSONObject();
		MockAbstractTraversal mock = new MockAbstractTraversal(true, true);
		JSONObject resultObject = null;
		
		try {
			Map<String, String> map = new HashMap<String, String>();
			map.put("here", "h");
			map.put("there", "t");
			
			requestObject.put(Tokens.ALLOW_CACHED, true);
			requestObject.put("here", "h");
			requestObject.put("there", "t");
			RexsterResourceContext ctx = this.createStandardContext(map);
			ctx.setRequestObject(requestObject);
			
			resultObject = mock.evaluate(ctx);
		} catch (JSONException ex) {
			Assert.fail(ex.getMessage());
		} catch (TraversalException tex) {
			Assert.fail(tex.getMessage());
		}
		
		Assert.assertFalse(mock.isTraverseCalled());
		Assert.assertEquals(this.baseUri + "[here=h, there=t]", mock.getCachedRequestUri());
		this.assertSuccessResultObject(resultObject, true);
	}
	
	@Test
	public void evaluateNonCachedResult() {
		JSONObject requestObject = new JSONObject();
		MockAbstractTraversal mock = new MockAbstractTraversal(false, true);
		JSONObject resultObject = null;
		
		try {
			requestObject.put(Tokens.ALLOW_CACHED, true);
			RexsterResourceContext ctx = this.createStandardContext();
			ctx.setRequestObject(requestObject);
			
			resultObject = mock.evaluate(ctx);
		} catch (JSONException ex) {
			Assert.fail(ex.getMessage());
		} catch (TraversalException tex) {
			Assert.fail(tex.getMessage());
		}
		
		Assert.assertTrue(mock.isTraverseCalled());
		this.assertSuccessResultObject(resultObject, true);
	}
	
	@Test
	public void evaluateIgnoreCachedResult() {
		JSONObject requestObject = new JSONObject();
		MockAbstractTraversal mock = new MockAbstractTraversal(true, true);
		JSONObject resultObject = null;
		
		try {
			requestObject.put(Tokens.ALLOW_CACHED, false);
			RexsterResourceContext ctx = this.createStandardContext();
			ctx.setRequestObject(requestObject);
			
			resultObject = mock.evaluate(ctx);
		} catch (JSONException ex) {
			Assert.fail(ex.getMessage());
		} catch (TraversalException tex) {
			Assert.fail(tex.getMessage());
		}
		
		Assert.assertTrue(mock.isTraverseCalled());
		this.assertSuccessResultObject(resultObject, true);
	}
	
	@Test
	public void evaluateNoSuccess() {
		JSONObject requestObject = new JSONObject();
		MockAbstractTraversal mock = new MockAbstractTraversal(true, false);
		JSONObject resultObject = null;
		
		try {
			requestObject.put(Tokens.ALLOW_CACHED, false);
			RexsterResourceContext ctx = this.createStandardContext();
			ctx.setRequestObject(requestObject);
			
			resultObject = mock.evaluate(ctx);
		} catch (JSONException ex) {
			Assert.fail(ex.getMessage());
		} catch (TraversalException tex) {
			Assert.fail(tex.getMessage());
		}
		
		Assert.assertTrue(mock.isTraverseCalled());
		this.assertSuccessResultObject(resultObject, false);
	}
	
	@Test
	public void evaluateWithMessage() {
		JSONObject requestObject = new JSONObject();
		MockAbstractTraversal mock = new MockAbstractTraversal(true, false, "amessage");
		JSONObject resultObject = null;
		
		try {
			requestObject.put(Tokens.ALLOW_CACHED, false);
			RexsterResourceContext ctx = this.createStandardContext();
			ctx.setRequestObject(requestObject);
			
			resultObject = mock.evaluate(ctx);
		} catch (JSONException ex) {
			Assert.fail(ex.getMessage());
		} catch (TraversalException tex) {
			Assert.fail(tex.getMessage());
		}
		
		Assert.assertTrue(mock.isTraverseCalled());
		try {
			Assert.assertEquals("amessage", resultObject.getString(Tokens.MESSAGE));
		} catch (JSONException ex) {
			Assert.fail(ex.getMessage());
		}
	}
	
	
	@Test
	public void getRequestValueNoMatchingKey() {
		JSONObject requestObject = new JSONObject();
		MockAbstractTraversal mock = new MockAbstractTraversal(false, true);
		
		try {
			requestObject.put(Tokens.ALLOW_CACHED, true);
			RexsterResourceContext ctx = this.createStandardContext();
			ctx.setRequestObject(requestObject);
			
			mock.evaluate(ctx);
			
			Assert.assertNull(mock.getRequestValue("sometoken"));
		} catch (JSONException ex) {
			Assert.fail(ex.getMessage());
		} catch (TraversalException e) {
			Assert.fail(e.getMessage());
		}
		
	}
	
	@Test
	public void getRequestValueMatchingKey() {
		JSONObject requestObject = new JSONObject();
		MockAbstractTraversal mock = new MockAbstractTraversal(false, true);
		
		try {
			requestObject.put(Tokens.ALLOW_CACHED, true);
			requestObject.put("sometoken", "this");
			RexsterResourceContext ctx = this.createStandardContext();
			ctx.setRequestObject(requestObject);
			
			mock.evaluate(ctx);
			
			String value = mock.getRequestValue("sometoken");
			Assert.assertNotNull(value);
			Assert.assertEquals("this", value);
		} catch (JSONException ex) {
			Assert.fail(ex.getMessage());
		} catch (TraversalException e) {
			Assert.fail(e.getMessage());
		}
		
	}
	
	@Test
	public void cacheCurrentResultObjectStateValid(){
		JSONObject requestObject = new JSONObject();
		MockAbstractTraversal mock = new MockAbstractTraversal(true, true);
		JSONObject resultObject = null;
		
		ResultObjectCache cache = new MapResultObjectCache();
		
		try {
			Map<String, String> map = new HashMap<String, String>();
			map.put("here", "h");
			map.put("there", "t");
			
			requestObject.put(Tokens.ALLOW_CACHED, true);
			requestObject.put("here", "h");
			requestObject.put("there", "t");
			RexsterResourceContext ctx = this.createStandardContext(map);
			ctx.setRequestObject(requestObject);
			ctx.setCache(cache);
			
			resultObject = mock.evaluate(ctx);
			mock.cacheCurrentResultObjectState();
		} catch (JSONException ex) {
			Assert.fail(ex.getMessage());
		} catch (TraversalException tex) {
			Assert.fail(tex.getMessage());
		}
		
		Assert.assertNotNull(cache.getCachedResult(this.baseUri + "[here=h, there=t]"));
	}
	
	private void assertSuccessResultObject(JSONObject resultObject, boolean expectedSuccess) {
		Assert.assertNotNull(resultObject);
		
		try {
			Assert.assertEquals(expectedSuccess, resultObject.getBoolean(Tokens.SUCCESS));
		} catch (JSONException ex) {
			Assert.fail(ex.getMessage());
		}
	}
	
	private RexsterResourceContext createStandardContext() {
		return this.createStandardContext(new HashMap<String, String>());
	}
	
	private RexsterResourceContext createStandardContext(final Map<String, String> parameterMap){
		Graph mockGraph = this.mockery.mock(Graph.class);
		ResultObjectCache mockCache = this.mockery.mock(ResultObjectCache.class);
		final HttpServletRequest request = this.mockery.mock(HttpServletRequest.class);
		final UriInfo uriInfo = this.mockery.mock(UriInfo.class);
		
		try {
			final URI uri = new URI(this.baseUri);
			
			this.mockery.checking(new Expectations() {{
				allowing (request).getParameterMap();
					will(returnValue(parameterMap));
				allowing (uriInfo).getBaseUri();
					will(returnValue(uri));
		    }});
			
		} catch (URISyntaxException ex) {
			Assert.fail(ex.getMessage());
		}
		
		return createContext(
        		new RexsterApplicationGraph("mock", mockGraph), new JSONObject(), 
        			new JSONObject(), mockCache, request, uriInfo);
	}
	
	private RexsterResourceContext createContext(RexsterApplicationGraph appGraph, 
			JSONObject requestObject, JSONObject resultObject, ResultObjectCache cache,
			HttpServletRequest request, UriInfo uriInfo){
		RexsterResourceContext ctx = new RexsterResourceContext();
		ctx.setCache(cache);
		ctx.setRequestObject(requestObject);
		ctx.setResultObject(resultObject);
		ctx.setRexsterApplicationGraph(appGraph);
		ctx.setRequest(request);
		ctx.setUriInfo(uriInfo);
		
		return ctx;
	}
	
	protected class MockAbstractTraversal extends AbstractTraversal {

		private boolean traverseCalled = false;
		private boolean resultInCache = false;
		
		public MockAbstractTraversal() {
		}
		
		public MockAbstractTraversal(boolean resultInCache, boolean success) {
			this.resultInCache = resultInCache;
			this.success = success;
		}
		
		public MockAbstractTraversal(boolean resultInCache, boolean success, String message) {
			this(resultInCache, success); 
			this.message = message;
		}
		
		@Override
		public String getTraversalName() {
			return "mock";
		}

		@Override
		protected void traverse() throws JSONException {
			this.traverseCalled = true;
		}

		@Override
		protected void addApiToResultObject() {
		}
		
		public Map<String, Object> getParameters() {
	        return super.getParameters();
	    }
		
		public String getRequestValue(String requestObjectKey) {
			return super.getRequestValue(requestObjectKey);
		}
		
		public boolean isTraverseCalled() {
			return this.traverseCalled;
		}

		@Override
		protected boolean isResultInCache() {
			return this.resultInCache;
		}
		
		public String getCachedRequestUri() {
			return this.cacheRequestURI;
		}
		
		public void cacheCurrentResultObjectState(){
			super.cacheCurrentResultObjectState();
		}
	}
}


