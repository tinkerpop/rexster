package com.tinkerpop.rexster;

import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Index;
import com.tinkerpop.blueprints.IndexableGraph;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.HashMap;

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
        final RexsterApplication ra = this.mockery.mock(RexsterApplication.class);

        final UriInfo uri = this.mockery.mock(UriInfo.class);

        final HttpServletRequest httpServletRequest = this.mockery.mock(HttpServletRequest.class);

        this.mockery.checking(new Expectations() {{
            allowing(httpServletRequest).getParameterMap();
            will(returnValue(new HashMap<String, String>()));
            allowing(ra).getApplicationGraph(with(any(String.class)));
            will(returnValue(rag));
        }});

        IndexResource resource = new IndexResource(uri, httpServletRequest, ra);
        resource.getAllIndices("graph");
    }

    @Test
    public void getAllIndicesNoOffset() {
        IndexResource resource = constructMockGetAllIndicesScenario(100, new HashMap<String, String>());
        Response response = resource.getAllIndices("graph");

        this.assertIndexOkResponseJsonStructure(100, 100, response);
    }

    @Test
    public void getAllIndicesWithValidOffset() {

        HashMap<String, String> parameters = new HashMap<String, String>();
        parameters.put(Tokens.REXSTER + "." + Tokens.OFFSET_START, "10");
        parameters.put(Tokens.REXSTER + "." + Tokens.OFFSET_END, "20");

        IndexResource resource = constructMockGetAllIndicesScenario(100, parameters);
        Response response = resource.getAllIndices("graph");

        this.assertIndexOkResponseJsonStructure(10, 100, response);
    }

    @Test
    public void getAllIndicesWithOffsetNotEnoughResults() {
        HashMap<String, String> parameters = new HashMap<String, String>();
        parameters.put(Tokens.REXSTER + "." + Tokens.OFFSET_START, "10");
        parameters.put(Tokens.REXSTER + "." + Tokens.OFFSET_END, "20");
        IndexResource resource = this.constructMockGetAllIndicesScenario(5, parameters);

        Response response = resource.getAllIndices("graph");
        this.assertIndexOkResponseJsonStructure(0, 5, response);

    }

    @Test
    public void getAllIndicesWithOffsetStartAfterEnd() {
        HashMap<String, String> parameters = new HashMap<String, String>();
        parameters.put(Tokens.REXSTER + "." + Tokens.OFFSET_START, "30");
        parameters.put(Tokens.REXSTER + "." + Tokens.OFFSET_END, "20");
        IndexResource resource = this.constructMockGetAllIndicesScenario(100, parameters);

        Response response = resource.getAllIndices("graph");
        this.assertIndexOkResponseJsonStructure(0, 100, response);

    }

    @Test(expected = WebApplicationException.class)
    public void getIndexCountIndexNotFound() {
        HashMap<String, String> parameters = new HashMap<String, String>();
        IndexResource resource = this.constructMockGetAllIndicesScenario(1, parameters);

        // only one graph exists so index-name-2 is junk
        Response response = resource.getIndexCount("graph", "index-name-2");
    }

    @Test(expected = WebApplicationException.class)
    public void getIndexCountIndexBadRequestNoKey() {
        HashMap<String, String> parameters = new HashMap<String, String>();
        parameters.put("value", "name");
        IndexResource resource = this.constructMockGetAllIndicesScenario(1, parameters);

        Response response = resource.getIndexCount("graph", "index-name-0");
    }

    @Test(expected = WebApplicationException.class)
    public void getIndexCountIndexBadRequestNoValue() {
        HashMap<String, String> parameters = new HashMap<String, String>();
        parameters.put("key", "name");
        IndexResource resource = this.constructMockGetAllIndicesScenario(1, parameters);

        Response response = resource.getIndexCount("graph", "index-name-0");
    }

    @Test
    public void getIndexCountIndexValid() {
        HashMap<String, String> parameters = new HashMap<String, String>();
        parameters.put("key", "name");
        parameters.put("value", "marko");
        IndexResource resource = this.constructMockGetAllIndicesScenario(1, parameters);

        Response response = resource.getIndexCount("graph", "index-name-0");

        Assert.assertNotNull(response);
        Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assert.assertNotNull(response.getEntity());
        Assert.assertTrue(response.getEntity() instanceof JSONObject);

        JSONObject json = (JSONObject) response.getEntity();
        Assert.assertTrue(json.has(Tokens.TOTAL_SIZE));
        Assert.assertEquals(100, json.optInt(Tokens.TOTAL_SIZE));
        Assert.assertTrue(json.has(Tokens.QUERY_TIME));
    }

    private IndexResource constructMockGetAllIndicesScenario(int numberOfIndicesToGenerate, final HashMap<String, String> parameters) {
        final IndexableGraph graph = this.mockery.mock(IndexableGraph.class);
        final RexsterApplicationGraph rag = new RexsterApplicationGraph("graph", graph);
        final RexsterApplication ra = this.mockery.mock(RexsterApplication.class);

        final UriInfo uri = this.mockery.mock(UriInfo.class);
        final HttpServletRequest httpServletRequest = this.mockery.mock(HttpServletRequest.class);
        final ArrayList<Index> indices = new ArrayList<Index>();

        for (int ix = 0; ix < numberOfIndicesToGenerate; ix++) {
            indices.add(new MockIndex("index-name-" + new Integer(ix).toString(), String.class, 100l));
        }

        this.mockery.checking(new Expectations() {{
            allowing(httpServletRequest).getParameterMap();
            will(returnValue(parameters));
            allowing(ra).getApplicationGraph(with(any(String.class)));
            will(returnValue(rag));
            allowing(graph).getIndices();
            will(returnValue(indices));
        }});

        IndexResource resource = new IndexResource(uri, httpServletRequest, ra);
        return resource;
    }

    private JSONObject assertIndexOkResponseJsonStructure(int numberOfIndicesReturned, int numberOfIndicesTotal, Response response) {
        Assert.assertNotNull(response);
        Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assert.assertNotNull(response.getEntity());
        Assert.assertTrue(response.getEntity() instanceof JSONObject);

        JSONObject json = (JSONObject) response.getEntity();
        Assert.assertTrue(json.has(Tokens.TOTAL_SIZE));
        Assert.assertEquals(numberOfIndicesTotal, json.optInt(Tokens.TOTAL_SIZE));
        Assert.assertTrue(json.has(Tokens.QUERY_TIME));

        Assert.assertTrue(json.has(Tokens.RESULTS));
        Assert.assertFalse(json.isNull(Tokens.RESULTS));

        JSONArray jsonResults = json.optJSONArray(Tokens.RESULTS);
        Assert.assertEquals(numberOfIndicesReturned, jsonResults.length());

        return json;
    }
}