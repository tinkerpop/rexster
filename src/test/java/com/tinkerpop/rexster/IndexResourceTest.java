package com.tinkerpop.rexster;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.tinkerpop.blueprints.pgm.Graph;
import com.tinkerpop.blueprints.pgm.Index;
import com.tinkerpop.blueprints.pgm.IndexableGraph;
import com.tinkerpop.blueprints.pgm.Index.Type;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.Status;

public class IndexResourceTest {
	protected Mockery mockery = new JUnit4Mockery();
    protected final String baseUri = "http://localhost/mock";

    @Before
    public void init() {
        this.mockery = new JUnit4Mockery();
    }
    
    @Test(expected = WebApplicationException.class)
    public void getAllIndicesNonIndexableGraph() {
    	final Graph graph = this.mockery.mock(Graph.class);
    	final RexsterApplicationGraph rag = new RexsterApplicationGraph("graph", graph);
        final RexsterApplicationProvider rap = this.mockery.mock(RexsterApplicationProvider.class);
    	
    	final UriInfo uri = this.mockery.mock(UriInfo.class);
    	
    	final HttpServletRequest httpServletRequest = this.mockery.mock(HttpServletRequest.class);
    	
    	this.mockery.checking(new Expectations() {{
    		allowing(httpServletRequest).getParameterMap();
            will(returnValue(new HashMap<String, String>()));
            allowing(rap).getApplicationGraph(with(any(String.class)));
            will(returnValue(rag));
        }});
    	
    	IndexResource resource = new IndexResource("graph", uri, httpServletRequest, rap);
    	resource.getAllIndices();
    }
    
    @Test
    public void getAllIndicesNoOffset() {
    	IndexResource resource = constructMockGetAllIndicesScenario(100, new HashMap<String, String>());
    	Response response = resource.getAllIndices();
    	
    	this.assertIndexOkResponseJsonStructure(100, 100, response);
    }
    
    @Test
    public void getAllIndicesWithValidOffset() {

    	HashMap<String, String> parameters = new HashMap<String, String>();
    	parameters.put(Tokens.REXSTER + "." + Tokens.OFFSET_START, "10");
    	parameters.put(Tokens.REXSTER + "." + Tokens.OFFSET_END, "20");
    	
    	IndexResource resource = constructMockGetAllIndicesScenario(100, parameters);
    	Response response = resource.getAllIndices();
    	
    	this.assertIndexOkResponseJsonStructure(10, 100, response);
    }
    
    @Test
    public void getAllIndicesWithOffsetNotEnoughResults() {
    	HashMap<String, String> parameters = new HashMap<String, String>();
    	parameters.put(Tokens.REXSTER + "." + Tokens.OFFSET_START, "10");
    	parameters.put(Tokens.REXSTER + "." + Tokens.OFFSET_END, "20");
    	IndexResource resource = this.constructMockGetAllIndicesScenario(5, parameters);
    	
    	Response response = resource.getAllIndices();
    	this.assertIndexOkResponseJsonStructure(0, 5, response);
    	
    }
    
    @Test
    public void getAllIndicesWithOffsetStartAfterEnd() {
    	HashMap<String, String> parameters = new HashMap<String, String>();
    	parameters.put(Tokens.REXSTER + "." + Tokens.OFFSET_START, "30");
    	parameters.put(Tokens.REXSTER + "." + Tokens.OFFSET_END, "20");
    	IndexResource resource = this.constructMockGetAllIndicesScenario(100, parameters);
    	
    	Response response = resource.getAllIndices();
    	this.assertIndexOkResponseJsonStructure(0, 100, response);
    	
    }

	private IndexResource constructMockGetAllIndicesScenario(int numberOfIndicesToGenerate, final HashMap<String, String> parameters) {
		final IndexableGraph graph = this.mockery.mock(IndexableGraph.class);
    	final RexsterApplicationGraph rag = new RexsterApplicationGraph("graph", graph);
        final RexsterApplicationProvider rap = this.mockery.mock(RexsterApplicationProvider.class);
    	
    	final UriInfo uri = this.mockery.mock(UriInfo.class);
    	final HttpServletRequest httpServletRequest = this.mockery.mock(HttpServletRequest.class);
    	final ArrayList<Index> indices = new ArrayList<Index>();
    	
    	for (int ix = 0; ix < numberOfIndicesToGenerate; ix++) {
    		indices.add(new MockIndex("index-name-" + new Integer(ix).toString(), Type.MANUAL, String.class));
    	}
    	
    	this.mockery.checking(new Expectations() {{
    		allowing(httpServletRequest).getParameterMap();
            will(returnValue(parameters));
            allowing(rap).getApplicationGraph(with(any(String.class)));
            will(returnValue(rag));
            allowing(graph).getIndices();
            will(returnValue(indices));
        }});
    	
    	IndexResource resource = new IndexResource("graph", uri, httpServletRequest, rap);
		return resource;
	}
    
    private JSONObject assertIndexOkResponseJsonStructure(int numberOfIndicesReturned, 
    		int numberOfIndicesTotal, Response response) {
    	Assert.assertNotNull(response);
    	Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
    	Assert.assertNotNull(response.getEntity());
    	Assert.assertTrue(response.getEntity() instanceof JSONObject);
    	
    	JSONObject json = (JSONObject) response.getEntity();
    	Assert.assertTrue(json.has(Tokens.TOTAL_SIZE));
    	Assert.assertEquals(numberOfIndicesTotal, json.optInt(Tokens.TOTAL_SIZE));
    	Assert.assertTrue(json.has(Tokens.QUERY_TIME));
    	Assert.assertTrue(json.optDouble(Tokens.QUERY_TIME) > 0);
    	
    	Assert.assertTrue(json.has(Tokens.RESULTS));
    	Assert.assertFalse(json.isNull(Tokens.RESULTS));
    	
    	JSONArray jsonResults = json.optJSONArray(Tokens.RESULTS);
    	Assert.assertEquals(numberOfIndicesReturned, jsonResults.length());
    	
    	return json;
    }
}