package com.tinkerpop.rexster;

import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.KeyIndexableGraph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.rexster.server.RexsterApplication;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.jmock.Expectations;
import org.junit.Assert;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.HashMap;

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class KeyIndexResourceTest extends BaseTest {

    @Test(expected = WebApplicationException.class)
    public void getKeyIndexNonIndexableGraph() {
        final KeyIndexResource resource = mockNonKeyIndexableGraph();
        resource.getKeyIndices(graphName);
    }

    @Test
    public void getKeyIndices() {
        final KeyIndexResource resource = constructKeyIndexResourceWithToyGraph().getResource();
        final Response response = resource.getKeyIndices(graphName);

        Assert.assertNotNull(response);
        Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        Assert.assertNotNull(response.getEntity());
        Assert.assertTrue(response.getEntity() instanceof JSONObject);

        final JSONObject json = (JSONObject) response.getEntity();
        Assert.assertTrue(json.has(Tokens.QUERY_TIME));
        Assert.assertTrue(json.has(Tokens.RESULTS));

        final JSONObject jsonKeys = json.optJSONObject(Tokens.RESULTS);
        Assert.assertTrue(jsonKeys.has(Tokens.VERTEX));
        Assert.assertTrue(jsonKeys.has(Tokens.EDGE));

        final JSONArray vertexKeys = jsonKeys.optJSONArray(Tokens.VERTEX);
        Assert.assertEquals(2, vertexKeys.length());

        final JSONArray edgeKeys = jsonKeys.optJSONArray(Tokens.EDGE);
        Assert.assertEquals(1, edgeKeys.length());
        Assert.assertEquals("weight", edgeKeys.optString(0));
    }

    @Test(expected = WebApplicationException.class)
    public void getIndexKeysNonIndexableGraph() {
        final KeyIndexResource resource = mockNonKeyIndexableGraph();
        resource.getIndexKeys("graph", Tokens.VERTEX);
    }

    @Test
    public void getIndexKeysVertex() {
        final KeyIndexResource resource = constructKeyIndexResourceWithToyGraph().getResource();
        final Response response = resource.getIndexKeys(graphName, Tokens.VERTEX);

        Assert.assertNotNull(response);
        Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        Assert.assertNotNull(response.getEntity());
        Assert.assertTrue(response.getEntity() instanceof JSONObject);

        final JSONObject json = (JSONObject) response.getEntity();
        Assert.assertTrue(json.has(Tokens.QUERY_TIME));
        Assert.assertTrue(json.has(Tokens.RESULTS));

        final JSONArray jsonKeys = json.optJSONArray(Tokens.RESULTS);
        Assert.assertEquals(2, jsonKeys.length());

    }

    @Test
    public void getIndexKeysEdge() {
        final KeyIndexResource resource = constructKeyIndexResourceWithToyGraph().getResource();
        final Response response = resource.getIndexKeys(graphName, Tokens.EDGE);

        Assert.assertNotNull(response);
        Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        Assert.assertNotNull(response.getEntity());
        Assert.assertTrue(response.getEntity() instanceof JSONObject);

        final JSONObject json = (JSONObject) response.getEntity();
        Assert.assertTrue(json.has(Tokens.QUERY_TIME));
        Assert.assertTrue(json.has(Tokens.RESULTS));

        final JSONArray jsonKeys = json.optJSONArray(Tokens.RESULTS);
        Assert.assertEquals(1, jsonKeys.length());
    }

    @Test(expected = WebApplicationException.class)
    public void deleteIndexKeyNonIndexableGraph() {
        final KeyIndexResource resource = mockNonKeyIndexableGraph();
        resource.deleteIndexKey(graphName, Tokens.VERTEX, "key");
    }

    @Test
    public void deleteIndexKeyVertex() {
        final KeyIndexResource resource = constructKeyIndexResourceWithToyGraph().getResource();
        final Response response = resource.deleteIndexKey(graphName, Tokens.VERTEX, "test");

        Assert.assertNotNull(response);
        Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        Assert.assertNotNull(response.getEntity());
        Assert.assertTrue(response.getEntity() instanceof JSONObject);

        final JSONObject json = (JSONObject) response.getEntity();
        Assert.assertTrue(json.has(Tokens.QUERY_TIME));

        final KeyIndexableGraph g = (KeyIndexableGraph) this.toyGraph;
        Assert.assertFalse(g.getIndexedKeys(Vertex.class).contains("test"));
    }

    @Test
    public void deleteIndexKeyEdge() {
        final KeyIndexResource resource = constructKeyIndexResourceWithToyGraph().getResource();
        final Response response = resource.deleteIndexKey(graphName, Tokens.EDGE, "weight");

        Assert.assertNotNull(response);
        Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        Assert.assertNotNull(response.getEntity());
        Assert.assertTrue(response.getEntity() instanceof JSONObject);

        final JSONObject json = (JSONObject) response.getEntity();
        Assert.assertTrue(json.has(Tokens.QUERY_TIME));

        final KeyIndexableGraph g = (KeyIndexableGraph) this.toyGraph;
        Assert.assertFalse(g.getIndexedKeys(Edge.class).contains("weight"));
    }

    @Test(expected = WebApplicationException.class)
    public void postIndexKeyNonIndexableGraph() {
        final KeyIndexResource resource = mockNonKeyIndexableGraph();
        resource.postIndexKey(graphName, Tokens.VERTEX, "key");
    }

    @Test
    public void postIndexKeyVertex() {
        final KeyIndexResource resource = constructKeyIndexResourceWithToyGraph().getResource();
        final Response response = resource.postIndexKey(graphName, Tokens.VERTEX, "lang");

        Assert.assertNotNull(response);
        Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        Assert.assertNotNull(response.getEntity());
        Assert.assertTrue(response.getEntity() instanceof JSONObject);

        final JSONObject json = (JSONObject) response.getEntity();
        Assert.assertTrue(json.has(Tokens.QUERY_TIME));

        final KeyIndexableGraph g = (KeyIndexableGraph) this.toyGraph;
        Assert.assertEquals(3, g.getIndexedKeys(Vertex.class).size());
        Assert.assertTrue(g.getIndexedKeys(Vertex.class).contains("lang"));
    }

    @Test
    public void postIndexKeyEdge() {
        final KeyIndexResource resource = constructKeyIndexResourceWithToyGraph().getResource();
        final Response response = resource.postIndexKey(graphName, Tokens.EDGE, "other");

        Assert.assertNotNull(response);
        Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        Assert.assertNotNull(response.getEntity());
        Assert.assertTrue(response.getEntity() instanceof JSONObject);

        final JSONObject json = (JSONObject) response.getEntity();
        Assert.assertTrue(json.has(Tokens.QUERY_TIME));

        final KeyIndexableGraph g = (KeyIndexableGraph) this.toyGraph;
        Assert.assertEquals(2, g.getIndexedKeys(Edge.class).size());
        Assert.assertTrue(g.getIndexedKeys(Edge.class).contains("other"));
    }

    private KeyIndexResource mockNonKeyIndexableGraph() {
        final Graph graph = this.mockery.mock(Graph.class);
        final RexsterApplicationGraph rag = new RexsterApplicationGraph(graphName, graph);
        final RexsterApplication ra = this.mockery.mock(RexsterApplication.class);

        final UriInfo uri = this.mockery.mock(UriInfo.class);

        final HttpServletRequest httpServletRequest = this.mockery.mock(HttpServletRequest.class);

        this.mockery.checking(new Expectations() {{
            allowing(httpServletRequest).getParameterMap();
            will(returnValue(new HashMap<String, String>()));
            allowing(ra).getApplicationGraph(with(any(String.class)));
            will(returnValue(rag));
        }});

        return new KeyIndexResource(uri, httpServletRequest, ra);
    }
}
