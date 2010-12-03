package com.tinkerpop.rexster.traversals;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.UriInfo;

import org.codehaus.jettison.json.JSONObject;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Assert;
import org.junit.Before;

import com.tinkerpop.blueprints.pgm.Graph;
import com.tinkerpop.rexster.ResultObjectCache;
import com.tinkerpop.rexster.RexsterApplicationGraph;
import com.tinkerpop.rexster.RexsterResourceContext;

public abstract class TraversalBaseTest {

	protected Mockery mockery = new JUnit4Mockery(); 
	protected final String baseUri = "http://localhost/mock";
	
	@Before
	public void init() {
		this.mockery = new JUnit4Mockery();
	}
	
	protected RexsterResourceContext createStandardContext() {
		return this.createStandardContext(new HashMap<String, String>());
	}
	
	protected RexsterResourceContext createStandardContext(final Map<String, String> parameterMap){
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
	
	protected RexsterResourceContext createContext(RexsterApplicationGraph appGraph, 
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
}
