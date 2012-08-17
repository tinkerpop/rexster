package com.tinkerpop.rexster;

import com.tinkerpop.blueprints.impls.tg.TinkerGraph;
import com.tinkerpop.rexster.server.DefaultRexsterApplication;
import com.tinkerpop.rexster.server.RexsterApplication;
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
import java.util.List;
import java.util.Set;

/**
 * Tests vertex resource.  Should not need to test any specific returns values as they are
 * covered under other unit tests.  The format of the results themselves should be covered
 * under the ElementJSONObject.
 */
public class VertexResourceTest {

    private Mockery mockery = new JUnit4Mockery();
    private final String baseUri = "http://localhost/mock";

    private final URI requestUriPath = URI.create("http://localhost/graphs/mock/vertices");

    private static final String graphName = "graph";

    private Graph toyGraph;
    private Graph emptyGraph;
    private RexsterApplication raToyGraph;
    private RexsterApplication raEmptyGraph;

    @Before
    public void init() {
        this.mockery = new JUnit4Mockery();

        this.toyGraph = TinkerGraphFactory.createTinkerGraph();
        this.raToyGraph = new DefaultRexsterApplication(graphName, this.toyGraph);

        this.emptyGraph = new TinkerGraph();
        this.raEmptyGraph = new DefaultRexsterApplication(graphName, this.emptyGraph);

        final List<String> namespaces = new ArrayList<String>();
        namespaces.add("*:*");
        this.raToyGraph.getApplicationGraph(graphName).loadAllowableExtensions(namespaces);
        this.raEmptyGraph.getApplicationGraph(graphName).loadAllowableExtensions(namespaces);
    }

    @Test
    public void getVerticesAll() {
        final VertexResource resource = this.constructResourceWithToyGraph().getResource();

        final Response response = resource.getVertices(graphName);
        this.assertVerticesOkResponseJsonStructure(6, 6, response);

        final JSONObject json = (JSONObject) response.getEntity();
        final JSONArray jsonResultArray = json.optJSONArray(Tokens.RESULTS);
        this.assertFoundElementsInResults(jsonResultArray, Tokens.VERTEX, "1", "2", "3", "4", "5", "6");
    }
    
    @Test
    public void getVerticesKeyIndexed() {
        final HashMap<String, Object> parameters = new HashMap<String, Object>();
        parameters.put(Tokens.KEY, "name");
        parameters.put(Tokens.VALUE, "marko");

        final VertexResource resource = this.constructResource(true, parameters).getResource();
        final KeyIndexableGraph graph = (KeyIndexableGraph) this.toyGraph;
        graph.createKeyIndex("name", Vertex.class);

        final Response response = resource.getVertices(graphName);
        assertVerticesOkResponseJsonStructure(1, 1, response);

        final JSONObject json = (JSONObject) response.getEntity();
        final JSONArray jsonResultArray = json.optJSONArray(Tokens.RESULTS);
        this.assertFoundElementsInResults(jsonResultArray, Tokens.VERTEX, "1");
    }

    @Test
    public void getVerticesNoResults() {
        final VertexResource resource = this.constructResourceWithEmptyGraph().getResource();
        final Response response = resource.getVertices(graphName);
        this.assertVerticesOkResponseJsonStructure(0, 0, response);
    }

    @Test
    public void getVerticesWithValidOffset() {
        final HashMap<String, Object> parameters = new HashMap<String, Object>();
        parameters.put(Tokens.REXSTER + "." + Tokens.OFFSET_START, "1");
        parameters.put(Tokens.REXSTER + "." + Tokens.OFFSET_END, "3");

        final VertexResource resource = this.constructResource(true, parameters).getResource();

        final Response response = resource.getVertices(graphName);
        this.assertVerticesOkResponseJsonStructure(2, 2, response);

        final JSONObject json = (JSONObject) response.getEntity();
        final JSONArray jsonResultArray = json.optJSONArray(Tokens.RESULTS);
        this.assertFoundElementsInResults(jsonResultArray, Tokens.VERTEX, "2", "1");
    }

    @Test
    public void getVerticesWithInvalidOffsetNotEnoughResults() {
        final HashMap<String, Object> parameters = new HashMap<String, Object>();
        parameters.put(Tokens.REXSTER + "." + Tokens.OFFSET_START, "10");
        parameters.put(Tokens.REXSTER + "." + Tokens.OFFSET_END, "20");
        final VertexResource resource = this.constructResource(true, parameters).getResource();

        final Response response = resource.getVertices(graphName);
        this.assertVerticesOkResponseJsonStructure(0, 0, response);
    }

    @Test
    public void getVerticesWithInvalidOffsetStartAfterEnd() {
        final HashMap<String, Object> parameters = new HashMap<String, Object>();
        parameters.put(Tokens.REXSTER + "." + Tokens.OFFSET_START, "100");
        parameters.put(Tokens.REXSTER + "." + Tokens.OFFSET_END, "20");
        final VertexResource resource = this.constructResource(true, parameters).getResource();

        final Response response = resource.getVertices(graphName);
        this.assertVerticesOkResponseJsonStructure(0, 0, response);
    }

    @Test(expected = WebApplicationException.class)
    public void getSingleVertexNotFound() {
        final VertexResource resource = this.constructResourceWithToyGraph().getResource();
        resource.getSingleVertex(graphName, "id-does-not-match-any");
    }

    @Test
    public void getSingleVertexFound() {
        final VertexResource resource = this.constructResourceWithToyGraph().getResource();

        final Response response = resource.getSingleVertex(graphName, "1");
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
    public void getVertexEdgesNotFound() {
        final VertexResource resource = this.constructResourceWithToyGraph().getResource();
        resource.getSingleVertex(graphName, "id-does-not-match-any");
    }

    @Test
    public void getVertexEdgesFoundVertexReturnInEdgesNoOffset() {
        final VertexResource resource = this.constructResourceWithToyGraph().getResource();

        final Response response = resource.getVertexEdges(graphName, "3", Tokens.IN_E);
        final JSONObject json = assertEdgesOkResponseJsonStructure(response, 3);

        final JSONArray jsonResultArray = json.optJSONArray(Tokens.RESULTS);
        this.assertFoundElementsInResults(jsonResultArray, Tokens.EDGE, "9", "11", "12");
    }

    @Test
    public void getVertexEdgesFoundVertexReturnOutEdgesNoOffsetWithLabel() {
        final HashMap<String, Object> map = new HashMap<String, Object>() {{
            put(Tokens._LABEL, "created");
        }};

        final VertexResource resource = this.constructResource(true, map).getResource();

        final Response response = resource.getVertexEdges(graphName, "1", Tokens.OUT_E);
        final JSONObject json = assertEdgesOkResponseJsonStructure(response, 1);

        final JSONArray jsonResultArray = json.optJSONArray(Tokens.RESULTS);
        this.assertFoundElementsInResults(jsonResultArray, Tokens.EDGE, "9");
    }

    @Test
    public void getVertexEdgesFoundVertexReturnInVerticesNoOffset() {
        final VertexResource resource = this.constructResourceWithToyGraph().getResource();

        final Response response = resource.getVertexEdges(graphName, "3", Tokens.IN);
        final JSONObject json = assertEdgesOkResponseJsonStructure(response, 3);

        final JSONArray jsonResultArray = json.optJSONArray(Tokens.RESULTS);
        this.assertFoundElementsInResults(jsonResultArray, Tokens.VERTEX, "1", "6", "4");
    }

    @Test
    public void getVertexEdgesFoundVertexReturnInVerticesNoOffsetWithLabel() {
        // add a edge to make sure the test is actually filtering
        final Vertex vertexMarko = this.toyGraph.getVertex(1);
        final Vertex vertexStephen = this.toyGraph.addVertex(null);
        this.toyGraph.addEdge(null, vertexStephen, vertexMarko, "knows");

        final HashMap<String, Object> map = new HashMap<String, Object>() {{
            put(Tokens._LABEL, "knows");
        }};

        final VertexResource resource = this.constructResource(true, map).getResource();

        final Response response = resource.getVertexEdges("graph", "1", Tokens.IN);
        final JSONObject json = assertEdgesOkResponseJsonStructure(response, 1);

        final JSONArray jsonResultArray = json.optJSONArray(Tokens.RESULTS);
        this.assertFoundElementsInResults(jsonResultArray, Tokens.VERTEX, vertexStephen.getId().toString());
    }

    @Test
    public void getVertexEdgesFoundVertexOutEdgesNoOffset() {
        final VertexResource resource = this.constructResourceWithToyGraph().getResource();

        final Response response = resource.getVertexEdges(graphName, "6", Tokens.OUT_E);
        final JSONObject json = assertEdgesOkResponseJsonStructure(response, 1);

        final JSONArray jsonResultArray = json.optJSONArray(Tokens.RESULTS);
        this.assertFoundElementsInResults(jsonResultArray, Tokens.EDGE, "12");
    }

    @Test
    public void getVertexEdgesFoundVertexOutVerticesNoOffset() {
        final VertexResource resource = this.constructResourceWithToyGraph().getResource();

        final Response response = resource.getVertexEdges(graphName, "6", Tokens.OUT);
        final JSONObject json = assertEdgesOkResponseJsonStructure(response, 1);

        final JSONArray jsonResultArray = json.optJSONArray(Tokens.RESULTS);
        this.assertFoundElementsInResults(jsonResultArray, Tokens.VERTEX, "3");
    }

    @Test
    public void getVertexEdgesFoundVertexBothEdgesNoOffset() {
        final VertexResource resource = this.constructResourceWithToyGraph().getResource();

        final Response response = resource.getVertexEdges(graphName, "4", Tokens.BOTH_E);
        final JSONObject json = assertEdgesOkResponseJsonStructure(response, 3);

        final JSONArray jsonResultArray = json.optJSONArray(Tokens.RESULTS);
        this.assertFoundElementsInResults(jsonResultArray, Tokens.EDGE, "8", "10", "11");
    }

    @Test
    public void getVertexEdgesFoundVertexBothVerticesNoOffset() {
        final VertexResource resource = this.constructResourceWithToyGraph().getResource();

        final Response response = resource.getVertexEdges(graphName, "4", Tokens.BOTH);
        final JSONObject json = assertEdgesOkResponseJsonStructure(response, 3);

        final JSONArray jsonResultArray = json.optJSONArray(Tokens.RESULTS);
        this.assertFoundElementsInResults(jsonResultArray, Tokens.VERTEX, "1", "3", "5");
    }

    @Test
    public void getVertexEdgesFoundVertexBothEdgesWithValidOffset() {
        final HashMap<String, Object> parameters = new HashMap<String, Object>();
        parameters.put(Tokens.REXSTER + "." + Tokens.OFFSET_START, "1");
        parameters.put(Tokens.REXSTER + "." + Tokens.OFFSET_END, "3");
        final VertexResource resource = this.constructResource(true, parameters).getResource();

        final Response response = resource.getVertexEdges(graphName, "1", Tokens.BOTH_E);
        final JSONObject json = assertEdgesOkResponseJsonStructure(response, 3);

        final JSONArray jsonResultArray = json.optJSONArray(Tokens.RESULTS);
        this.assertFoundElementsInResults(jsonResultArray, Tokens.EDGE, "8", "9");
    }

    @Test
    public void getVertexEdgesFoundVertexBothEdgesWithOffsetNotEnoughResults() {
        final HashMap<String, Object> parameters = new HashMap<String, Object>();
        parameters.put(Tokens.REXSTER + "." + Tokens.OFFSET_START, "10");
        parameters.put(Tokens.REXSTER + "." + Tokens.OFFSET_END, "20");
        final VertexResource resource = this.constructResource(true, parameters).getResource();

        final Response response = resource.getVertexEdges(graphName, "1", Tokens.BOTH_E);
        final JSONObject json = assertEdgesOkResponseJsonStructure(response, 3);

        final JSONArray jsonResultArray = json.optJSONArray(Tokens.RESULTS);
        Assert.assertNotNull(jsonResultArray);
        Assert.assertEquals(0, jsonResultArray.length());
    }

    @Test
    public void getVertexEdgesFoundVertexBothEdgesWithOffsetStartAfterEnd() {
        final HashMap<String, Object> parameters = new HashMap<String, Object>();
        parameters.put(Tokens.REXSTER + "." + Tokens.OFFSET_START, "30");
        parameters.put(Tokens.REXSTER + "." + Tokens.OFFSET_END, "20");
        final VertexResource resource = this.constructResource(true, parameters).getResource();

        final Response response = resource.getVertexEdges(graphName, "1", Tokens.BOTH_E);
        final JSONObject json = assertEdgesOkResponseJsonStructure(response, 3);

        final JSONArray jsonResultArray = json.optJSONArray(Tokens.RESULTS);
        Assert.assertNotNull(jsonResultArray);
        Assert.assertEquals(0, jsonResultArray.length());
    }

    @Test
    public void postNullVertexOnUriAcceptTypedJsonValid() {
        // types are always parsed on the URI
        final HashMap<String, Object> parameters = generateVertexParametersToPost(true);
        final ResourceHolder holder = constructResource(false, parameters,
                RexsterMediaType.APPLICATION_REXSTER_TYPED_JSON_TYPE);
        final VertexResource resource = holder.getResource();
        final Response response = resource.postNullVertexOnUri(holder.request, graphName);

        assertPostVertexProducesJson(response, false, true);
    }

    @Test
    public void postNullVertexRexsterConsumesJsonAcceptJsonValid() {
        final HashMap<String, Object> parameters = generateVertexParametersToPost(false);
        final JSONObject jsonToPost = new JSONObject(parameters);
        final ResourceHolder holder = constructResource(false, parameters);
        final VertexResource resource = holder.getResource();
        final Response response = resource.postNullVertexRexsterConsumesJson(holder.getRequest(), graphName, jsonToPost);
        assertPostVertexProducesJson(response, false, false);
    }

    @Test
    public void postNullVertexRexsterConsumesTypedJsonAcceptTypedJsonValid() {
        final HashMap<String, Object> parameters = generateVertexParametersToPost(true);
        final JSONObject jsonToPost = new JSONObject(parameters);
        final ResourceHolder holder = constructResource(false, parameters, RexsterMediaType.APPLICATION_REXSTER_TYPED_JSON_TYPE);
        final VertexResource resource = holder.getResource();
        final Response response = resource.postNullVertexRexsterConsumesTypedJson(holder.getRequest(), graphName, jsonToPost);
        assertPostVertexProducesJson(response, false, true);
    }

    @Test(expected = WebApplicationException.class)
    public void postVertexOnUriButHasElementProperties() {
        final HashMap<String, Object> parameters = new HashMap<String, Object>();
        parameters.put(Tokens._ID, "300");
        final ResourceHolder holder = constructResource(true, parameters);
        final VertexResource resource = holder.getResource();
        resource.postVertexOnUri(holder.getRequest(), graphName, "1");
    }

    @Test
    public void putVertexConsumesUriValid() {
        final HashMap<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("some-property", "300a");
        final ResourceHolder holder = this.constructResource(true, parameters);

        final VertexResource resource = holder.getResource();
        final Response response = resource.putVertexConsumesUri(holder.getRequest(), graphName, "1");

        Assert.assertNotNull(response);
        Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assert.assertNotNull(response.getEntity());
        Assert.assertTrue(response.getEntity() instanceof JSONObject);

        final JSONObject json = (JSONObject) response.getEntity();
        Assert.assertTrue(json.has(Tokens.QUERY_TIME));

        Assert.assertTrue(json.has(Tokens.RESULTS));
        Assert.assertFalse(json.isNull(Tokens.RESULTS));

        final Vertex v = this.toyGraph.getVertex(1);
        Assert.assertEquals("300a", v.getProperty("some-property"));
        Assert.assertFalse(v.getPropertyKeys().contains("name"));
    }

    @Test(expected = WebApplicationException.class)
    public void putVertexConsumesUriNoVertexFound() {
        final HashMap<String, Object> parameters = generateVertexParametersToPost(false);
        final ResourceHolder holder = constructResource(false, parameters);
        holder.getResource().putVertexConsumesUri(holder.getRequest(), graphName, "1");
    }

    @Test
    public void deleteVertexValid() {
        final ResourceHolder holder = this.constructResourceWithToyGraph();
        final VertexResource resource = holder.getResource();
        final Response response = resource.deleteVertex(graphName, "1");

        Assert.assertNotNull(response);
        Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assert.assertNotNull(response.getEntity());
        Assert.assertTrue(response.getEntity() instanceof JSONObject);

        final JSONObject json = (JSONObject) response.getEntity();
        Assert.assertTrue(json.has(Tokens.QUERY_TIME));
    }

    @Test(expected = WebApplicationException.class)
    public void deleteVertexNoVertexFound() {
        final ResourceHolder holder = this.constructResourceWithToyGraph();
        final VertexResource resource = holder.getResource();
        resource.deleteVertex(graphName, "100");
    }

    @Test
    public void deleteVertexPropertiesValid() {
        final HashMap<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("name", "");
        final ResourceHolder holder = this.constructResource(true, parameters);
        final VertexResource resource = holder.getResource();
        final Response response = resource.deleteVertex(graphName, "1");

        Assert.assertNotNull(response);
        Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assert.assertNotNull(response.getEntity());
        Assert.assertTrue(response.getEntity() instanceof JSONObject);

        final JSONObject json = (JSONObject) response.getEntity();
        Assert.assertTrue(json.has(Tokens.QUERY_TIME));

        final Vertex v = this.toyGraph.getVertex(1);
        final Set<String> keys = v.getPropertyKeys();
        Assert.assertEquals(1, keys.size());
    }

    private static HashMap<String, Object> generateVertexParametersToPost(boolean rexsterTyped) {
        final HashMap<String, Object> parameters = new HashMap<String, Object>();

        if (rexsterTyped) {
            parameters.put("some-property", "(s,300a)");
            parameters.put("int-property", "(i,300)");
        } else {
            parameters.put("some-property", "300a");
            parameters.put("int-property", 300);
        }
        return parameters;
    }

    private static void assertPostVertexProducesJson(final Response response, final boolean hasHypermedia, final boolean hasTypes) {
        Assert.assertNotNull(response);
        Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assert.assertNotNull(response.getEntity());
        Assert.assertTrue(response.getEntity() instanceof JSONObject);

        JSONObject json = (JSONObject) response.getEntity();
        Assert.assertTrue(json.has(Tokens.QUERY_TIME));

        Assert.assertTrue(json.has(Tokens.RESULTS));
        Assert.assertFalse(json.isNull(Tokens.RESULTS));

        JSONObject postedVertex = json.optJSONObject(Tokens.RESULTS);

        if (hasTypes) {
            JSONObject somePropertyJson = postedVertex.optJSONObject("some-property");
            Assert.assertEquals("string", somePropertyJson.optString("type"));
            Assert.assertEquals("300a", somePropertyJson.optString("value"));

            JSONObject intPropertyJson = postedVertex.optJSONObject("int-property");
            Assert.assertEquals("integer", intPropertyJson.optString("type"));
            Assert.assertEquals(300, intPropertyJson.optInt("value"));
        } else {
            Assert.assertEquals("300a", postedVertex.optString("some-property"));
            Assert.assertEquals(300, postedVertex.optInt("int-property"));
        }

        if (hasHypermedia) {
            Assert.assertTrue(json.has(Tokens.EXTENSIONS));
        }
    }

    private JSONObject assertEdgesOkResponseJsonStructure(Response response, int expectedTotalSize) {
        Assert.assertNotNull(response);
        Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assert.assertNotNull(response.getEntity());
        Assert.assertTrue(response.getEntity() instanceof JSONObject);

        JSONObject json = (JSONObject) response.getEntity();
        Assert.assertTrue(json.has(Tokens.QUERY_TIME));

        Assert.assertTrue(json.has(Tokens.TOTAL_SIZE));
        Assert.assertEquals(expectedTotalSize, json.optInt(Tokens.TOTAL_SIZE));

        Assert.assertTrue(json.has(Tokens.RESULTS));
        Assert.assertFalse(json.isNull(Tokens.RESULTS));
        return json;
    }

    private void assertVerticesOkResponseJsonStructure(int numberOfVerticesReturned, int numberOfVerticesTotal, Response response) {
        Assert.assertNotNull(response);
        Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assert.assertNotNull(response.getEntity());
        Assert.assertTrue(response.getEntity() instanceof JSONObject);

        JSONObject json = (JSONObject) response.getEntity();
        Assert.assertTrue(json.has(Tokens.TOTAL_SIZE));
        Assert.assertEquals(numberOfVerticesTotal, json.optInt(Tokens.TOTAL_SIZE));
        Assert.assertTrue(json.has(Tokens.QUERY_TIME));

        Assert.assertTrue(json.has(Tokens.RESULTS));
        Assert.assertFalse(json.isNull(Tokens.RESULTS));

        JSONArray jsonResults = json.optJSONArray(Tokens.RESULTS);
        Assert.assertEquals(numberOfVerticesReturned, jsonResults.length());
    }

    private ResourceHolder constructResourceWithToyGraph() {
        return this.constructResource(true, new HashMap<String, Object>(), MediaType.APPLICATION_JSON_TYPE);
    }

    private ResourceHolder constructResourceWithEmptyGraph() {
        return this.constructResource(false, new HashMap<String, Object>(), MediaType.APPLICATION_JSON_TYPE);
    }

    private ResourceHolder constructResource(final boolean useToyGraph,
                                             final HashMap<String, Object> parameters){
        return this.constructResource(useToyGraph, parameters, MediaType.APPLICATION_JSON_TYPE);
    }

    private ResourceHolder constructResource(final boolean useToyGraph,
                                             final HashMap<String, Object> parameters,
                                             final MediaType mediaType) {
        final UriInfo uri = this.mockery.mock(UriInfo.class);
        final HttpServletRequest httpServletRequest = this.mockery.mock(HttpServletRequest.class);

        final Request request = this.mockery.mock(Request.class);
        final Variant variantJson = new Variant(mediaType, null, null);

        this.mockery.checking(new Expectations() {{
            allowing(httpServletRequest).getParameterMap();
            will(returnValue(parameters));
            allowing(request).selectVariant(with(any(List.class)));
            will(returnValue(variantJson));
            allowing(uri).getAbsolutePath();
            will(returnValue(requestUriPath));
        }});

        final VertexResource resource = useToyGraph ? new VertexResource(uri, httpServletRequest, this.raToyGraph)
                : new VertexResource(uri, httpServletRequest, this.raEmptyGraph);
        return new ResourceHolder(resource, request);
    }

    private void assertFoundElementsInResults(final JSONArray jsonResultArray, final String elementType,
                                              final String... expectedIds) {
        Assert.assertNotNull(jsonResultArray);
        Assert.assertEquals(expectedIds.length, jsonResultArray.length());

        final List<String> foundIds = new ArrayList<String>();
        for (int ix = 0; ix < jsonResultArray.length(); ix++) {
            final JSONObject jsonResult = jsonResultArray.optJSONObject(ix);
            Assert.assertNotNull(jsonResult);
            Assert.assertEquals(elementType, jsonResult.optString(Tokens._TYPE));
            Assert.assertTrue(jsonResult.has(Tokens._ID));
            foundIds.add(jsonResult.optString(Tokens._ID));
        }

        for (String expectedId : expectedIds) {
            Assert.assertTrue(foundIds.contains(expectedId));
        }
    }

    private class ResourceHolder {
        private final VertexResource resource;
        private final Request request;

        public ResourceHolder(final VertexResource resource, final Request request) {
            this.resource = resource;
            this.request = request;
        }

        public VertexResource getResource() {
            return resource;
        }

        public Request getRequest() {
            return request;
        }
    }
}
