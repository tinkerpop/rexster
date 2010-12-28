package com.tinkerpop.rexster;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.Status;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.tinkerpop.blueprints.pgm.Graph;
import com.tinkerpop.rexster.traversals.Traversal;

public class TraversalResourceTest {
	protected Mockery mockery = new JUnit4Mockery();
    protected final String baseUri = "http://localhost/mock";

    @Before
    public void init() {
        this.mockery = new JUnit4Mockery();
    }
    
    @Test
    public void getTraversalsValid(){
    	final Graph graph = this.mockery.mock(Graph.class);
    	final RexsterApplicationGraph rag = new RexsterApplicationGraph("graph", graph);
        final RexsterApplicationProvider rap = this.mockery.mock(RexsterApplicationProvider.class);
        
        Map<String, Class<? extends Traversal>> loadedTraversals = new HashMap<String, Class<? extends Traversal>>();
        loadedTraversals.put("some-traversal", MockTraversal.class);
        rag.setLoadedTraversals(loadedTraversals);
        
    	final UriInfo uri = this.mockery.mock(UriInfo.class);
    	final HttpServletRequest httpServletRequest = this.mockery.mock(HttpServletRequest.class);
    	
    	this.mockery.checking(new Expectations() {{
    		allowing(httpServletRequest).getParameterMap();
            will(returnValue(new HashMap<String, String>()));
            allowing(rap).getApplicationGraph(with(any(String.class)));
            will(returnValue(rag));
        }});
    	
    	TraversalResource resource = new TraversalResource("graph", uri, httpServletRequest, rap);
    	Response response = resource.getTraversals();
    	
    	Assert.assertNotNull(response);
    	Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
    	Assert.assertNotNull(response.getEntity());
    	Assert.assertTrue(response.getEntity() instanceof JSONObject);
    	
    	JSONObject json = (JSONObject) response.getEntity();
    	Assert.assertTrue(json.has(Tokens.TOTAL_SIZE));
    	Assert.assertEquals(1, json.optInt(Tokens.TOTAL_SIZE));
    	Assert.assertTrue(json.has(Tokens.QUERY_TIME));
    	Assert.assertTrue(json.optDouble(Tokens.QUERY_TIME) > 0);
    	
    	Assert.assertTrue(json.has(Tokens.RESULTS));
    	Assert.assertFalse(json.isNull(Tokens.RESULTS));
    	
    	JSONArray jsonResults = json.optJSONArray(Tokens.RESULTS);
    	Assert.assertEquals(1, jsonResults.length());
    }
    
    @Test(expected = WebApplicationException.class)
    public void getTraversalNotFound(){
    	final Graph graph = this.mockery.mock(Graph.class);
    	final RexsterApplicationGraph rag = new RexsterApplicationGraph("graph", graph);
        final RexsterApplicationProvider rap = this.mockery.mock(RexsterApplicationProvider.class);
        
        Map<String, Class<? extends Traversal>> loadedTraversals = new HashMap<String, Class<? extends Traversal>>();
        loadedTraversals.put("some-traversal-that-does-not-match", MockTraversal.class);
        rag.setLoadedTraversals(loadedTraversals);
        
    	final UriInfo uri = this.mockery.mock(UriInfo.class);
    	final HttpServletRequest httpServletRequest = this.mockery.mock(HttpServletRequest.class);
    	final List<PathSegment> pathSegments = new ArrayList<PathSegment>();
    	
    	// the first two path segments are ignored by the mock as a traversal
    	// must have at least that much of a path to work properly.
    	final PathSegment pathSegment = this.mockery.mock(PathSegment.class);
    	pathSegments.add(pathSegment);
    	pathSegments.add(pathSegment);
    	pathSegments.add(pathSegment);
    	
    	this.mockery.checking(new Expectations() {{
    		allowing(uri).getPathSegments();
    		will(returnValue(pathSegments));
    		allowing(pathSegment).getPath();
    		will(returnValue("some-traversal"));
    		allowing(httpServletRequest).getParameterMap();
            will(returnValue(new HashMap<String, String>()));
            allowing(rap).getApplicationGraph(with(any(String.class)));
            will(returnValue(rag));
        }});
    	
    	TraversalResource resource = new TraversalResource("graph", uri, httpServletRequest, rap);
    	resource.getTraversal();
    	
    }
    
    @Test(expected = WebApplicationException.class)
    public void getTraversalAndThrowsTraversalException(){
    	final Graph graph = this.mockery.mock(Graph.class);
    	final RexsterApplicationGraph rag = new RexsterApplicationGraph("graph", graph);
        final RexsterApplicationProvider rap = this.mockery.mock(RexsterApplicationProvider.class);
        
        Map<String, Class<? extends Traversal>> loadedTraversals = new HashMap<String, Class<? extends Traversal>>();
        loadedTraversals.put("some-traversal", MockTraversal.MockEvilTraversal.class);
        rag.setLoadedTraversals(loadedTraversals);
        
    	final UriInfo uri = this.mockery.mock(UriInfo.class);
    	final HttpServletRequest httpServletRequest = this.mockery.mock(HttpServletRequest.class);
    	final List<PathSegment> pathSegments = new ArrayList<PathSegment>();
    	
    	// the first two path segments are ignored by the mock as a traversal
    	// must have at least that much of a path to work properly.
    	final PathSegment pathSegment = this.mockery.mock(PathSegment.class);
    	pathSegments.add(pathSegment);
    	pathSegments.add(pathSegment);
    	pathSegments.add(pathSegment);
    	
    	this.mockery.checking(new Expectations() {{
    		allowing(uri).getPathSegments();
    		will(returnValue(pathSegments));
    		allowing(pathSegment).getPath();
    		will(returnValue("some-traversal"));
    		allowing(httpServletRequest).getParameterMap();
            will(returnValue(new HashMap<String, String>()));
            allowing(rap).getApplicationGraph(with(any(String.class)));
            will(returnValue(rag));
        }});
    	
    	TraversalResource resource = new TraversalResource("graph", uri, httpServletRequest, rap);
    	resource.getTraversal();
    	
    }
    @Test
    public void getTraversalValid(){
    	final Graph graph = this.mockery.mock(Graph.class);
    	final RexsterApplicationGraph rag = new RexsterApplicationGraph("graph", graph);
        final RexsterApplicationProvider rap = this.mockery.mock(RexsterApplicationProvider.class);
        
        Map<String, Class<? extends Traversal>> loadedTraversals = new HashMap<String, Class<? extends Traversal>>();
        loadedTraversals.put("some-traversal", MockTraversal.class);
        rag.setLoadedTraversals(loadedTraversals);
        
    	final UriInfo uri = this.mockery.mock(UriInfo.class);
    	final HttpServletRequest httpServletRequest = this.mockery.mock(HttpServletRequest.class);
    	final List<PathSegment> pathSegments = new ArrayList<PathSegment>();
    	final PathSegment pathSegment = this.mockery.mock(PathSegment.class);
    	pathSegments.add(pathSegment);
    	
    	final ResultObjectCache resultObjectCache = this.mockery.mock(ResultObjectCache.class);
    	
    	this.mockery.checking(new Expectations() {{
    		allowing(uri).getPathSegments();
    		will(returnValue(pathSegments));
    		allowing(pathSegment).getPath();
    		will(returnValue("some-traversal"));
    		allowing(httpServletRequest).getParameterMap();
            will(returnValue(new HashMap<String, String>()));
            allowing(rap).getApplicationGraph(with(any(String.class)));
            will(returnValue(rag));
            allowing(rap).getResultObjectCache();
            will(returnValue(resultObjectCache));
        }});
    	
    	TraversalResource resource = new TraversalResource("graph", uri, httpServletRequest, rap);
    	Response response = resource.getTraversals();
    	
    	Assert.assertNotNull(response);
    	Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
    	Assert.assertNotNull(response.getEntity());
    	Assert.assertTrue(response.getEntity() instanceof JSONObject);
    	
    	JSONObject json = (JSONObject) response.getEntity();
    	Assert.assertTrue(json.has(Tokens.QUERY_TIME));
    	Assert.assertTrue(json.optDouble(Tokens.QUERY_TIME) > 0);
    }
}
