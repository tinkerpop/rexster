package com.tinkerpop.rexster;

import com.tinkerpop.rexster.server.RexsterApplication;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.KeyIndexableGraph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.tg.TinkerGraphFactory;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.Sequence;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Variant;
import java.io.StringReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

/**
 * Tests edge resource.  Should not need to test any specific returns values as they are
 * covered under other unit tests.  The format of the results themselves should be covered
 * under the ElementJSONObject.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class EdgeResourceTest extends BaseTest {
    private final String requestUriPath = "";

    @Test
    public void getAllEdgesNoOffset() {
        final EdgeResource resource = this.constructEdgeResourceWithToyGraph().getResource();

        final Response response = resource.getAllEdges(graphName);
        this.assertEdgesOkResponseJsonStructure(6, 6, response);
    }

    @Test
    public void getEdgesKeyIndexed() {
        final HashMap<String, Object> parameters = new HashMap<String, Object>();
        parameters.put(Tokens.KEY, "weight");
        parameters.put(Tokens.VALUE, "(f,0.4)");

        final EdgeResource resource = this.constructEdgeResource(true, parameters).getResource();
        final KeyIndexableGraph graph = (KeyIndexableGraph) this.toyGraph;
        graph.createKeyIndex("weight", Edge.class);

        assertEdgesOkResponseJsonStructure(2, 2, resource.getAllEdges(graphName));
    }

    @Test
    public void getAllEdgesNoResults() {
        final EdgeResource resource = this.constructEdgeResourceWithEmptyGraph().getResource();
        final Response response = resource.getAllEdges(graphName);
        this.assertEdgesOkResponseJsonStructure(0, 0, response);
    }

    @Test
    public void getAllEdgesWithValidOffset() {
        final HashMap<String, Object> parameters = new HashMap<String, Object>();
        parameters.put(Tokens.REXSTER + "." + Tokens.OFFSET_START, "1");
        parameters.put(Tokens.REXSTER + "." + Tokens.OFFSET_END, "3");
        final EdgeResource resource = this.constructEdgeResource(true, parameters).getResource();

        final Response response = resource.getAllEdges(graphName);
        this.assertEdgesOkResponseJsonStructure(2, 2, response);
    }

    @Test
    public void getAllEdgesWithInvalidOffsetNotEnoughResults() {
        final HashMap<String, Object> parameters = new HashMap<String, Object>();
        parameters.put(Tokens.REXSTER + "." + Tokens.OFFSET_START, "10");
        parameters.put(Tokens.REXSTER + "." + Tokens.OFFSET_END, "20");
        final EdgeResource resource = this.constructEdgeResource(true, parameters).getResource();

        final Response response = resource.getAllEdges(graphName);
        this.assertEdgesOkResponseJsonStructure(0, 0, response);
    }

    @Test
    public void getAllEdgesWithInvalidOffsetStartAfterEnd() {
        final HashMap<String, Object> parameters = new HashMap<String, Object>();
        parameters.put(Tokens.REXSTER + "." + Tokens.OFFSET_START, "100");
        parameters.put(Tokens.REXSTER + "." + Tokens.OFFSET_END, "20");
        final EdgeResource resource = this.constructEdgeResource(true, parameters).getResource();

        final Response response = resource.getAllEdges(graphName);
        this.assertEdgesOkResponseJsonStructure(0, 0, response);
    }

    @Test(expected = WebApplicationException.class)
    public void getSingleEdgeNotFound() {
        final EdgeResource resource = this.constructEdgeResourceWithToyGraph().getResource();
        resource.getSingleEdge(graphName, "id-does-not-match-any");
    }

    @Test
    public void getSingleVertexFound() {
        final EdgeResource resource = this.constructEdgeResourceWithToyGraph().getResource();

        final Response response = resource.getSingleEdge(graphName, "12");
        Assert.assertNotNull(response);
        Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assert.assertNotNull(response.getEntity());
        Assert.assertTrue(response.getEntity() instanceof JSONObject);

        final JSONObject json = (JSONObject) response.getEntity();
        Assert.assertTrue(json.has(Tokens.QUERY_TIME));

        Assert.assertTrue(json.has(Tokens.RESULTS));
        Assert.assertFalse(json.isNull(Tokens.RESULTS));

        final JSONObject jsonResult = json.optJSONObject(Tokens.RESULTS);
        Assert.assertNotNull(jsonResult);
    }

    @Test(expected = WebApplicationException.class)
    public void postNullEdgeConsumesUriBadRequest() {
        final ResourceHolder<EdgeResource> holder = this.constructEdgeResourceWithToyGraph();
        final EdgeResource resource = holder.getResource();
        resource.postNullEdgeConsumesUri(holder.getRequest(), graphName);
    }

    @Test(expected = WebApplicationException.class)
    public void postNullEdgeConsumesUriVerticesNotFound() {
        final HashMap<String, Object> parameters = generateEdgeParametersToPost(true);
        final ResourceHolder<EdgeResource> holder = this.constructEdgeResource(false, parameters);
        final EdgeResource resource = holder.getResource();

        resource.postNullEdgeConsumesUri(holder.getRequest(), graphName);
    }

    @Test(expected = WebApplicationException.class)
    public void postEdgeConsumesUriWithIdThatIsExistingEdgeNoProperties() {
        final HashMap<String, Object> parameters = generateEdgeParametersToPost(true);
        final ResourceHolder<EdgeResource> holder = this.constructEdgeResource(false, parameters);
        final EdgeResource resource = holder.getResource();
        resource.postEdgeConsumesUri(holder.getRequest(), graphName, "12");
    }

    @Test
    public void postNullEdgeConsumesUriAcceptJsonValid() {
        final HashMap<String, Object> parameters = generateEdgeParametersToPost(true);

        this.emptyGraph.addVertex(1);
        this.emptyGraph.addVertex(2);

        final ResourceHolder<EdgeResource> holder = this.constructEdgeResource(false, parameters);
        final EdgeResource resource = holder.getResource();
        final Response response = resource.postNullEdgeConsumesUri(holder.getRequest(), graphName);
        assertPostEdgeProducesJson(response, false, false);
    }

    @Test
    public void postNullEdgeConsumesJsonAcceptJsonValid() {
        final HashMap<String, Object> parameters = generateEdgeParametersToPost(false);
        final JSONObject jsonToPost = new JSONObject(parameters);

        this.emptyGraph.addVertex(1);
        this.emptyGraph.addVertex(2);

        final ResourceHolder<EdgeResource> holder = this.constructEdgeResource(false, parameters);
        final EdgeResource resource = holder.getResource();
        final Response response = resource.postNullEdgeConsumesJson(holder.getRequest(), graphName, jsonToPost);
        assertPostEdgeProducesJson(response, false, false);
    }

    @Test
    public void postNullEdgeConsumesTypedJsonAcceptJsonValid() {
        final HashMap<String, Object> parameters = generateEdgeParametersToPost(true);
        final JSONObject jsonToPost = new JSONObject(parameters);

        this.emptyGraph.addVertex(1);
        this.emptyGraph.addVertex(2);

        final ResourceHolder<EdgeResource> holder = this.constructEdgeResource(false, parameters);
        final EdgeResource resource = holder.getResource();
        final Response response = resource.postNullEdgeConsumesTypedJson(holder.getRequest(), graphName, jsonToPost);
        assertPostEdgeProducesJson(response, false, false);
    }

    @Test
    public void postEdgeConsumesUriWithIdThatIsNewEdge() {
        final HashMap<String, Object> parameters = generateEdgeParametersToPost(true);

        this.emptyGraph.addVertex(1);
        this.emptyGraph.addVertex(2);

        final ResourceHolder<EdgeResource> holder = this.constructEdgeResource(false, parameters);
        final EdgeResource resource = holder.getResource();
        final Response response = resource.postEdgeConsumesUri(holder.getRequest(), graphName, "1");
        assertPostEdgeProducesJson(response, false, false);
    }

    @Test
    public void postEdgeConsumesUriWithIdThatIsExistingEdgeHasProperties() {
        final HashMap<String, Object> parameters = generateEdgeParametersToPost(true);
        parameters.put("some-property", "edge-property-value");

        final Vertex v1 = this.emptyGraph.addVertex(1);
        final Vertex v2 = this.emptyGraph.addVertex(2);
        final Edge e = this.emptyGraph.addEdge(1, v1, v2, "knows");

        final ResourceHolder<EdgeResource> holder = this.constructEdgeResource(false, parameters);
        final EdgeResource resource = holder.getResource();
        final Response response = resource.postEdgeConsumesUri(holder.getRequest(), graphName, "1");

        Assert.assertNotNull(response);
        Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assert.assertNotNull(response.getEntity());
        Assert.assertTrue(response.getEntity() instanceof JSONObject);

        final JSONObject json = (JSONObject) response.getEntity();
        Assert.assertTrue(json.has(Tokens.QUERY_TIME));
        Assert.assertTrue(json.has(Tokens.RESULTS));

        Assert.assertEquals("edge-property-value", e.getProperty("some-property"));

    }

    @Test
    public void putEdgeOnUriValid() {
        final HashMap<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("newProperty", "NEW");

        final Vertex v1 = this.emptyGraph.addVertex(1);
        final Vertex v2 = this.emptyGraph.addVertex(2);
        final Edge e = this.emptyGraph.addEdge(1, v1, v2, "knows");
        e.setProperty("oldProperty", "OLD");

        final ResourceHolder<EdgeResource> holder = this.constructEdgeResource(false, parameters);
        final EdgeResource resource = holder.getResource();
        final Response response = resource.putEdgeOnUri(holder.getRequest(), graphName, "1");

        Assert.assertNotNull(response);
        Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assert.assertNotNull(response.getEntity());
        Assert.assertTrue(response.getEntity() instanceof JSONObject);

        final JSONObject json = (JSONObject) response.getEntity();
        Assert.assertTrue(json.has(Tokens.QUERY_TIME));
        Assert.assertTrue(json.has(Tokens.RESULTS));

        Assert.assertEquals("NEW", e.getProperty("newProperty"));
        Assert.assertFalse(e.getPropertyKeys().contains("oldProperty"));
    }

    @Test(expected = WebApplicationException.class)
    public void deleteEdgeEdgeNotFound() {
        final EdgeResource resource = this.constructEdgeResourceWithToyGraph().getResource();
        resource.deleteEdge(graphName, "100");
    }

    @Test
    public void deleteEdgeRemoveEdgeFromGraph() {
        Assert.assertNotNull(this.toyGraph.getEdge(12));

        final EdgeResource resource = this.constructEdgeResourceWithToyGraph().getResource();
        resource.deleteEdge(graphName, "12");

        Assert.assertNull(this.toyGraph.getEdge(12));
    }

    @Test
    public void deleteEdgeRemovePropertyFromEdge() {
        final HashMap<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("to-delete", "");

        final Vertex v1 = this.emptyGraph.addVertex(1);
        final Vertex v2 = this.emptyGraph.addVertex(2);
        final Edge e = this.emptyGraph.addEdge(1, v1, v2, "knows");
        e.setProperty("to-delete", "get rid of me");

        Assert.assertTrue(this.emptyGraph.getEdge(1).getPropertyKeys().contains("to-delete"));

        final ResourceHolder<EdgeResource> holder = this.constructEdgeResource(false, parameters);
        final EdgeResource resource = holder.getResource();
        final Response response = resource.deleteEdge(graphName, "1");

        Assert.assertFalse(this.emptyGraph.getEdge(1).getPropertyKeys().contains("to-delete"));
    }

    private static void assertPostEdgeProducesJson(Response response, boolean hasHypermedia, boolean hasTypes) {
        Assert.assertNotNull(response);
        Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assert.assertNotNull(response.getEntity());
        Assert.assertTrue(response.getEntity() instanceof JSONObject);

        JSONObject json = (JSONObject) response.getEntity();
        Assert.assertTrue(json.has(Tokens.QUERY_TIME));

        Assert.assertTrue(json.has(Tokens.RESULTS));
        Assert.assertFalse(json.isNull(Tokens.RESULTS));

        JSONObject postedEdge = json.optJSONObject(Tokens.RESULTS);

        if (hasTypes) {
            JSONObject somePropertyJson = postedEdge.optJSONObject("some-property");
            Assert.assertEquals("string", somePropertyJson.optString("type"));
            Assert.assertEquals("300a", somePropertyJson.optString("value"));

            JSONObject intPropertyJson = postedEdge.optJSONObject("int-property");
            Assert.assertEquals("integer", intPropertyJson.optString("type"));
            Assert.assertEquals(300, intPropertyJson.optInt("value"));
        } else {
            Assert.assertEquals("300a", postedEdge.optString("some-property"));
            Assert.assertEquals(300, postedEdge.optInt("int-property"));
        }

        if (hasHypermedia) {
            Assert.assertTrue(json.has(Tokens.EXTENSIONS));
        }
    }

    private static HashMap<String, Object> generateEdgeParametersToPost(boolean rexsterTyped) {
        final HashMap<String, Object> parameters = new HashMap<String, Object>();
        parameters.put(Tokens._IN_V, "1");
        parameters.put(Tokens._OUT_V, "2");
        parameters.put(Tokens._LABEL, "edge-label");

        if (rexsterTyped) {
            parameters.put("some-property", "(s,300a)");
            parameters.put("int-property", "(i,300)");
        } else {
            parameters.put("some-property", "300a");
            parameters.put("int-property", 300);
        }

        return parameters;
    }

    private static void initializeExtensionConfigurations(RexsterApplicationGraph rag) {
        String xmlString = "<extension><namespace>tp</namespace><name>extensionname</name><configuration><test>1</test></configuration></extension>";

        XMLConfiguration xmlConfig = new XMLConfiguration();

        try {
            xmlConfig.load(new StringReader(xmlString));
        } catch (ConfigurationException ex) {
            Assert.fail(ex.getMessage());
        }

        List<HierarchicalConfiguration> list = new ArrayList<HierarchicalConfiguration>();
        list.add(xmlConfig);

        List allowables = new ArrayList();
        allowables.add("tp:*");
        rag.loadAllowableExtensions(allowables);

        rag.loadExtensionsConfigurations(list);
    }

    private EdgeResource constructMockDeleteEdgeScenario(final Edge edge, final HashMap<String, String> parameters) {

        final Graph graph = this.mockery.mock(Graph.class);
        final RexsterApplicationGraph rag = new RexsterApplicationGraph("graph", graph);
        final RexsterApplication ra = this.mockery.mock(RexsterApplication.class);

        final UriInfo uri = this.mockery.mock(UriInfo.class);
        final HttpServletRequest httpServletRequest = this.mockery.mock(HttpServletRequest.class);

        this.mockery.checking(new Expectations() {{
            allowing(httpServletRequest).getParameterMap();
            will(returnValue(parameters));
            allowing(graph).getEdge(with(any(Object.class)));
            will(returnValue(edge));
            allowing(ra).getApplicationGraph(with(any(String.class)));
            will(returnValue(rag));
            allowing(uri).getAbsolutePath();
            will(returnValue(requestUriPath));
        }});

        EdgeResource resource = new EdgeResource(uri, httpServletRequest, ra);
        return resource;
    }

    private EdgeResource constructMockGetSingleEdgeScenario(final Edge edge, final HashMap<String, String> parameters) {
        final Graph graph = this.mockery.mock(Graph.class);
        final RexsterApplicationGraph rag = new RexsterApplicationGraph("graph", graph);

        List<String> namespaces = new ArrayList<String>();
        namespaces.add("*:*");
        rag.loadAllowableExtensions(namespaces);

        final RexsterApplication ra = this.mockery.mock(RexsterApplication.class);

        final UriInfo uri = this.mockery.mock(UriInfo.class);

        final HttpServletRequest httpServletRequest = this.mockery.mock(HttpServletRequest.class);

        this.mockery.checking(new Expectations() {{
            allowing(httpServletRequest).getParameterMap();
            will(returnValue(parameters));
            allowing(graph).getEdge(with(any(Object.class)));
            will(returnValue(edge));
            allowing(ra).getApplicationGraph(with(any(String.class)));
            will(returnValue(rag));
            allowing(uri).getAbsolutePath();
            will(returnValue(requestUriPath));
        }});

        EdgeResource resource = new EdgeResource(uri, httpServletRequest, ra);
        return resource;
    }

    private EdgeResource constructMockGetAllEdgesScenario(final int numberOfEdges, final HashMap<String, Object> parameters) {
        final Graph graph = this.mockery.mock(Graph.class);
        final RexsterApplicationGraph rag = new RexsterApplicationGraph("graph", graph);
        final RexsterApplication ra = this.mockery.mock(RexsterApplication.class);

        final UriInfo uri = this.mockery.mock(UriInfo.class);

        final HttpServletRequest httpServletRequest = this.mockery.mock(HttpServletRequest.class);

        this.mockery.checking(new Expectations() {{
            allowing(httpServletRequest).getParameterMap();
            will(returnValue(parameters));
            allowing(graph).getEdges();
            will(returnValue(generateMockedEdges(numberOfEdges)));
            allowing(ra).getApplicationGraph(with(any(String.class)));
            will(returnValue(rag));
            allowing(uri).getAbsolutePath();
            will(returnValue(requestUriPath));
        }});

        EdgeResource resource = new EdgeResource(uri, httpServletRequest, ra);
        return resource;
    }

    private void assertEdgesOkResponseJsonStructure(int numberOfEdgesReturned, int numberOfEdgesTotal, Response response) {
        Assert.assertNotNull(response);
        Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assert.assertNotNull(response.getEntity());
        Assert.assertTrue(response.getEntity() instanceof JSONObject);

        JSONObject json = (JSONObject) response.getEntity();
        Assert.assertTrue(json.has(Tokens.TOTAL_SIZE));
        Assert.assertEquals(numberOfEdgesTotal, json.optInt(Tokens.TOTAL_SIZE));
        Assert.assertTrue(json.has(Tokens.QUERY_TIME));

        Assert.assertTrue(json.has(Tokens.RESULTS));
        Assert.assertFalse(json.isNull(Tokens.RESULTS));

        JSONArray jsonResults = json.optJSONArray(Tokens.RESULTS);
        Assert.assertEquals(numberOfEdgesReturned, jsonResults.length());
    }

    private static Iterable<Edge> generateMockedEdges(int numberOfEdges) {
        ArrayList<Edge> edges = new ArrayList<Edge>();

        MockVertex v1 = new MockVertex("1");
        MockVertex v2 = new MockVertex("2");

        for (int ix = 0; ix < numberOfEdges; ix++) {
            MockEdge e = new MockEdge(new Integer(ix).toString(), "label-" + new Integer(ix).toString(), new Hashtable<String, Object>(), v1, v2);
            edges.add(e);
        }

        return edges;
    }
}