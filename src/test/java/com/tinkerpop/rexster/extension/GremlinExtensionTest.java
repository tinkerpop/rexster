package com.tinkerpop.rexster.extension;

import com.tinkerpop.blueprints.pgm.Graph;
import com.tinkerpop.blueprints.pgm.Vertex;
import com.tinkerpop.blueprints.pgm.impls.tg.TinkerGraph;
import com.tinkerpop.rexster.RexsterResourceContext;
import com.tinkerpop.rexster.Tokens;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.HashMap;

public class GremlinExtensionTest {

    private Mockery mockery = new JUnit4Mockery();
    private GremlinExtension gremlinExtension;

    private static Graph graph;

    private HttpServletRequest httpServletRequest = this.mockery.mock(HttpServletRequest.class);
    private UriInfo uriInfo = this.mockery.mock(UriInfo.class);
    private URI uri = URI.create("http://localhost:8182");

    /**
     * a null constructed extension method creates no API documentation on error
     */
    private ExtensionMethod extensionMethodNoApi = new ExtensionMethod(null, null, null);

    private RexsterResourceContext rexsterResourceContext = new RexsterResourceContext(null, uriInfo,
                httpServletRequest, null, extensionMethodNoApi);

    /**
     * Choosing not to mock Graph instance for these tests as GremlinScriptEngine is
     * embedded into the GremlinExtension.
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        graph = new TinkerGraph();
        Vertex marko = graph.addVertex("1");
        marko.setProperty("name", "marko");
        marko.setProperty("age", 29);

        Vertex vadas = graph.addVertex("2");
        vadas.setProperty("name", "vadas");
        vadas.setProperty("age", 27);

        Vertex lop = graph.addVertex("3");
        lop.setProperty("name", "lop");
        lop.setProperty("lang", "java");

        Vertex josh = graph.addVertex("4");
        josh.setProperty("name", "josh");
        josh.setProperty("age", 32);

        Vertex ripple = graph.addVertex("5");
        ripple.setProperty("name", "ripple");
        ripple.setProperty("lang", "java");

        Vertex peter = graph.addVertex("6");
        peter.setProperty("name", "peter");
        peter.setProperty("age", 35);

        graph.addEdge("7", marko, vadas, "knows").setProperty("weight", 0.5f);
        graph.addEdge("8", marko, josh, "knows").setProperty("weight", 1.0f);
        graph.addEdge("9", marko, lop, "created").setProperty("weight", 0.4f);

        graph.addEdge("10", josh, ripple, "created").setProperty("weight", 1.0f);
        graph.addEdge("11", josh, lop, "created").setProperty("weight", 0.4f);

        graph.addEdge("12", peter, lop, "created").setProperty("weight", 0.2f);
    }

    @Before
    public void beforeEachTest() {
        this.gremlinExtension = new GremlinExtension();
    }

    @Test
    public void evaluateOnGraphNoScript() {

        ExtensionResponse extensionResponse = this.gremlinExtension.evaluateOnGraph(
                rexsterResourceContext, graph, false, "", null);

        JSONObject jsonResponse = assertResponseAndGetEntity(extensionResponse,
                true,
                Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());

        Assert.assertNotNull(jsonResponse);
        Assert.assertTrue(jsonResponse.has(Tokens.MESSAGE));
        Assert.assertEquals("no script provided", jsonResponse.optString(Tokens.MESSAGE));
    }

    @Test
    public void evaluateOnGraphNoKeysNoTypesReturnVertex() {
        ExtensionResponse extensionResponse = this.gremlinExtension.evaluateOnGraph(
                rexsterResourceContext, graph, false, "g.v(1)", null);
        JSONObject jsonResponse = assertResponseAndGetEntity(extensionResponse,
                Response.Status.OK.getStatusCode());

        Assert.assertNotNull(jsonResponse);
        Assert.assertTrue(jsonResponse.has("results"));

        JSONArray jsonResults = jsonResponse.optJSONArray("results");
        Assert.assertNotNull(jsonResults);
        Assert.assertEquals(1, jsonResults.length());

        JSONObject firstResult = jsonResults.optJSONObject(0);
        Assert.assertNotNull(firstResult);
        Assert.assertTrue(firstResult.has("name"));
        Assert.assertEquals("marko", firstResult.optString("name"));
    }

    @Test
    public void evaluateOnVertexNoKeysNoTypesReturnOutEdges() {
        ExtensionResponse extensionResponse = this.gremlinExtension.evaluateOnVertex(
                rexsterResourceContext, graph, graph.getVertex(6), false, "v.outEdges", null);
        JSONObject jsonResponse = assertResponseAndGetEntity(extensionResponse,
                Response.Status.OK.getStatusCode());

        Assert.assertNotNull(jsonResponse);
        Assert.assertTrue(jsonResponse.has("results"));

        JSONArray jsonResults = jsonResponse.optJSONArray("results");
        Assert.assertNotNull(jsonResults);
        Assert.assertEquals(1, jsonResults.length());
    }

    @Test
    public void evaluateOnEdgeNoKeysNoTypesReturnOutVertex() {
        ExtensionResponse extensionResponse = this.gremlinExtension.evaluateOnEdge(
                rexsterResourceContext, graph, graph.getEdge(7), false, "e.outVertex", null);
        JSONObject jsonResponse = assertResponseAndGetEntity(extensionResponse,
                Response.Status.OK.getStatusCode());

        Assert.assertNotNull(jsonResponse);
        Assert.assertTrue(jsonResponse.has("results"));

        JSONArray jsonResults = jsonResponse.optJSONArray("results");
        Assert.assertNotNull(jsonResults);
        Assert.assertEquals(1, jsonResults.length());
    }

    private JSONObject assertResponseAndGetEntity(ExtensionResponse extensionResponse, int expectedStatusCode) {
        return assertResponseAndGetEntity(extensionResponse, false, expectedStatusCode);
    }

    private JSONObject assertResponseAndGetEntity(ExtensionResponse extensionResponse, boolean isError, int expectedStatusCode) {
        Assert.assertNotNull(extensionResponse);
        Assert.assertEquals(isError, extensionResponse.isErrorResponse());

        Response response = extensionResponse.getJerseyResponse();
        Assert.assertEquals(expectedStatusCode, response.getStatus());

        return (JSONObject) response.getEntity();
    }
}
