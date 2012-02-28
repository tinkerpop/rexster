package com.tinkerpop.rexster;

import com.tinkerpop.blueprints.pgm.Edge;
import com.tinkerpop.blueprints.pgm.Graph;
import com.tinkerpop.blueprints.pgm.Vertex;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
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
 */
public class EdgeResourceTest {

    protected Mockery mockery = new JUnit4Mockery();
    protected final String baseUri = "http://localhost/mock";

    protected final URI requestUriPath = URI.create("http://localhost/graphs/mock/edges");

    @Before
    public void init() {
        this.mockery = new JUnit4Mockery();
    }

    @Test
    public void getAllEdgesNoOffset() {
        final int numberOfEdges = 100;
        EdgeResource resource = this.constructMockGetAllEdgesScenario(numberOfEdges);

        Response response = resource.getAllEdges("graph");
        this.assertEdgesOkResponseJsonStructure(numberOfEdges, numberOfEdges, response);
    }

    @Test
    public void getAllEdgesNoResults() {
        final int numberOfEdges = 0;
        EdgeResource resource = this.constructMockGetAllEdgesScenario(numberOfEdges);

        Response response = resource.getAllEdges("graph");
        this.assertEdgesOkResponseJsonStructure(numberOfEdges, numberOfEdges, response);
    }

    @Test
    public void getAllEdgesWithValidOffset() {
        final int numberOfEdges = 100;
        HashMap<String, String> parameters = new HashMap<String, String>();
        parameters.put(Tokens.REXSTER + "." + Tokens.OFFSET_START, "10");
        parameters.put(Tokens.REXSTER + "." + Tokens.OFFSET_END, "20");
        EdgeResource resource = this.constructMockGetAllEdgesScenario(numberOfEdges, parameters);

        Response response = resource.getAllEdges("graph");
        this.assertEdgesOkResponseJsonStructure(10, numberOfEdges, response);

        JSONObject json = (JSONObject) response.getEntity();
        JSONArray jsonResults = json.optJSONArray(Tokens.RESULTS);

        // should return ids 10 through 19 from the random generated data
        for (int ix = 0; ix < jsonResults.length(); ix++) {
            Assert.assertEquals(ix + 10, jsonResults.optJSONObject(ix).optInt(Tokens._ID));
        }
    }

    @Test
    public void getAllEdgesWithInvalidOffsetNotEnoughResults() {
        final int numberOfEdges = 5;
        HashMap<String, String> parameters = new HashMap<String, String>();
        parameters.put(Tokens.REXSTER + "." + Tokens.OFFSET_START, "10");
        parameters.put(Tokens.REXSTER + "." + Tokens.OFFSET_END, "20");
        EdgeResource resource = this.constructMockGetAllEdgesScenario(numberOfEdges, parameters);

        Response response = resource.getAllEdges("graph");
        this.assertEdgesOkResponseJsonStructure(0, numberOfEdges, response);
    }

    @Test
    public void getAllEdgesWithInvalidOffsetStartAfterEnd() {
        final int numberOfEdges = 5;
        HashMap<String, String> parameters = new HashMap<String, String>();
        parameters.put(Tokens.REXSTER + "." + Tokens.OFFSET_START, "100");
        parameters.put(Tokens.REXSTER + "." + Tokens.OFFSET_END, "20");
        EdgeResource resource = this.constructMockGetAllEdgesScenario(numberOfEdges, parameters);

        Response response = resource.getAllEdges("graph");
        this.assertEdgesOkResponseJsonStructure(0, numberOfEdges, response);
    }

    @Test(expected = WebApplicationException.class)
    public void getSingleEdgeNotFound() {
        EdgeResource resource = this.constructMockGetSingleEdgeScenario(null, new HashMap<String, String>());
        resource.getSingleEdge("graph", "id-does-not-match-any");
    }

    @Test
    public void getSingleVertexFound() {

        Vertex v1 = new MockVertex("1");
        Vertex v2 = new MockVertex("2");

        Edge v = new MockEdge("1", "label-1", new Hashtable<String, Object>(), v1, v2);
        EdgeResource resource = this.constructMockGetSingleEdgeScenario(v, new HashMap<String, String>());

        Response response = resource.getSingleEdge("graph", "1");
        Assert.assertNotNull(response);
        Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assert.assertNotNull(response.getEntity());
        Assert.assertTrue(response.getEntity() instanceof JSONObject);

        JSONObject json = (JSONObject) response.getEntity();
        Assert.assertTrue(json.has(Tokens.QUERY_TIME));

        Assert.assertTrue(json.has(Tokens.RESULTS));
        Assert.assertFalse(json.isNull(Tokens.RESULTS));

        JSONObject jsonResult = (JSONObject) json.optJSONObject(Tokens.RESULTS);
        Assert.assertNotNull(jsonResult);
    }

    @Test(expected = WebApplicationException.class)
    public void postNullEdgeConsumesUriBadRequest() {
        final HashMap<String, String> parameters = new HashMap<String, String>();

        final Graph graph = this.mockery.mock(Graph.class);
        final RexsterApplicationGraph rag = new RexsterApplicationGraph("graph", graph);
        final RexsterApplicationProvider rap = this.mockery.mock(RexsterApplicationProvider.class);

        final UriInfo uri = this.mockery.mock(UriInfo.class);
        final HttpServletRequest httpServletRequest = this.mockery.mock(HttpServletRequest.class);

        final Request jsr311Request = this.mockery.mock(Request.class);
        final Variant variantJson = new Variant(MediaType.APPLICATION_JSON_TYPE, null, null);

        this.mockery.checking(new Expectations() {{
            allowing(httpServletRequest).getParameterMap();
            will(returnValue(parameters));
            allowing(graph).getEdge(with(any(Object.class)));
            will(returnValue(null));
            allowing(rap).getApplicationGraph(with(any(String.class)));
            will(returnValue(rag));
            allowing(jsr311Request).selectVariant(with(any(List.class)));
            will(returnValue(variantJson));
            allowing(uri).getAbsolutePath();
            will(returnValue(requestUriPath));
        }});

        EdgeResource resource = new EdgeResource(uri, httpServletRequest, rap);

        resource.postNullEdgeConsumesUri(jsr311Request, "graph");
    }

    @Test(expected = WebApplicationException.class)
    public void postNullEdgeConsumesUriVertexesNotFound() {
        final HashMap<String, Object> parameters = generateEdgeParametersToPost(true);

        final Graph graph = this.mockery.mock(Graph.class);
        final RexsterApplicationGraph rag = new RexsterApplicationGraph("graph", graph);
        final RexsterApplicationProvider rap = this.mockery.mock(RexsterApplicationProvider.class);

        final UriInfo uri = this.mockery.mock(UriInfo.class);
        final HttpServletRequest httpServletRequest = this.mockery.mock(HttpServletRequest.class);

        final Request jsr311Request = this.mockery.mock(Request.class);
        final Variant variantJson = new Variant(MediaType.APPLICATION_JSON_TYPE, null, null);

        this.mockery.checking(new Expectations() {{
            allowing(httpServletRequest).getParameterMap();
            will(returnValue(parameters));
            allowing(graph).getVertex(with(any(Object.class)));
            will(returnValue(null));
            allowing(graph).getEdge(with(any(Object.class)));
            will(returnValue(null));
            allowing(rap).getApplicationGraph(with(any(String.class)));
            will(returnValue(rag));
            allowing(jsr311Request).selectVariant(with(any(List.class)));
            will(returnValue(variantJson));
            allowing(uri).getAbsolutePath();
            will(returnValue(requestUriPath));
        }});

        EdgeResource resource = new EdgeResource(uri, httpServletRequest, rap);
        resource.postNullEdgeConsumesUri(jsr311Request, "graph");
    }

    @Test(expected = WebApplicationException.class)
    public void postEdgeConsumesUriWithIdThatIsExistingEdgeNoProperties() {
        final HashMap<String, String> parameters = new HashMap<String, String>();
        parameters.put(Tokens._IN_V, "1");
        parameters.put(Tokens._OUT_V, "2");
        parameters.put(Tokens._LABEL, "edge-label");

        final Vertex v1 = new MockVertex("1");
        final Vertex v2 = new MockVertex("2");

        final Edge returnEdge = new MockEdge("1", "label-1", new Hashtable<String, Object>(), v1, v2);

        final Graph graph = this.mockery.mock(Graph.class);
        final RexsterApplicationGraph rag = new RexsterApplicationGraph("graph", graph);
        final RexsterApplicationProvider rap = this.mockery.mock(RexsterApplicationProvider.class);

        final UriInfo uri = this.mockery.mock(UriInfo.class);
        final HttpServletRequest httpServletRequest = this.mockery.mock(HttpServletRequest.class);

        final Request jsr311Request = this.mockery.mock(Request.class);
        final Variant variantJson = new Variant(MediaType.APPLICATION_JSON_TYPE, null, null);

        this.mockery.checking(new Expectations() {{
            allowing(httpServletRequest).getParameterMap();
            will(returnValue(parameters));
            allowing(graph).getEdge(with(any(Object.class)));
            will(returnValue(returnEdge));
            allowing(rap).getApplicationGraph(with(any(String.class)));
            will(returnValue(rag));
            allowing(graph).getVertex(with(equal("1")));
            will(returnValue(v1));
            allowing(graph).getVertex(with(equal("2")));
            will(returnValue(v2));
            allowing(jsr311Request).selectVariant(with(any(List.class)));
            will(returnValue(variantJson));
            allowing(uri).getAbsolutePath();
            will(returnValue(requestUriPath));
        }});

        EdgeResource resource = new EdgeResource(uri, httpServletRequest, rap);
        resource.postEdgeConsumesUri(jsr311Request, "graph", "1");
    }

    @Test
    public void postNullEdgeConsumesUriAcceptJsonValid() {
        final HashMap<String, Object> parameters = generateEdgeParametersToPost(true);

        final Vertex v1 = new MockVertex("1");
        final Vertex v2 = new MockVertex("2");

        final Edge returnEdge = new MockEdge("1", "label-1", new Hashtable<String, Object>(), v1, v2);

        final Graph graph = this.mockery.mock(Graph.class);
        final RexsterApplicationGraph rag = new RexsterApplicationGraph("graph", graph);
        final RexsterApplicationProvider rap = this.mockery.mock(RexsterApplicationProvider.class);

        final UriInfo uri = this.mockery.mock(UriInfo.class);
        final HttpServletRequest httpServletRequest = this.mockery.mock(HttpServletRequest.class);

        final Request jsr311Request = this.mockery.mock(Request.class);
        final Variant variantJson = new Variant(MediaType.APPLICATION_JSON_TYPE, null, null);

        this.mockery.checking(new Expectations() {{
            allowing(httpServletRequest).getParameterMap();
            will(returnValue(parameters));
            allowing(graph).getEdge(with(any(Object.class)));
            will(returnValue(null));
            allowing(rap).getApplicationGraph(with(any(String.class)));
            will(returnValue(rag));
            allowing(graph).getVertex(with(equal("1")));
            will(returnValue(v1));
            allowing(graph).getVertex(with(equal("2")));
            will(returnValue(v2));
            allowing(graph).addEdge(null, v2, v1, "edge-label");
            will(returnValue(returnEdge));
            allowing(jsr311Request).selectVariant(with(any(List.class)));
            will(returnValue(variantJson));
            allowing(uri).getAbsolutePath();
            will(returnValue(requestUriPath));
        }});

        EdgeResource resource = new EdgeResource(uri, httpServletRequest, rap);
        Response response = resource.postNullEdgeConsumesUri(jsr311Request, "graph");

        assertPostEdgeProducesJson(response, false, false);
    }

    @Test
    public void postNullEdgeConsumesJsonAcceptJsonValid() {
        final HashMap<String, Object> parameters = generateEdgeParametersToPost(false);
        final JSONObject jsonToPost = new JSONObject(parameters);

        final Vertex v1 = new MockVertex("1");
        final Vertex v2 = new MockVertex("2");

        final Edge returnEdge = new MockEdge("1", "label-1", new Hashtable<String, Object>(), v1, v2);

        final Graph graph = this.mockery.mock(Graph.class);
        final RexsterApplicationGraph rag = new RexsterApplicationGraph("graph", graph);
        final RexsterApplicationProvider rap = this.mockery.mock(RexsterApplicationProvider.class);

        final UriInfo uri = this.mockery.mock(UriInfo.class);
        final HttpServletRequest httpServletRequest = this.mockery.mock(HttpServletRequest.class);

        final Request jsr311Request = this.mockery.mock(Request.class);
        final Variant variantJson = new Variant(MediaType.APPLICATION_JSON_TYPE, null, null);

        this.mockery.checking(new Expectations() {{
            allowing(httpServletRequest).getParameterMap();
            will(returnValue(parameters));
            allowing(graph).getEdge(with(any(Object.class)));
            will(returnValue(null));
            allowing(rap).getApplicationGraph(with(any(String.class)));
            will(returnValue(rag));
            allowing(graph).getVertex(with(equal("1")));
            will(returnValue(v1));
            allowing(graph).getVertex(with(equal("2")));
            will(returnValue(v2));
            allowing(graph).addEdge(null, v2, v1, "edge-label");
            will(returnValue(returnEdge));
            allowing(jsr311Request).selectVariant(with(any(List.class)));
            will(returnValue(variantJson));
            allowing(uri).getAbsolutePath();
            will(returnValue(requestUriPath));
        }});

        EdgeResource resource = new EdgeResource(uri, httpServletRequest, rap);
        Response response = resource.postNullEdgeConsumesJson(jsr311Request, "graph", jsonToPost);

        assertPostEdgeProducesJson(response, false, false);
    }

    @Test
    public void postNullEdgeConsumesTypedJsonAcceptJsonValid() {
        final HashMap<String, Object> parameters = generateEdgeParametersToPost(true);
        final JSONObject jsonToPost = new JSONObject(parameters);

        final Vertex v1 = new MockVertex("1");
        final Vertex v2 = new MockVertex("2");

        final Edge returnEdge = new MockEdge("1", "label-1", new Hashtable<String, Object>(), v1, v2);

        final Graph graph = this.mockery.mock(Graph.class);
        final RexsterApplicationGraph rag = new RexsterApplicationGraph("graph", graph);
        final RexsterApplicationProvider rap = this.mockery.mock(RexsterApplicationProvider.class);

        final UriInfo uri = this.mockery.mock(UriInfo.class);
        final HttpServletRequest httpServletRequest = this.mockery.mock(HttpServletRequest.class);

        final Request jsr311Request = this.mockery.mock(Request.class);
        final Variant variantJson = new Variant(MediaType.APPLICATION_JSON_TYPE, null, null);

        this.mockery.checking(new Expectations() {{
            allowing(httpServletRequest).getParameterMap();
            will(returnValue(parameters));
            allowing(graph).getEdge(with(any(Object.class)));
            will(returnValue(null));
            allowing(rap).getApplicationGraph(with(any(String.class)));
            will(returnValue(rag));
            allowing(graph).getVertex(with(equal("1")));
            will(returnValue(v1));
            allowing(graph).getVertex(with(equal("2")));
            will(returnValue(v2));
            allowing(graph).addEdge(null, v2, v1, "edge-label");
            will(returnValue(returnEdge));
            allowing(jsr311Request).selectVariant(with(any(List.class)));
            will(returnValue(variantJson));
            allowing(uri).getAbsolutePath();
            will(returnValue(requestUriPath));
        }});

        EdgeResource resource = new EdgeResource(uri, httpServletRequest, rap);
        Response response = resource.postNullEdgeConsumesTypedJson(jsr311Request, "graph", jsonToPost);

        assertPostEdgeProducesJson(response, false, false);
    }

    @Test
    public void postNullEdgeConsumesUriAcceptRexsterJsonValid() {
        final HashMap<String, Object> parameters = generateEdgeParametersToPost(true);

        final Vertex v1 = new MockVertex("1");
        final Vertex v2 = new MockVertex("2");

        final Edge returnEdge = new MockEdge("1", "label-1", new Hashtable<String, Object>(), v1, v2);

        final Graph graph = this.mockery.mock(Graph.class);
        final RexsterApplicationGraph rag = new RexsterApplicationGraph("graph", graph);
        initializeExtensionConfigurations(rag);
        final RexsterApplicationProvider rap = this.mockery.mock(RexsterApplicationProvider.class);

        final UriInfo uri = this.mockery.mock(UriInfo.class);
        final HttpServletRequest httpServletRequest = this.mockery.mock(HttpServletRequest.class);

        final Request jsr311Request = this.mockery.mock(Request.class);
        final Variant variantJson = new Variant(RexsterMediaType.APPLICATION_REXSTER_JSON_TYPE, null, null);

        this.mockery.checking(new Expectations() {{
            allowing(httpServletRequest).getParameterMap();
            will(returnValue(parameters));
            allowing(graph).getEdge(with(any(Object.class)));
            will(returnValue(null));
            allowing(rap).getApplicationGraph(with(any(String.class)));
            will(returnValue(rag));
            allowing(graph).getVertex(with(equal("1")));
            will(returnValue(v1));
            allowing(graph).getVertex(with(equal("2")));
            will(returnValue(v2));
            allowing(graph).addEdge(null, v2, v1, "edge-label");
            will(returnValue(returnEdge));
            allowing(jsr311Request).selectVariant(with(any(List.class)));
            will(returnValue(variantJson));
            allowing(uri).getAbsolutePath();
            will(returnValue(requestUriPath));
        }});

        EdgeResource resource = new EdgeResource(uri, httpServletRequest, rap);
        Response response = resource.postNullEdgeConsumesUri(jsr311Request, "graph");

        assertPostEdgeProducesJson(response, true, false);
    }

    @Test
    public void postNullEdgeConsumesJsonAcceptRexsterJsonValid() {
        final HashMap<String, Object> parameters = generateEdgeParametersToPost(false);
        final JSONObject jsonToPost = new JSONObject(parameters);

        final Vertex v1 = new MockVertex("1");
        final Vertex v2 = new MockVertex("2");

        final Edge returnEdge = new MockEdge("1", "label-1", new Hashtable<String, Object>(), v1, v2);

        final Graph graph = this.mockery.mock(Graph.class);
        final RexsterApplicationGraph rag = new RexsterApplicationGraph("graph", graph);
        initializeExtensionConfigurations(rag);
        final RexsterApplicationProvider rap = this.mockery.mock(RexsterApplicationProvider.class);

        final UriInfo uri = this.mockery.mock(UriInfo.class);
        final HttpServletRequest httpServletRequest = this.mockery.mock(HttpServletRequest.class);

        final Request jsr311Request = this.mockery.mock(Request.class);
        final Variant variantJson = new Variant(RexsterMediaType.APPLICATION_REXSTER_JSON_TYPE, null, null);

        this.mockery.checking(new Expectations() {{
            allowing(httpServletRequest).getParameterMap();
            will(returnValue(parameters));
            allowing(graph).getEdge(with(any(Object.class)));
            will(returnValue(null));
            allowing(rap).getApplicationGraph(with(any(String.class)));
            will(returnValue(rag));
            allowing(graph).getVertex(with(equal("1")));
            will(returnValue(v1));
            allowing(graph).getVertex(with(equal("2")));
            will(returnValue(v2));
            allowing(graph).addEdge(null, v2, v1, "edge-label");
            will(returnValue(returnEdge));
            allowing(jsr311Request).selectVariant(with(any(List.class)));
            will(returnValue(variantJson));
            allowing(uri).getAbsolutePath();
            will(returnValue(requestUriPath));
        }});

        EdgeResource resource = new EdgeResource(uri, httpServletRequest, rap);
        Response response = resource.postNullEdgeConsumesJson(jsr311Request, "graph", jsonToPost);

        assertPostEdgeProducesJson(response, true, false);
    }

    @Test
    public void postNullEdgeConsumesTypedJsonAcceptRexsterJsonValid() {
        final HashMap<String, Object> parameters = generateEdgeParametersToPost(true);
        final JSONObject jsonToPost = new JSONObject(parameters);

        final Vertex v1 = new MockVertex("1");
        final Vertex v2 = new MockVertex("2");

        final Edge returnEdge = new MockEdge("1", "label-1", new Hashtable<String, Object>(), v1, v2);

        final Graph graph = this.mockery.mock(Graph.class);
        final RexsterApplicationGraph rag = new RexsterApplicationGraph("graph", graph);
        initializeExtensionConfigurations(rag);
        final RexsterApplicationProvider rap = this.mockery.mock(RexsterApplicationProvider.class);

        final UriInfo uri = this.mockery.mock(UriInfo.class);
        final HttpServletRequest httpServletRequest = this.mockery.mock(HttpServletRequest.class);

        final Request jsr311Request = this.mockery.mock(Request.class);
        final Variant variantJson = new Variant(RexsterMediaType.APPLICATION_REXSTER_JSON_TYPE, null, null);

        this.mockery.checking(new Expectations() {{
            allowing(httpServletRequest).getParameterMap();
            will(returnValue(parameters));
            allowing(graph).getEdge(with(any(Object.class)));
            will(returnValue(null));
            allowing(rap).getApplicationGraph(with(any(String.class)));
            will(returnValue(rag));
            allowing(graph).getVertex(with(equal("1")));
            will(returnValue(v1));
            allowing(graph).getVertex(with(equal("2")));
            will(returnValue(v2));
            allowing(graph).addEdge(null, v2, v1, "edge-label");
            will(returnValue(returnEdge));
            allowing(jsr311Request).selectVariant(with(any(List.class)));
            will(returnValue(variantJson));
            allowing(uri).getAbsolutePath();
            will(returnValue(requestUriPath));
        }});

        EdgeResource resource = new EdgeResource(uri, httpServletRequest, rap);
        Response response = resource.postNullEdgeConsumesTypedJson(jsr311Request, "graph", jsonToPost);

        assertPostEdgeProducesJson(response, true, false);
    }


    @Test
    public void postNullEdgeConsumesUriAcceptRexsterJsonTrueValid() {
        final HashMap<String, Object> parameters = generateEdgeParametersToPost(true);

        final Vertex v1 = new MockVertex("1");
        final Vertex v2 = new MockVertex("2");

        final Edge returnEdge = new MockEdge("1", "label-1", new Hashtable<String, Object>(), v1, v2);

        final Graph graph = this.mockery.mock(Graph.class);
        final RexsterApplicationGraph rag = new RexsterApplicationGraph("graph", graph);
        initializeExtensionConfigurations(rag);
        final RexsterApplicationProvider rap = this.mockery.mock(RexsterApplicationProvider.class);

        final UriInfo uri = this.mockery.mock(UriInfo.class);
        final HttpServletRequest httpServletRequest = this.mockery.mock(HttpServletRequest.class);

        final Request jsr311Request = this.mockery.mock(Request.class);
        final Variant variantJson = new Variant(RexsterMediaType.APPLICATION_REXSTER_TYPED_JSON_TYPE, null, null);

        this.mockery.checking(new Expectations() {{
            allowing(httpServletRequest).getParameterMap();
            will(returnValue(parameters));
            allowing(graph).getEdge(with(any(Object.class)));
            will(returnValue(null));
            allowing(rap).getApplicationGraph(with(any(String.class)));
            will(returnValue(rag));
            allowing(graph).getVertex(with(equal("1")));
            will(returnValue(v1));
            allowing(graph).getVertex(with(equal("2")));
            will(returnValue(v2));
            allowing(graph).addEdge(null, v2, v1, "edge-label");
            will(returnValue(returnEdge));
            allowing(jsr311Request).selectVariant(with(any(List.class)));
            will(returnValue(variantJson));
            allowing(uri).getAbsolutePath();
            will(returnValue(requestUriPath));
        }});

        EdgeResource resource = new EdgeResource(uri, httpServletRequest, rap);
        Response response = resource.postNullEdgeConsumesUri(jsr311Request, "graph");

        assertPostEdgeProducesJson(response, true, true);
    }

    @Test
    public void postNullEdgeConsumesJsonAcceptRexsterTypedJsonValid() {
        final HashMap<String, Object> parameters = generateEdgeParametersToPost(false);
        final JSONObject jsonToPost = new JSONObject(parameters);

        final Vertex v1 = new MockVertex("1");
        final Vertex v2 = new MockVertex("2");

        final Edge returnEdge = new MockEdge("1", "label-1", new Hashtable<String, Object>(), v1, v2);

        final Graph graph = this.mockery.mock(Graph.class);
        final RexsterApplicationGraph rag = new RexsterApplicationGraph("graph", graph);
        initializeExtensionConfigurations(rag);
        final RexsterApplicationProvider rap = this.mockery.mock(RexsterApplicationProvider.class);

        final UriInfo uri = this.mockery.mock(UriInfo.class);
        final HttpServletRequest httpServletRequest = this.mockery.mock(HttpServletRequest.class);

        final Request jsr311Request = this.mockery.mock(Request.class);
        final Variant variantJson = new Variant(RexsterMediaType.APPLICATION_REXSTER_TYPED_JSON_TYPE, null, null);

        this.mockery.checking(new Expectations() {{
            allowing(httpServletRequest).getParameterMap();
            will(returnValue(parameters));
            allowing(graph).getEdge(with(any(Object.class)));
            will(returnValue(null));
            allowing(rap).getApplicationGraph(with(any(String.class)));
            will(returnValue(rag));
            allowing(graph).getVertex(with(equal("1")));
            will(returnValue(v1));
            allowing(graph).getVertex(with(equal("2")));
            will(returnValue(v2));
            allowing(graph).addEdge(null, v2, v1, "edge-label");
            will(returnValue(returnEdge));
            allowing(jsr311Request).selectVariant(with(any(List.class)));
            will(returnValue(variantJson));
            allowing(uri).getAbsolutePath();
            will(returnValue(requestUriPath));
        }});

        EdgeResource resource = new EdgeResource(uri, httpServletRequest, rap);
        Response response = resource.postNullEdgeConsumesJson(jsr311Request, "graph", jsonToPost);

        assertPostEdgeProducesJson(response, true, true);
    }

    @Test
    public void postNullEdgeConsumesTypedJsonAcceptRexsterTypedJsonValid() {
        final HashMap<String, Object> parameters = generateEdgeParametersToPost(true);
        final JSONObject jsonToPost = new JSONObject(parameters);

        final Vertex v1 = new MockVertex("1");
        final Vertex v2 = new MockVertex("2");

        final Edge returnEdge = new MockEdge("1", "label-1", new Hashtable<String, Object>(), v1, v2);

        final Graph graph = this.mockery.mock(Graph.class);
        final RexsterApplicationGraph rag = new RexsterApplicationGraph("graph", graph);
        initializeExtensionConfigurations(rag);
        final RexsterApplicationProvider rap = this.mockery.mock(RexsterApplicationProvider.class);

        final UriInfo uri = this.mockery.mock(UriInfo.class);
        final HttpServletRequest httpServletRequest = this.mockery.mock(HttpServletRequest.class);

        final Request jsr311Request = this.mockery.mock(Request.class);
        final Variant variantJson = new Variant(RexsterMediaType.APPLICATION_REXSTER_TYPED_JSON_TYPE, null, null);

        this.mockery.checking(new Expectations() {{
            allowing(httpServletRequest).getParameterMap();
            will(returnValue(parameters));
            allowing(graph).getEdge(with(any(Object.class)));
            will(returnValue(null));
            allowing(rap).getApplicationGraph(with(any(String.class)));
            will(returnValue(rag));
            allowing(graph).getVertex(with(equal("1")));
            will(returnValue(v1));
            allowing(graph).getVertex(with(equal("2")));
            will(returnValue(v2));
            allowing(graph).addEdge(null, v2, v1, "edge-label");
            will(returnValue(returnEdge));
            allowing(jsr311Request).selectVariant(with(any(List.class)));
            will(returnValue(variantJson));
            allowing(uri).getAbsolutePath();
            will(returnValue(requestUriPath));
        }});

        EdgeResource resource = new EdgeResource(uri, httpServletRequest, rap);
        Response response = resource.postNullEdgeConsumesTypedJson(jsr311Request, "graph", jsonToPost);

        assertPostEdgeProducesJson(response, true, true);
    }

    @Test
    public void postEdgeConsumesUriWithIdThatIsNewEdge() {
        final HashMap<String, Object> parameters = generateEdgeParametersToPost(true);

        final Vertex v1 = new MockVertex("1");
        final Vertex v2 = new MockVertex("2");

        final Edge returnEdge = new MockEdge("1", "label-1", new Hashtable<String, Object>(), v1, v2);

        final Graph graph = this.mockery.mock(Graph.class);
        final RexsterApplicationGraph rag = new RexsterApplicationGraph("graph", graph);
        final RexsterApplicationProvider rap = this.mockery.mock(RexsterApplicationProvider.class);

        final UriInfo uri = this.mockery.mock(UriInfo.class);
        final HttpServletRequest httpServletRequest = this.mockery.mock(HttpServletRequest.class);

        final Request jsr311Request = this.mockery.mock(Request.class);
        final Variant variantJson = new Variant(MediaType.APPLICATION_JSON_TYPE, null, null);

        this.mockery.checking(new Expectations() {{
            allowing(httpServletRequest).getParameterMap();
            will(returnValue(parameters));
            allowing(graph).getEdge(with(any(Object.class)));
            will(returnValue(null));
            allowing(rap).getApplicationGraph(with(any(String.class)));
            will(returnValue(rag));
            allowing(graph).getVertex(with(equal("1")));
            will(returnValue(v1));
            allowing(graph).getVertex(with(equal("2")));
            will(returnValue(v2));
            allowing(graph).addEdge("1", v2, v1, "edge-label");
            will(returnValue(returnEdge));
            allowing(jsr311Request).selectVariant(with(any(List.class)));
            will(returnValue(variantJson));
            allowing(uri).getAbsolutePath();
            will(returnValue(requestUriPath));
        }});

        EdgeResource resource = new EdgeResource(uri, httpServletRequest, rap);
        resource.postEdgeConsumesUri(jsr311Request, "graph", "1");
    }

    @Test
    public void postEdgeConsumesUriWithIdThatIsExistingEdgeHasProperties() {
        final HashMap<String, Object> parameters = generateEdgeParametersToPost(true);
        parameters.put("some-property", "edge-property-value");

        final Vertex v1 = new MockVertex("1");
        final Vertex v2 = new MockVertex("2");

        final Edge returnEdge = new MockEdge("1", "label-1", new Hashtable<String, Object>(), v1, v2);

        final Graph graph = this.mockery.mock(Graph.class);
        final RexsterApplicationGraph rag = new RexsterApplicationGraph("graph", graph);
        final RexsterApplicationProvider rap = this.mockery.mock(RexsterApplicationProvider.class);

        final UriInfo uri = this.mockery.mock(UriInfo.class);
        final HttpServletRequest httpServletRequest = this.mockery.mock(HttpServletRequest.class);

        final Request jsr311Request = this.mockery.mock(Request.class);
        final Variant variantJson = new Variant(MediaType.APPLICATION_JSON_TYPE, null, null);

        this.mockery.checking(new Expectations() {{
            allowing(httpServletRequest).getParameterMap();
            will(returnValue(parameters));
            allowing(graph).getEdge(with(any(Object.class)));
            will(returnValue(returnEdge));
            allowing(rap).getApplicationGraph(with(any(String.class)));
            will(returnValue(rag));
            allowing(graph).getVertex(with(equal("1")));
            will(returnValue(v1));
            allowing(graph).getVertex(with(equal("2")));
            will(returnValue(v2));
            allowing(jsr311Request).selectVariant(with(any(List.class)));
            will(returnValue(variantJson));
            allowing(uri).getAbsolutePath();
            will(returnValue(requestUriPath));
        }});

        EdgeResource resource = new EdgeResource(uri, httpServletRequest, rap);
        Response response = resource.postEdgeConsumesUri(jsr311Request, "graph", "1");

        Assert.assertNotNull(response);
        Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assert.assertNotNull(response.getEntity());
        Assert.assertTrue(response.getEntity() instanceof JSONObject);

        JSONObject json = (JSONObject) response.getEntity();
        Assert.assertTrue(json.has(Tokens.QUERY_TIME));
        Assert.assertTrue(json.has(Tokens.RESULTS));

        Assert.assertEquals("edge-property-value", returnEdge.getProperty("some-property"));

    }

    @Test
    public void putEdgeOnUriValid() {
        final HashMap<String, String> parameters = new HashMap<String, String>();
        parameters.put("newProperty", "NEW");

        final Vertex v1 = new MockVertex("1");
        final Vertex v2 = new MockVertex("2");

        Hashtable<String, Object> properties = new Hashtable<String, Object>();
        properties.put("oldProperty", "OLD");

        final Edge returnEdge = new MockEdge("1", "label-1", properties, v1, v2);

        final Graph graph = this.mockery.mock(Graph.class);
        final RexsterApplicationGraph rag = new RexsterApplicationGraph("graph", graph);
        final RexsterApplicationProvider rap = this.mockery.mock(RexsterApplicationProvider.class);

        final UriInfo uri = this.mockery.mock(UriInfo.class);
        final HttpServletRequest httpServletRequest = this.mockery.mock(HttpServletRequest.class);

        final Request jsr311Request = this.mockery.mock(Request.class);
        final Variant variantJson = new Variant(MediaType.APPLICATION_JSON_TYPE, null, null);

        this.mockery.checking(new Expectations() {{
            allowing(httpServletRequest).getParameterMap();
            will(returnValue(parameters));
            allowing(graph).getEdge(with(any(Object.class)));
            will(returnValue(returnEdge));
            allowing(rap).getApplicationGraph(with(any(String.class)));
            will(returnValue(rag));
            allowing(jsr311Request).selectVariant(with(any(List.class)));
            will(returnValue(variantJson));
            allowing(uri).getAbsolutePath();
            will(returnValue(requestUriPath));
        }});

        EdgeResource resource = new EdgeResource(uri, httpServletRequest, rap);
        Response response = resource.putEdgeOnUri(jsr311Request, "graph", "1");

        Assert.assertNotNull(response);
        Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assert.assertNotNull(response.getEntity());
        Assert.assertTrue(response.getEntity() instanceof JSONObject);

        JSONObject json = (JSONObject) response.getEntity();
        Assert.assertTrue(json.has(Tokens.QUERY_TIME));
        Assert.assertTrue(json.has(Tokens.RESULTS));

        Assert.assertEquals("NEW", returnEdge.getProperty("newProperty"));
        Assert.assertFalse(returnEdge.getPropertyKeys().contains("oldProperty"));
    }


    @Test(expected = WebApplicationException.class)
    public void deleteEdgeEdgeNotFound() {
        EdgeResource resource = this.constructMockDeleteEdgeScenario(null, new HashMap<String, String>());
        resource.deleteEdge("graph", "100");
    }

    @Test
    public void deleteEdgeRemoveEdgeFromGraph() {
        final Vertex v1 = new MockVertex("1");
        final Vertex v2 = new MockVertex("2");

        final Edge returnEdge = new MockEdge("1", "label-1", new Hashtable<String, Object>(), v1, v2);

        final Graph graph = this.mockery.mock(Graph.class);
        final RexsterApplicationGraph rag = new RexsterApplicationGraph("graph", graph);
        final RexsterApplicationProvider rap = this.mockery.mock(RexsterApplicationProvider.class);

        final UriInfo uri = this.mockery.mock(UriInfo.class);
        final HttpServletRequest httpServletRequest = this.mockery.mock(HttpServletRequest.class);

        this.mockery.checking(new Expectations() {{
            allowing(httpServletRequest).getParameterMap();
            will(returnValue(new HashMap<String, String>()));
            allowing(graph).getEdge(with(any(Object.class)));
            will(returnValue(returnEdge));
            allowing(rap).getApplicationGraph(with(any(String.class)));
            will(returnValue(rag));
            allowing(graph).removeEdge(returnEdge);
            allowing(uri).getAbsolutePath();
            will(returnValue(requestUriPath));
        }});

        EdgeResource resource = new EdgeResource(uri, httpServletRequest, rap);
        resource.deleteEdge("graph", "100");
    }

    @Test
    public void deleteEdgeRemovePropertyFromEdge() {
        final HashMap<String, String> parameters = new HashMap<String, String>();
        parameters.put("to-delete", "");

        final Vertex v1 = new MockVertex("1");
        final Vertex v2 = new MockVertex("2");

        Hashtable<String, Object> properties = new Hashtable<String, Object>();
        properties.put("to-delete", "value");
        final Edge returnEdge = new MockEdge("100", "label-1", properties, v1, v2);

        final Graph graph = this.mockery.mock(Graph.class);
        final RexsterApplicationGraph rag = new RexsterApplicationGraph("graph", graph);
        final RexsterApplicationProvider rap = this.mockery.mock(RexsterApplicationProvider.class);

        final UriInfo uri = this.mockery.mock(UriInfo.class);
        final HttpServletRequest httpServletRequest = this.mockery.mock(HttpServletRequest.class);

        this.mockery.checking(new Expectations() {{
            allowing(httpServletRequest).getParameterMap();
            will(returnValue(parameters));
            allowing(graph).getEdge(with(any(Object.class)));
            will(returnValue(returnEdge));
            allowing(rap).getApplicationGraph(with(any(String.class)));
            will(returnValue(rag));
            allowing(uri).getAbsolutePath();
            will(returnValue(requestUriPath));
        }});

        EdgeResource resource = new EdgeResource(uri, httpServletRequest, rap);
        resource.deleteEdge("graph", "100");

        Assert.assertNull(returnEdge.getProperty("to-delete"));
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
        final RexsterApplicationProvider rap = this.mockery.mock(RexsterApplicationProvider.class);

        final UriInfo uri = this.mockery.mock(UriInfo.class);
        final HttpServletRequest httpServletRequest = this.mockery.mock(HttpServletRequest.class);

        this.mockery.checking(new Expectations() {{
            allowing(httpServletRequest).getParameterMap();
            will(returnValue(parameters));
            allowing(graph).getEdge(with(any(Object.class)));
            will(returnValue(edge));
            allowing(rap).getApplicationGraph(with(any(String.class)));
            will(returnValue(rag));
            allowing(uri).getAbsolutePath();
            will(returnValue(requestUriPath));
        }});

        EdgeResource resource = new EdgeResource(uri, httpServletRequest, rap);
        return resource;
    }

    private EdgeResource constructMockPostEdgeScenario(final Edge edge, final String requestedInId, final String requestedOutId, final Vertex inVertex, final Vertex outVertex, final HashMap<String, String> parameters) {

        final Graph graph = this.mockery.mock(Graph.class);
        final RexsterApplicationGraph rag = new RexsterApplicationGraph("graph", graph);
        final RexsterApplicationProvider rap = this.mockery.mock(RexsterApplicationProvider.class);

        final UriInfo uri = this.mockery.mock(UriInfo.class);
        final HttpServletRequest httpServletRequest = this.mockery.mock(HttpServletRequest.class);

        this.mockery.checking(new Expectations() {{
            allowing(httpServletRequest).getParameterMap();
            will(returnValue(parameters));
            allowing(graph).getEdge(with(any(Object.class)));
            will(returnValue(edge));
            allowing(rap).getApplicationGraph(with(any(String.class)));
            will(returnValue(rag));
            allowing(graph).getVertex(with(equal(requestedInId)));
            will(returnValue(inVertex));
            allowing(graph).getVertex(with(equal(requestedOutId)));
            will(returnValue(outVertex));
            allowing(uri).getAbsolutePath();
            will(returnValue(requestUriPath));
        }});

        EdgeResource resource = new EdgeResource(uri, httpServletRequest, rap);
        return resource;
    }

    private EdgeResource constructMockGetSingleEdgeScenario(final Edge edge, final HashMap<String, String> parameters) {
        final Graph graph = this.mockery.mock(Graph.class);
        final RexsterApplicationGraph rag = new RexsterApplicationGraph("graph", graph);

        List<String> namespaces = new ArrayList<String>();
        namespaces.add("*:*");
        rag.loadAllowableExtensions(namespaces);

        final RexsterApplicationProvider rap = this.mockery.mock(RexsterApplicationProvider.class);

        final UriInfo uri = this.mockery.mock(UriInfo.class);

        final HttpServletRequest httpServletRequest = this.mockery.mock(HttpServletRequest.class);

        this.mockery.checking(new Expectations() {{
            allowing(httpServletRequest).getParameterMap();
            will(returnValue(parameters));
            allowing(graph).getEdge(with(any(Object.class)));
            will(returnValue(edge));
            allowing(rap).getApplicationGraph(with(any(String.class)));
            will(returnValue(rag));
            allowing(uri).getAbsolutePath();
            will(returnValue(requestUriPath));
        }});

        EdgeResource resource = new EdgeResource(uri, httpServletRequest, rap);
        return resource;
    }

    private EdgeResource constructMockGetAllEdgesScenario(final int numberOfVertices) {
        return this.constructMockGetAllEdgesScenario(numberOfVertices, new HashMap<String, String>());
    }

    private EdgeResource constructMockGetAllEdgesScenario(final int numberOfEdges, final HashMap<String, String> parameters) {
        final Graph graph = this.mockery.mock(Graph.class);
        final RexsterApplicationGraph rag = new RexsterApplicationGraph("graph", graph);
        final RexsterApplicationProvider rap = this.mockery.mock(RexsterApplicationProvider.class);

        final UriInfo uri = this.mockery.mock(UriInfo.class);

        final HttpServletRequest httpServletRequest = this.mockery.mock(HttpServletRequest.class);

        this.mockery.checking(new Expectations() {{
            allowing(httpServletRequest).getParameterMap();
            will(returnValue(parameters));
            allowing(graph).getEdges();
            will(returnValue(generateMockedEdges(numberOfEdges)));
            allowing(rap).getApplicationGraph(with(any(String.class)));
            will(returnValue(rag));
            allowing(uri).getAbsolutePath();
            will(returnValue(requestUriPath));
        }});

        EdgeResource resource = new EdgeResource(uri, httpServletRequest, rap);
        return resource;
    }

    private void assertEdgesOkResponseJsonStructure(int numberOfEdgesReturned, int numberOfEdgesTotal, Response response) {
        Assert.assertNotNull(response);
        Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assert.assertNotNull(response.getEntity());
        Assert.assertTrue(response.getEntity() instanceof JSONObject);

        JSONObject json = (JSONObject) response.getEntity();
        Assert.assertTrue(json.has(Tokens.TOTAL_SIZE));
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