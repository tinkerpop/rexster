package com.tinkerpop.rexster.gremlin;

import com.codahale.metrics.MetricRegistry;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.impls.tg.TinkerGraphFactory;
import com.tinkerpop.rexster.RexsterResourceContext;
import com.tinkerpop.rexster.Tokens;
import com.tinkerpop.rexster.extension.ExtensionMethod;
import com.tinkerpop.rexster.extension.ExtensionResponse;
import com.tinkerpop.rexster.protocol.EngineController;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.json.JSONTokener;
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
    private ExtensionMethod extensionMethodNoApi = new ExtensionMethod(null, null, null, null);

    private RexsterResourceContext rexsterResourceContext = new RexsterResourceContext(null, uriInfo,
            httpServletRequest, null, null, extensionMethodNoApi, null, new MetricRegistry());

    /**
     * Choosing not to mock Graph instance for these tests as GremlinGroovyScriptEngine is
     * embedded into the GremlinExtension.
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        EngineController.configure(-1, null);
        graph = TinkerGraphFactory.createTinkerGraph();
    }

    @Before
    public void beforeEachTest() {
        this.gremlinExtension = new GremlinExtension();
    }

    @Test
    public void evaluateGetOnGraphNoScript() {

        ExtensionResponse extensionResponse = this.gremlinExtension.evaluateGetOnGraph(
                rexsterResourceContext, graph, "");

        JSONObject jsonResponse = assertResponseAndGetEntity(extensionResponse,
                true,
                Response.Status.BAD_REQUEST.getStatusCode());

        Assert.assertNotNull(jsonResponse);
        Assert.assertTrue(jsonResponse.has(Tokens.MESSAGE));
        Assert.assertEquals("no scripts provided", jsonResponse.optString(Tokens.MESSAGE));
    }

    @Test
    public void evaluateGetOnGraphNoKeysNoTypesReturnVertex() {
        ExtensionResponse extensionResponse = this.gremlinExtension.evaluateGetOnGraph(
                rexsterResourceContext, graph, "g.v(1)");
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
    public void evaluateGetOnGraphWithBindings() throws Exception {
        String json = "{\"params\":{\"x\":1, \"y\":2, \"z\":\"test\", \"list\":[3,2,1,0], \"map\":{\"mapx\":[300,200,100]}}}";
        RexsterResourceContext rexsterResourceContext = new RexsterResourceContext(null, uriInfo,
                httpServletRequest, new JSONObject(new JSONTokener(json)), null, extensionMethodNoApi, null, new MetricRegistry());

        ExtensionResponse extensionResponse = this.gremlinExtension.evaluateGetOnGraph(
                rexsterResourceContext, graph, "[x+y, z, list.size, map.mapx.size]");
        JSONObject jsonResponse = assertResponseAndGetEntity(extensionResponse,
                Response.Status.OK.getStatusCode());

        Assert.assertNotNull(jsonResponse);
        Assert.assertTrue(jsonResponse.has("results"));

        JSONArray jsonResults = jsonResponse.optJSONArray("results");
        Assert.assertNotNull(jsonResults);
        Assert.assertEquals(4, jsonResults.length());

        Assert.assertEquals(3, jsonResults.optInt(0));
        Assert.assertEquals("test", jsonResults.optString(1));
        Assert.assertEquals(4, jsonResults.optInt(2));
        Assert.assertEquals(3, jsonResults.optInt(3));

    }

    @Test
    public void evaluateGetOnVertexNoKeysNoTypesReturnOutEdges() {
        ExtensionResponse extensionResponse = this.gremlinExtension.evaluateGetOnVertex(
                rexsterResourceContext, graph, graph.getVertex(6), "v.outEdges");
        JSONObject jsonResponse = assertResponseAndGetEntity(extensionResponse,
                Response.Status.OK.getStatusCode());

        Assert.assertNotNull(jsonResponse);
        Assert.assertTrue(jsonResponse.has("results"));

        JSONArray jsonResults = jsonResponse.optJSONArray("results");
        Assert.assertNotNull(jsonResults);
        Assert.assertEquals(1, jsonResults.length());
    }

    @Test
    public void evaluateGetOnEdgeNoKeysNoTypesReturnOutVertex() {
        ExtensionResponse extensionResponse = this.gremlinExtension.evaluateGetOnEdge(
                rexsterResourceContext, graph, graph.getEdge(7), "e.outVertex");
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
