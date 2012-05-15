package com.tinkerpop.rexster;

import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.KeyIndexableGraph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.tg.TinkerGraphFactory;
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
import javax.ws.rs.core.UriInfo;
import java.util.HashMap;

public class KeyIndexResourceTest {
    protected Mockery mockery = new JUnit4Mockery();
    protected final String baseUri = "http://localhost/mock";

    @Before
    public void init() {
        this.mockery = new JUnit4Mockery();
    }

    @Test(expected = WebApplicationException.class)
    public void getKeyIndexNonIndexableGraph() {
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

        KeyIndexResource resource = new KeyIndexResource(uri, httpServletRequest, ra);
        resource.getKeyIndices("graph");
    }
    
    @Test
    public void getKeyIndices() {
        final KeyIndexableGraph g = createKeyIndexableGraph();
        
        final RexsterApplicationGraph rag = new RexsterApplicationGraph("graph", g);
        final RexsterApplication ra = this.mockery.mock(RexsterApplication.class);
        final UriInfo uri = this.mockery.mock(UriInfo.class);

        final HttpServletRequest httpServletRequest = this.mockery.mock(HttpServletRequest.class);

        this.mockery.checking(new Expectations() {{
            allowing(httpServletRequest).getParameterMap();
            will(returnValue(new HashMap<String, String>()));
            allowing(ra).getApplicationGraph(with(any(String.class)));
            will(returnValue(rag));
        }});

        final KeyIndexResource resource = new KeyIndexResource(uri, httpServletRequest, ra);
        final Response response = resource.getKeyIndices("graph");

        Assert.assertNotNull(response);
        Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        Assert.assertNotNull(response.getEntity());
        Assert.assertTrue(response.getEntity() instanceof JSONObject);

        JSONObject json = (JSONObject) response.getEntity();
        Assert.assertTrue(json.has(Tokens.QUERY_TIME));
        Assert.assertTrue(json.has(Tokens.KEYS));
        
        JSONObject jsonKeys = json.optJSONObject(Tokens.KEYS);
        Assert.assertTrue(jsonKeys.has(Tokens.VERTEX));
        Assert.assertTrue(jsonKeys.has(Tokens.EDGE));
        
        JSONArray vertexKeys = jsonKeys.optJSONArray(Tokens.VERTEX);
        Assert.assertEquals(2, vertexKeys.length());
        
        JSONArray edgeKeys = jsonKeys.optJSONArray(Tokens.EDGE);
        Assert.assertEquals(1, edgeKeys.length());
        Assert.assertEquals("weight", edgeKeys.optString(0));
    }

    @Test(expected = WebApplicationException.class)
    public void getIndexKeysNonIndexableGraph() {
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

        KeyIndexResource resource = new KeyIndexResource(uri, httpServletRequest, ra);
        resource.getIndexKeys("graph", Tokens.VERTEX);
    }

    @Test
    public void getIndexKeysVertex() {
        final KeyIndexableGraph g = createKeyIndexableGraph();

        final RexsterApplicationGraph rag = new RexsterApplicationGraph("graph", g);
        final RexsterApplication ra = this.mockery.mock(RexsterApplication.class);
        final UriInfo uri = this.mockery.mock(UriInfo.class);

        final HttpServletRequest httpServletRequest = this.mockery.mock(HttpServletRequest.class);

        this.mockery.checking(new Expectations() {{
            allowing(httpServletRequest).getParameterMap();
            will(returnValue(new HashMap<String, String>()));
            allowing(ra).getApplicationGraph(with(any(String.class)));
            will(returnValue(rag));
        }});

        final KeyIndexResource resource = new KeyIndexResource(uri, httpServletRequest, ra);
        final Response response = resource.getIndexKeys("graph", Tokens.VERTEX);

        Assert.assertNotNull(response);
        Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        Assert.assertNotNull(response.getEntity());
        Assert.assertTrue(response.getEntity() instanceof JSONObject);

        JSONObject json = (JSONObject) response.getEntity();
        Assert.assertTrue(json.has(Tokens.QUERY_TIME));
        Assert.assertTrue(json.has(Tokens.RESULTS));

        JSONArray jsonKeys = json.optJSONArray(Tokens.RESULTS);
        Assert.assertEquals(2, jsonKeys.length());

    }

    @Test
    public void getIndexKeysEdge() {
        final KeyIndexableGraph g = createKeyIndexableGraph();

        final RexsterApplicationGraph rag = new RexsterApplicationGraph("graph", g);
        final RexsterApplication ra = this.mockery.mock(RexsterApplication.class);
        final UriInfo uri = this.mockery.mock(UriInfo.class);

        final HttpServletRequest httpServletRequest = this.mockery.mock(HttpServletRequest.class);

        this.mockery.checking(new Expectations() {{
            allowing(httpServletRequest).getParameterMap();
            will(returnValue(new HashMap<String, String>()));
            allowing(ra).getApplicationGraph(with(any(String.class)));
            will(returnValue(rag));
        }});

        final KeyIndexResource resource = new KeyIndexResource(uri, httpServletRequest, ra);
        final Response response = resource.getIndexKeys("graph", Tokens.EDGE);

        Assert.assertNotNull(response);
        Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        Assert.assertNotNull(response.getEntity());
        Assert.assertTrue(response.getEntity() instanceof JSONObject);

        JSONObject json = (JSONObject) response.getEntity();
        Assert.assertTrue(json.has(Tokens.QUERY_TIME));
        Assert.assertTrue(json.has(Tokens.RESULTS));

        JSONArray jsonKeys = json.optJSONArray(Tokens.RESULTS);
        Assert.assertEquals(1, jsonKeys.length());
    }

    @Test(expected = WebApplicationException.class)
    public void deleteIndexKeyNonIndexableGraph() {
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

        KeyIndexResource resource = new KeyIndexResource(uri, httpServletRequest, ra);
        resource.deleteIndexKey("graph", Tokens.VERTEX, "key");
    }

    @Test
    public void deleteIndexKeyVertex() {
        final KeyIndexableGraph g = createKeyIndexableGraph();

        final RexsterApplicationGraph rag = new RexsterApplicationGraph("graph", g);
        final RexsterApplication ra = this.mockery.mock(RexsterApplication.class);
        final UriInfo uri = this.mockery.mock(UriInfo.class);

        final HttpServletRequest httpServletRequest = this.mockery.mock(HttpServletRequest.class);

        this.mockery.checking(new Expectations() {{
            allowing(httpServletRequest).getParameterMap();
            will(returnValue(new HashMap<String, String>()));
            allowing(ra).getApplicationGraph(with(any(String.class)));
            will(returnValue(rag));
        }});

        final KeyIndexResource resource = new KeyIndexResource(uri, httpServletRequest, ra);
        final Response response = resource.deleteIndexKey("graph", Tokens.VERTEX, "test");

        Assert.assertNotNull(response);
        Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        Assert.assertNotNull(response.getEntity());
        Assert.assertTrue(response.getEntity() instanceof JSONObject);

        JSONObject json = (JSONObject) response.getEntity();
        Assert.assertTrue(json.has(Tokens.QUERY_TIME));
        
        Assert.assertFalse(g.getIndexedKeys(Vertex.class).contains("test"));
    }

    @Test
    public void deleteIndexKeyEdge() {
        final KeyIndexableGraph g = createKeyIndexableGraph();

        final RexsterApplicationGraph rag = new RexsterApplicationGraph("graph", g);
        final RexsterApplication ra = this.mockery.mock(RexsterApplication.class);
        final UriInfo uri = this.mockery.mock(UriInfo.class);

        final HttpServletRequest httpServletRequest = this.mockery.mock(HttpServletRequest.class);

        this.mockery.checking(new Expectations() {{
            allowing(httpServletRequest).getParameterMap();
            will(returnValue(new HashMap<String, String>()));
            allowing(ra).getApplicationGraph(with(any(String.class)));
            will(returnValue(rag));
        }});

        final KeyIndexResource resource = new KeyIndexResource(uri, httpServletRequest, ra);
        final Response response = resource.deleteIndexKey("graph", Tokens.EDGE, "weight");

        Assert.assertNotNull(response);
        Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        Assert.assertNotNull(response.getEntity());
        Assert.assertTrue(response.getEntity() instanceof JSONObject);

        JSONObject json = (JSONObject) response.getEntity();
        Assert.assertTrue(json.has(Tokens.QUERY_TIME));

        Assert.assertFalse(g.getIndexedKeys(Edge.class).contains("weight"));
    }

    @Test(expected = WebApplicationException.class)
    public void postIndexKeyNonIndexableGraph() {
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

        KeyIndexResource resource = new KeyIndexResource(uri, httpServletRequest, ra);
        resource.postIndexKey("graph", Tokens.VERTEX, "key");
    }

    @Test
    public void postIndexKeyVertex() {
        final KeyIndexableGraph g = createKeyIndexableGraph();

        final RexsterApplicationGraph rag = new RexsterApplicationGraph("graph", g);
        final RexsterApplication ra = this.mockery.mock(RexsterApplication.class);
        final UriInfo uri = this.mockery.mock(UriInfo.class);

        final HttpServletRequest httpServletRequest = this.mockery.mock(HttpServletRequest.class);

        this.mockery.checking(new Expectations() {{
            allowing(httpServletRequest).getParameterMap();
            will(returnValue(new HashMap<String, String>()));
            allowing(ra).getApplicationGraph(with(any(String.class)));
            will(returnValue(rag));
        }});

        final KeyIndexResource resource = new KeyIndexResource(uri, httpServletRequest, ra);
        final Response response = resource.postIndexKey("graph", Tokens.VERTEX, "lang");

        Assert.assertNotNull(response);
        Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        Assert.assertNotNull(response.getEntity());
        Assert.assertTrue(response.getEntity() instanceof JSONObject);

        JSONObject json = (JSONObject) response.getEntity();
        Assert.assertTrue(json.has(Tokens.QUERY_TIME));

        Assert.assertEquals(3, g.getIndexedKeys(Vertex.class).size());
        Assert.assertTrue(g.getIndexedKeys(Vertex.class).contains("lang"));
    }


    @Test
    public void postIndexKeyEdge() {
        final KeyIndexableGraph g = createKeyIndexableGraph();

        final RexsterApplicationGraph rag = new RexsterApplicationGraph("graph", g);
        final RexsterApplication ra = this.mockery.mock(RexsterApplication.class);
        final UriInfo uri = this.mockery.mock(UriInfo.class);

        final HttpServletRequest httpServletRequest = this.mockery.mock(HttpServletRequest.class);

        this.mockery.checking(new Expectations() {{
            allowing(httpServletRequest).getParameterMap();
            will(returnValue(new HashMap<String, String>()));
            allowing(ra).getApplicationGraph(with(any(String.class)));
            will(returnValue(rag));
        }});

        final KeyIndexResource resource = new KeyIndexResource(uri, httpServletRequest, ra);
        final Response response = resource.postIndexKey("graph", Tokens.EDGE, "other");

        Assert.assertNotNull(response);
        Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        Assert.assertNotNull(response.getEntity());
        Assert.assertTrue(response.getEntity() instanceof JSONObject);

        JSONObject json = (JSONObject) response.getEntity();
        Assert.assertTrue(json.has(Tokens.QUERY_TIME));

        Assert.assertEquals(2, g.getIndexedKeys(Edge.class).size());
        Assert.assertTrue(g.getIndexedKeys(Edge.class).contains("other"));
    }

    private static KeyIndexableGraph createKeyIndexableGraph() {
        final KeyIndexableGraph g = TinkerGraphFactory.createTinkerGraph();
        g.createKeyIndex("name", Vertex.class);
        g.createKeyIndex("test", Vertex.class);
        g.createKeyIndex("weight", Edge.class);
        return g;
    }
}
