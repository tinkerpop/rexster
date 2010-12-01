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
import com.tinkerpop.rexster.ResultObjectCache;
import com.tinkerpop.rexster.RexsterApplicationGraph;
import com.tinkerpop.rexster.RexsterResourceContext;
import com.tinkerpop.rexster.Tokens;

/**
 * @author: Marko A. Rodriguez (http://markorodriguez.com)
 */
public class AbstractTraversalTest {
	
	private Mockery mockery = new JUnit4Mockery(); 

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
		MockAbstractTraversal mock = new MockAbstractTraversal(true);
		
		try {
			requestObject.put(Tokens.ALLOW_CACHED, true);
			RexsterResourceContext ctx = this.createStandardContext();
			ctx.setRequestObject(requestObject);
			
			mock.evaluate(ctx);
		} catch (JSONException ex) {
			Assert.fail(ex.getMessage());
		} catch (TraversalException tex) {
			Assert.fail(tex.getMessage());
		}
		
		Assert.assertFalse(mock.isTraverseCalled());
	}
	
	@Test
	public void evaluateNonCachedResult() {
		JSONObject requestObject = new JSONObject();
		MockAbstractTraversal mock = new MockAbstractTraversal(false);
		
		try {
			requestObject.put(Tokens.ALLOW_CACHED, true);
			RexsterResourceContext ctx = this.createStandardContext();
			ctx.setRequestObject(requestObject);
			
			mock.evaluate(ctx);
		} catch (JSONException ex) {
			Assert.fail(ex.getMessage());
		} catch (TraversalException tex) {
			Assert.fail(tex.getMessage());
		}
		
		Assert.assertTrue(mock.isTraverseCalled());
	}
	
	@Test
	public void evaluateIgnoreCachedResult() {
		JSONObject requestObject = new JSONObject();
		MockAbstractTraversal mock = new MockAbstractTraversal(true);
		
		try {
			requestObject.put(Tokens.ALLOW_CACHED, false);
			RexsterResourceContext ctx = this.createStandardContext();
			ctx.setRequestObject(requestObject);
			
			mock.evaluate(ctx);
		} catch (JSONException ex) {
			Assert.fail(ex.getMessage());
		} catch (TraversalException tex) {
			Assert.fail(tex.getMessage());
		}
		
		Assert.assertTrue(mock.isTraverseCalled());
	}
	
	private RexsterResourceContext createStandardContext(){
		Graph mockGraph = this.mockery.mock(Graph.class);
		ResultObjectCache mockCache = this.mockery.mock(ResultObjectCache.class);
		final HttpServletRequest request = this.mockery.mock(HttpServletRequest.class);
		final UriInfo uriInfo = this.mockery.mock(UriInfo.class);
		
		try {
			final URI uri = new URI("http://localhost/mock");
			
			this.mockery.checking(new Expectations() {{
				allowing (request).getParameterMap();
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
		
		public MockAbstractTraversal(boolean resultInCache) {
			this.resultInCache = resultInCache;
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
		
		public boolean isTraverseCalled() {
			return this.traverseCalled;
		}

		@Override
		protected boolean isResultInCache() {
			return this.resultInCache;
		}
	}
}


