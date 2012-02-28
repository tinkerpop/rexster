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
import java.util.Set;

/**
 * Tests vertex resource.  Should not need to test any specific returns values as they are
 * covered under other unit tests.  The format of the results themselves should be covered
 * under the ElementJSONObject.
 */
public class VertexResourceTest {

    protected Mockery mockery = new JUnit4Mockery();
    protected final String baseUri = "http://localhost/mock";

    protected final URI requestUriPath = URI.create("http://localhost/graphs/mock/vertices");

    @Before
    public void init() {
        this.mockery = new JUnit4Mockery();
    }

    @Test
    public void getVerticesAll() {
        final int numberOfVertices = 100;
        VertexResource resource = this.constructMockGetVerticesScenario(numberOfVertices);

        Response response = resource.getVertices("graph");
        this.assertVerticesOkResponseJsonStructure(numberOfVertices, numberOfVertices, response);
    }

    @Test
    public void getVerticesNoResults() {
        final int numberOfVertices = 0;
        VertexResource resource = this.constructMockGetVerticesScenario(numberOfVertices);

        Response response = resource.getVertices("graph");
        this.assertVerticesOkResponseJsonStructure(numberOfVertices, numberOfVertices, response);
    }

    @Test
    public void getVerticesWithValidOffset() {
        final int numberOfVertices = 100;
        HashMap<String, String> parameters = new HashMap<String, String>();
        parameters.put(Tokens.REXSTER + "." + Tokens.OFFSET_START, "10");
        parameters.put(Tokens.REXSTER + "." + Tokens.OFFSET_END, "20");
        VertexResource resource = this.constructMockGetVerticesScenario(numberOfVertices, parameters);

        Response response = resource.getVertices("graph");
        this.assertVerticesOkResponseJsonStructure(10, numberOfVertices, response);

        JSONObject json = (JSONObject) response.getEntity();
        JSONArray jsonResults = json.optJSONArray(Tokens.RESULTS);

        // should return ids 10 through 19 from the random generated data
        for (int ix = 0; ix < jsonResults.length(); ix++) {
            Assert.assertEquals(ix + 10, jsonResults.optJSONObject(ix).optInt(Tokens._ID));
        }
    }

    @Test
    public void getVerticesWithInvalidOffsetNotEnoughResults() {
        final int numberOfVertices = 5;
        HashMap<String, String> parameters = new HashMap<String, String>();
        parameters.put(Tokens.REXSTER + "." + Tokens.OFFSET_START, "10");
        parameters.put(Tokens.REXSTER + "." + Tokens.OFFSET_END, "20");
        VertexResource resource = this.constructMockGetVerticesScenario(numberOfVertices, parameters);

        Response response = resource.getVertices("graph");
        this.assertVerticesOkResponseJsonStructure(0, numberOfVertices, response);
    }

    @Test
    public void getVerticesWithInvalidOffsetStartAfterEnd() {
        final int numberOfVertices = 5;
        HashMap<String, String> parameters = new HashMap<String, String>();
        parameters.put(Tokens.REXSTER + "." + Tokens.OFFSET_START, "100");
        parameters.put(Tokens.REXSTER + "." + Tokens.OFFSET_END, "20");
        VertexResource resource = this.constructMockGetVerticesScenario(numberOfVertices, parameters);

        Response response = resource.getVertices("graph");
        this.assertVerticesOkResponseJsonStructure(0, numberOfVertices, response);
    }

    @Test(expected = WebApplicationException.class)
    public void getSingleVertexNotFound() {
        VertexResource resource = this.constructMockGetSingleVertexScenario(null, new HashMap<String, String>());
        resource.getSingleVertex("graph", "id-does-not-match-any");
    }

    @Test
    public void getSingleVertexFound() {
        Vertex v = new MockVertex("1");
        VertexResource resource = this.constructMockGetSingleVertexScenario(v, new HashMap<String, String>());

        Response response = resource.getSingleVertex("graph", "1");
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
    public void getVertexEdgesNotFound() {
        VertexResource resource = this.constructMockGetSingleVertexScenario(null, new HashMap<String, String>());
        resource.getSingleVertex("graph", "id-does-not-match-any");
    }

    @Test
    public void getVertexEdgesFoundVertexReturnInEdgesNoOffset() {
        VertexResource resource = this.constructMockSimpleGraphScenario();

        Response response = resource.getVertexEdges("graph", "1", Tokens.IN_E);
        JSONObject json = assertEdgesOkResponseJsonStructure(response, 1);

        JSONArray jsonResultArray = json.optJSONArray(Tokens.RESULTS);
        Assert.assertNotNull(jsonResultArray);
        Assert.assertEquals(1, jsonResultArray.length());

        JSONObject jsonResult = jsonResultArray.optJSONObject(0);
        Assert.assertNotNull(jsonResult);
        Assert.assertTrue(jsonResult.has(Tokens._ID));
        Assert.assertEquals("1-2", jsonResult.optString(Tokens._ID));
    }

    @Test
    public void getVertexEdgesFoundVertexReturnOutEdgesNoOffsetWithLabel() {
        HashMap<String, String> map = new HashMap<String, String>() {{
            put(Tokens._LABEL, "label31");
        }};

        VertexResource resource = this.constructMockSimpleGraphScenario(map);

        Response response = resource.getVertexEdges("graph", "3", Tokens.OUT_E);
        JSONObject json = assertEdgesOkResponseJsonStructure(response, 1);

        JSONArray jsonResultArray = json.optJSONArray(Tokens.RESULTS);
        Assert.assertNotNull(jsonResultArray);
        Assert.assertEquals(1, jsonResultArray.length());

        JSONObject jsonResult = jsonResultArray.optJSONObject(0);
        Assert.assertNotNull(jsonResult);
        Assert.assertTrue(jsonResult.has(Tokens._ID));
        Assert.assertEquals("3-1", jsonResult.optString(Tokens._ID));
    }

    @Test
    public void getVertexEdgesFoundVertexReturnInVerticesNoOffset() {
        VertexResource resource = this.constructMockSimpleGraphScenario();

        Response response = resource.getVertexEdges("graph", "1", Tokens.IN);
        JSONObject json = assertEdgesOkResponseJsonStructure(response, 1);

        JSONArray jsonResultArray = json.optJSONArray(Tokens.RESULTS);
        Assert.assertNotNull(jsonResultArray);
        Assert.assertEquals(1, jsonResultArray.length());

        JSONObject jsonResult = jsonResultArray.optJSONObject(0);
        Assert.assertNotNull(jsonResult);
        Assert.assertTrue(jsonResult.has(Tokens._ID));
        Assert.assertEquals("2", jsonResult.optString(Tokens._ID));
    }

    @Test
    public void getVertexEdgesFoundVertexReturnInVerticesNoOffsetWithLabel() {
        HashMap<String, String> map = new HashMap<String, String>() {{
            put(Tokens._LABEL, "label12");
        }};

        VertexResource resource = this.constructMockSimpleGraphScenario(map);

        Response response = resource.getVertexEdges("graph", "1", Tokens.IN);
        JSONObject json = assertEdgesOkResponseJsonStructure(response, 1);

        JSONArray jsonResultArray = json.optJSONArray(Tokens.RESULTS);
        Assert.assertNotNull(jsonResultArray);
        Assert.assertEquals(1, jsonResultArray.length());

        JSONObject jsonResult = jsonResultArray.optJSONObject(0);
        Assert.assertNotNull(jsonResult);
        Assert.assertTrue(jsonResult.has(Tokens._ID));
        Assert.assertEquals("2", jsonResult.optString(Tokens._ID));
    }

    @Test
    public void getVertexEdgesFoundVertexOutEdgesNoOffset() {
        VertexResource resource = this.constructMockSimpleGraphScenario();

        Response response = resource.getVertexEdges("graph", "1", Tokens.OUT_E);
        JSONObject json = assertEdgesOkResponseJsonStructure(response, 1);

        JSONArray jsonResultArray = json.optJSONArray(Tokens.RESULTS);
        Assert.assertNotNull(jsonResultArray);
        Assert.assertEquals(1, jsonResultArray.length());

        JSONObject jsonResult = jsonResultArray.optJSONObject(0);
        Assert.assertNotNull(jsonResult);
        Assert.assertTrue(jsonResult.has(Tokens._ID));
        Assert.assertEquals("3-1", jsonResult.optString(Tokens._ID));
    }

    @Test
    public void getVertexEdgesFoundVertexOutVerticesNoOffset() {
        VertexResource resource = this.constructMockSimpleGraphScenario();

        Response response = resource.getVertexEdges("graph", "1", Tokens.OUT);
        JSONObject json = assertEdgesOkResponseJsonStructure(response, 1);

        JSONArray jsonResultArray = json.optJSONArray(Tokens.RESULTS);
        Assert.assertNotNull(jsonResultArray);
        Assert.assertEquals(1, jsonResultArray.length());

        JSONObject jsonResult = jsonResultArray.optJSONObject(0);
        Assert.assertNotNull(jsonResult);
        Assert.assertTrue(jsonResult.has(Tokens._ID));
        Assert.assertEquals("3", jsonResult.optString(Tokens._ID));
    }

    @Test
    public void getVertexEdgesFoundVertexBothEdgesNoOffset() {
        VertexResource resource = this.constructMockSimpleGraphScenario();

        Response response = resource.getVertexEdges("graph", "1", Tokens.BOTH_E);
        JSONObject json = assertEdgesOkResponseJsonStructure(response, 2);

        JSONArray jsonResultArray = (JSONArray) json.optJSONArray(Tokens.RESULTS);
        Assert.assertNotNull(jsonResultArray);
        Assert.assertEquals(2, jsonResultArray.length());
    }

    @Test
    public void getVertexEdgesFoundVertexBothVerticesNoOffset() {
        VertexResource resource = this.constructMockSimpleGraphScenario();

        Response response = resource.getVertexEdges("graph", "1", Tokens.BOTH);
        JSONObject json = assertEdgesOkResponseJsonStructure(response, 2);

        JSONArray jsonResultArray = json.optJSONArray(Tokens.RESULTS);
        Assert.assertNotNull(jsonResultArray);
        Assert.assertEquals(2, jsonResultArray.length());
    }

    @Test
    public void getVertexEdgesFoundVertexBothEdgesWithValidOffset() {
        HashMap<String, String> parameters = new HashMap<String, String>();
        parameters.put(Tokens.REXSTER + "." + Tokens.OFFSET_START, "10");
        parameters.put(Tokens.REXSTER + "." + Tokens.OFFSET_END, "20");
        VertexResource resource = this.constructMockMultiEdgedGraphScenario(100, parameters);

        Response response = resource.getVertexEdges("graph", "1", Tokens.BOTH_E);
        JSONObject json = assertEdgesOkResponseJsonStructure(response, 100);

        JSONArray jsonResultArray = (JSONArray) json.optJSONArray(Tokens.RESULTS);
        Assert.assertNotNull(jsonResultArray);
        Assert.assertEquals(10, jsonResultArray.length());
    }

    @Test
    public void getVertexEdgesFoundVertexBothEdgesWithOffsetNotEnoughResults() {
        HashMap<String, String> parameters = new HashMap<String, String>();
        parameters.put(Tokens.REXSTER + "." + Tokens.OFFSET_START, "10");
        parameters.put(Tokens.REXSTER + "." + Tokens.OFFSET_END, "20");
        VertexResource resource = this.constructMockMultiEdgedGraphScenario(5, parameters);

        Response response = resource.getVertexEdges("graph", "1", Tokens.BOTH_E);
        JSONObject json = assertEdgesOkResponseJsonStructure(response, 5);

        JSONArray jsonResultArray = (JSONArray) json.optJSONArray(Tokens.RESULTS);
        Assert.assertNotNull(jsonResultArray);
        Assert.assertEquals(0, jsonResultArray.length());
    }

    @Test
    public void getVertexEdgesFoundVertexBothEdgesWithOffsetStartAfterEnd() {
        HashMap<String, String> parameters = new HashMap<String, String>();
        parameters.put(Tokens.REXSTER + "." + Tokens.OFFSET_START, "30");
        parameters.put(Tokens.REXSTER + "." + Tokens.OFFSET_END, "20");
        VertexResource resource = this.constructMockMultiEdgedGraphScenario(100, parameters);

        Response response = resource.getVertexEdges("graph", "1", Tokens.BOTH_E);
        JSONObject json = assertEdgesOkResponseJsonStructure(response, 100);

        JSONArray jsonResultArray = (JSONArray) json.optJSONArray(Tokens.RESULTS);
        Assert.assertNotNull(jsonResultArray);
        Assert.assertEquals(0, jsonResultArray.length());
    }

    @Test
    public void postNullVertexOnUriAcceptJsonValid() {
        final HashMap<String, Object> parameters = generateVertexParametersToPost(true);

        final Graph graph = this.mockery.mock(Graph.class);
        final RexsterApplicationGraph rag = new RexsterApplicationGraph("graph", graph);
        final RexsterApplicationProvider rap = this.mockery.mock(RexsterApplicationProvider.class);

        final UriInfo uri = this.mockery.mock(UriInfo.class);
        final HttpServletRequest httpServletRequest = this.mockery.mock(HttpServletRequest.class);
        final Vertex v = new MockVertex("1");

        final Request jsr311Request = this.mockery.mock(Request.class);
        final Variant variantJson = new Variant(MediaType.APPLICATION_JSON_TYPE, null, null);

        this.mockery.checking(new Expectations() {{
            allowing(httpServletRequest).getParameterMap();
            will(returnValue(parameters));
            allowing(graph).getVertex(with(any(Object.class)));
            will(returnValue(null));
            allowing(graph).addVertex(with(any(Object.class)));
            will(returnValue(v));
            allowing(rap).getApplicationGraph(with(any(String.class)));
            will(returnValue(rag));
            allowing(jsr311Request).selectVariant(with(any(List.class)));
            will(returnValue(variantJson));
            allowing(uri).getAbsolutePath();
            will(returnValue(requestUriPath));
        }});

        VertexResource resource = new VertexResource(uri, httpServletRequest, rap);
        Response response = resource.postNullVertexOnUri(jsr311Request, "graph");

        assertPostVertexProducesJson(response, false, false);
    }

    @Test
    public void postNullVertexRexsterConsumesJsonAcceptJsonValid() {
        final HashMap<String, Object> parameters = generateVertexParametersToPost(false);

        final JSONObject jsonToPost = new JSONObject(parameters);

        final Graph graph = this.mockery.mock(Graph.class);
        final RexsterApplicationGraph rag = new RexsterApplicationGraph("graph", graph);
        final RexsterApplicationProvider rap = this.mockery.mock(RexsterApplicationProvider.class);

        final UriInfo uri = this.mockery.mock(UriInfo.class);
        final HttpServletRequest httpServletRequest = this.mockery.mock(HttpServletRequest.class);
        final Vertex v = new MockVertex("1");

        final Request jsr311Request = this.mockery.mock(Request.class);
        final Variant variantJson = new Variant(MediaType.APPLICATION_JSON_TYPE, null, null);

        this.mockery.checking(new Expectations() {{
            allowing(httpServletRequest).getParameterMap();
            will(returnValue(parameters));
            allowing(graph).getVertex(with(any(Object.class)));
            will(returnValue(null));
            allowing(graph).addVertex(with(any(Object.class)));
            will(returnValue(v));
            allowing(rap).getApplicationGraph(with(any(String.class)));
            will(returnValue(rag));
            allowing(jsr311Request).selectVariant(with(any(List.class)));
            will(returnValue(variantJson));
            allowing(uri).getAbsolutePath();
            will(returnValue(requestUriPath));
        }});

        VertexResource resource = new VertexResource(uri, httpServletRequest, rap);
        Response response = resource.postNullVertexRexsterConsumesJson(jsr311Request, "graph", jsonToPost);

        assertPostVertexProducesJson(response, false, false);
    }

    @Test
    public void postNullVertexRexsterConsumesTypedJsonAcceptJsonValid() {
        final HashMap<String, Object> parameters = generateVertexParametersToPost(true);

        final JSONObject jsonToPost = new JSONObject(parameters);

        final Graph graph = this.mockery.mock(Graph.class);
        final RexsterApplicationGraph rag = new RexsterApplicationGraph("graph", graph);
        final RexsterApplicationProvider rap = this.mockery.mock(RexsterApplicationProvider.class);

        final UriInfo uri = this.mockery.mock(UriInfo.class);
        final HttpServletRequest httpServletRequest = this.mockery.mock(HttpServletRequest.class);
        final Vertex v = new MockVertex("1");

        final Request jsr311Request = this.mockery.mock(Request.class);
        final Variant variantJson = new Variant(MediaType.APPLICATION_JSON_TYPE, null, null);

        this.mockery.checking(new Expectations() {{
            allowing(httpServletRequest).getParameterMap();
            will(returnValue(parameters));
            allowing(graph).getVertex(with(any(Object.class)));
            will(returnValue(null));
            allowing(graph).addVertex(with(any(Object.class)));
            will(returnValue(v));
            allowing(rap).getApplicationGraph(with(any(String.class)));
            will(returnValue(rag));
            allowing(jsr311Request).selectVariant(with(any(List.class)));
            will(returnValue(variantJson));
            allowing(uri).getAbsolutePath();
            will(returnValue(requestUriPath));
        }});

        VertexResource resource = new VertexResource(uri, httpServletRequest, rap);
        Response response = resource.postNullVertexRexsterConsumesTypedJson(jsr311Request, "graph", jsonToPost);

        assertPostVertexProducesJson(response, false, false);
    }

    @Test
    public void postNullVertexOnUriAcceptRexsterJsonValid() {
        final HashMap<String, Object> parameters = generateVertexParametersToPost(true);

        final Graph graph = this.mockery.mock(Graph.class);
        final RexsterApplicationGraph rag = new RexsterApplicationGraph("graph", graph);
        initializeExtensionConfigurations(rag);
        final RexsterApplicationProvider rap = this.mockery.mock(RexsterApplicationProvider.class);

        final UriInfo uri = this.mockery.mock(UriInfo.class);
        final HttpServletRequest httpServletRequest = this.mockery.mock(HttpServletRequest.class);
        final Vertex v = new MockVertex("1");

        final Request jsr311Request = this.mockery.mock(Request.class);
        final Variant variantJson = new Variant(RexsterMediaType.APPLICATION_REXSTER_JSON_TYPE, null, null);

        this.mockery.checking(new Expectations() {{
            allowing(httpServletRequest).getParameterMap();
            will(returnValue(parameters));
            allowing(graph).getVertex(with(any(Object.class)));
            will(returnValue(null));
            allowing(graph).addVertex(with(any(Object.class)));
            will(returnValue(v));
            allowing(rap).getApplicationGraph(with(any(String.class)));
            will(returnValue(rag));
            allowing(jsr311Request).selectVariant(with(any(List.class)));
            will(returnValue(variantJson));
            allowing(uri).getAbsolutePath();
            will(returnValue(requestUriPath));
        }});

        VertexResource resource = new VertexResource(uri, httpServletRequest, rap);
        Response response = resource.postNullVertexOnUri(jsr311Request, "graph");

        assertPostVertexProducesJson(response, true, false);
    }

    @Test
    public void postNullVertexRexsterConsumesJsonAcceptRexsterJsonValid() {
        final HashMap<String, Object> parameters = generateVertexParametersToPost(false);

        final JSONObject jsonToPost = new JSONObject(parameters);

        final Graph graph = this.mockery.mock(Graph.class);
        final RexsterApplicationGraph rag = new RexsterApplicationGraph("graph", graph);
        initializeExtensionConfigurations(rag);
        final RexsterApplicationProvider rap = this.mockery.mock(RexsterApplicationProvider.class);

        final UriInfo uri = this.mockery.mock(UriInfo.class);
        final HttpServletRequest httpServletRequest = this.mockery.mock(HttpServletRequest.class);
        final Vertex v = new MockVertex("1");

        final Request jsr311Request = this.mockery.mock(Request.class);
        final Variant variantJson = new Variant(RexsterMediaType.APPLICATION_REXSTER_JSON_TYPE, null, null);

        this.mockery.checking(new Expectations() {{
            allowing(httpServletRequest).getParameterMap();
            will(returnValue(parameters));
            allowing(graph).getVertex(with(any(Object.class)));
            will(returnValue(null));
            allowing(graph).addVertex(with(any(Object.class)));
            will(returnValue(v));
            allowing(rap).getApplicationGraph(with(any(String.class)));
            will(returnValue(rag));
            allowing(jsr311Request).selectVariant(with(any(List.class)));
            will(returnValue(variantJson));
            allowing(uri).getAbsolutePath();
            will(returnValue(requestUriPath));
        }});

        VertexResource resource = new VertexResource(uri, httpServletRequest, rap);
        Response response = resource.postNullVertexRexsterConsumesJson(jsr311Request, "graph", jsonToPost);

        assertPostVertexProducesJson(response, true, false);
    }

    @Test
    public void postNullVertexRexsterConsumesTypedJsonAcceptRexsterJsonValid() {
        final HashMap<String, Object> parameters = generateVertexParametersToPost(true);

        final JSONObject jsonToPost = new JSONObject(parameters);

        final Graph graph = this.mockery.mock(Graph.class);
        final RexsterApplicationGraph rag = new RexsterApplicationGraph("graph", graph);
        initializeExtensionConfigurations(rag);
        final RexsterApplicationProvider rap = this.mockery.mock(RexsterApplicationProvider.class);

        final UriInfo uri = this.mockery.mock(UriInfo.class);
        final HttpServletRequest httpServletRequest = this.mockery.mock(HttpServletRequest.class);
        final Vertex v = new MockVertex("1");

        final Request jsr311Request = this.mockery.mock(Request.class);
        final Variant variantJson = new Variant(RexsterMediaType.APPLICATION_REXSTER_JSON_TYPE, null, null);

        this.mockery.checking(new Expectations() {{
            allowing(httpServletRequest).getParameterMap();
            will(returnValue(parameters));
            allowing(graph).getVertex(with(any(Object.class)));
            will(returnValue(null));
            allowing(graph).addVertex(with(any(Object.class)));
            will(returnValue(v));
            allowing(rap).getApplicationGraph(with(any(String.class)));
            will(returnValue(rag));
            allowing(jsr311Request).selectVariant(with(any(List.class)));
            will(returnValue(variantJson));
            allowing(uri).getAbsolutePath();
            will(returnValue(requestUriPath));
        }});

        VertexResource resource = new VertexResource(uri, httpServletRequest, rap);
        Response response = resource.postNullVertexRexsterConsumesTypedJson(jsr311Request, "graph", jsonToPost);

        assertPostVertexProducesJson(response, true, false);
    }

    @Test
    public void postNullVertexOnUriAcceptRexsterTypedJsonValid() {
        final HashMap<String, Object> parameters = generateVertexParametersToPost(true);

        final Graph graph = this.mockery.mock(Graph.class);
        final RexsterApplicationGraph rag = new RexsterApplicationGraph("graph", graph);
        initializeExtensionConfigurations(rag);
        final RexsterApplicationProvider rap = this.mockery.mock(RexsterApplicationProvider.class);

        final UriInfo uri = this.mockery.mock(UriInfo.class);
        final HttpServletRequest httpServletRequest = this.mockery.mock(HttpServletRequest.class);
        final Vertex v = new MockVertex("1");

        final Request jsr311Request = this.mockery.mock(Request.class);
        final Variant variantJson = new Variant(RexsterMediaType.APPLICATION_REXSTER_TYPED_JSON_TYPE, null, null);

        this.mockery.checking(new Expectations() {{
            allowing(httpServletRequest).getParameterMap();
            will(returnValue(parameters));
            allowing(graph).getVertex(with(any(Object.class)));
            will(returnValue(null));
            allowing(graph).addVertex(with(any(Object.class)));
            will(returnValue(v));
            allowing(rap).getApplicationGraph(with(any(String.class)));
            will(returnValue(rag));
            allowing(jsr311Request).selectVariant(with(any(List.class)));
            will(returnValue(variantJson));
            allowing(uri).getAbsolutePath();
            will(returnValue(requestUriPath));
        }});

        VertexResource resource = new VertexResource(uri, httpServletRequest, rap);
        Response response = resource.postNullVertexOnUri(jsr311Request, "graph");

        assertPostVertexProducesJson(response, true, true);
    }

    @Test
    public void postNullVertexRexsterConsumesJsonAcceptRexsterTypedJsonValid() {
        final HashMap<String, Object> parameters = generateVertexParametersToPost(false);

        final JSONObject jsonToPost = new JSONObject(parameters);

        final Graph graph = this.mockery.mock(Graph.class);
        final RexsterApplicationGraph rag = new RexsterApplicationGraph("graph", graph);
        initializeExtensionConfigurations(rag);
        final RexsterApplicationProvider rap = this.mockery.mock(RexsterApplicationProvider.class);

        final UriInfo uri = this.mockery.mock(UriInfo.class);
        final HttpServletRequest httpServletRequest = this.mockery.mock(HttpServletRequest.class);
        final Vertex v = new MockVertex("1");

        final Request jsr311Request = this.mockery.mock(Request.class);
        final Variant variantJson = new Variant(RexsterMediaType.APPLICATION_REXSTER_TYPED_JSON_TYPE, null, null);

        this.mockery.checking(new Expectations() {{
            allowing(httpServletRequest).getParameterMap();
            will(returnValue(parameters));
            allowing(graph).getVertex(with(any(Object.class)));
            will(returnValue(null));
            allowing(graph).addVertex(with(any(Object.class)));
            will(returnValue(v));
            allowing(rap).getApplicationGraph(with(any(String.class)));
            will(returnValue(rag));
            allowing(jsr311Request).selectVariant(with(any(List.class)));
            will(returnValue(variantJson));
            allowing(uri).getAbsolutePath();
            will(returnValue(requestUriPath));
        }});

        VertexResource resource = new VertexResource(uri, httpServletRequest, rap);
        Response response = resource.postNullVertexRexsterConsumesJson(jsr311Request, "graph", jsonToPost);

        assertPostVertexProducesJson(response, true, true);
    }

    @Test
    public void postNullVertexRexsterConsumesTypedJsonAcceptRexsterTypedJsonValid() {
        final HashMap<String, Object> parameters = generateVertexParametersToPost(true);

        final JSONObject jsonToPost = new JSONObject(parameters);

        final Graph graph = this.mockery.mock(Graph.class);
        final RexsterApplicationGraph rag = new RexsterApplicationGraph("graph", graph);
        initializeExtensionConfigurations(rag);
        final RexsterApplicationProvider rap = this.mockery.mock(RexsterApplicationProvider.class);

        final UriInfo uri = this.mockery.mock(UriInfo.class);
        final HttpServletRequest httpServletRequest = this.mockery.mock(HttpServletRequest.class);
        final Vertex v = new MockVertex("1");

        final Request jsr311Request = this.mockery.mock(Request.class);
        final Variant variantJson = new Variant(RexsterMediaType.APPLICATION_REXSTER_TYPED_JSON_TYPE, null, null);

        this.mockery.checking(new Expectations() {{
            allowing(httpServletRequest).getParameterMap();
            will(returnValue(parameters));
            allowing(graph).getVertex(with(any(Object.class)));
            will(returnValue(null));
            allowing(graph).addVertex(with(any(Object.class)));
            will(returnValue(v));
            allowing(rap).getApplicationGraph(with(any(String.class)));
            will(returnValue(rag));
            allowing(jsr311Request).selectVariant(with(any(List.class)));
            will(returnValue(variantJson));
            allowing(uri).getAbsolutePath();
            will(returnValue(requestUriPath));
        }});

        VertexResource resource = new VertexResource(uri, httpServletRequest, rap);
        Response response = resource.postNullVertexRexsterConsumesTypedJson(jsr311Request, "graph", jsonToPost);

        assertPostVertexProducesJson(response, true, true);
    }

    @Test(expected = WebApplicationException.class)
    public void postVertexOnUriButHasElementProperties() {
        final HashMap<String, String> parameters = new HashMap<String, String>();
        parameters.put(Tokens._ID, "300");

        final Graph graph = this.mockery.mock(Graph.class);
        final RexsterApplicationGraph rag = new RexsterApplicationGraph("graph", graph);
        final RexsterApplicationProvider rap = this.mockery.mock(RexsterApplicationProvider.class);

        final UriInfo uri = this.mockery.mock(UriInfo.class);
        final HttpServletRequest httpServletRequest = this.mockery.mock(HttpServletRequest.class);
        final Vertex v = new MockVertex("1");

        final Request jsr311Request = this.mockery.mock(Request.class);
        final Variant variantJson = new Variant(MediaType.APPLICATION_JSON_TYPE, null, null);

        this.mockery.checking(new Expectations() {{
            allowing(httpServletRequest).getParameterMap();
            will(returnValue(parameters));
            allowing(graph).getVertex(with(any(Object.class)));
            will(returnValue(v));
            allowing(rap).getApplicationGraph(with(any(String.class)));
            will(returnValue(rag));
            allowing(jsr311Request).selectVariant(with(any(List.class)));
            will(returnValue(variantJson));
            allowing(uri).getAbsolutePath();
            will(returnValue(requestUriPath));
        }});

        VertexResource resource = new VertexResource(uri, httpServletRequest, rap);
        resource.postVertexOnUri(jsr311Request, "graph", "1");
    }

    @Test
    public void putVertexConsumesUriValid() {
        final HashMap<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("some-property", "300a");

        final Graph graph = this.mockery.mock(Graph.class);
        final RexsterApplicationGraph rag = new RexsterApplicationGraph("graph", graph);
        final RexsterApplicationProvider rap = this.mockery.mock(RexsterApplicationProvider.class);

        final UriInfo uri = this.mockery.mock(UriInfo.class);
        final HttpServletRequest httpServletRequest = this.mockery.mock(HttpServletRequest.class);
        final Vertex v = new MockVertex("1");
        v.setProperty("property-to-remove", "bye");

        final Request jsr311Request = this.mockery.mock(Request.class);
        final Variant variantJson = new Variant(MediaType.APPLICATION_JSON_TYPE, null, null);

        this.mockery.checking(new Expectations() {{
            allowing(httpServletRequest).getParameterMap();
            will(returnValue(parameters));
            allowing(graph).getVertex(with(any(Object.class)));
            will(returnValue(v));
            allowing(rap).getApplicationGraph(with(any(String.class)));
            will(returnValue(rag));
            allowing(jsr311Request).selectVariant(with(any(List.class)));
            will(returnValue(variantJson));
            allowing(uri).getAbsolutePath();
            will(returnValue(requestUriPath));
        }});

        VertexResource resource = new VertexResource(uri, httpServletRequest, rap);
        Response response = resource.putVertexConsumesUri(jsr311Request, "graph", "1");

        Assert.assertNotNull(response);
        Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assert.assertNotNull(response.getEntity());
        Assert.assertTrue(response.getEntity() instanceof JSONObject);

        JSONObject json = (JSONObject) response.getEntity();
        Assert.assertTrue(json.has(Tokens.QUERY_TIME));

        Assert.assertTrue(json.has(Tokens.RESULTS));
        Assert.assertFalse(json.isNull(Tokens.RESULTS));

        Assert.assertEquals("300a", v.getProperty("some-property"));

        Assert.assertFalse(v.getPropertyKeys().contains("property-to-remove"));
    }

    @Test(expected = WebApplicationException.class)
    public void putVertexConsumesUriNoVertexFound() {
        final HashMap<String, Object> parameters = generateVertexParametersToPost(false);

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
            allowing(rap).getApplicationGraph(with(any(String.class)));
            will(returnValue(rag));
            allowing(jsr311Request).selectVariant(with(any(List.class)));
            will(returnValue(variantJson));
            allowing(uri).getAbsolutePath();
            will(returnValue(requestUriPath));
        }});

        VertexResource resource = new VertexResource(uri, httpServletRequest, rap);
        resource.putVertexConsumesUri(jsr311Request, "graph", "1");

    }

    @Test
    public void deleteVertexValid() {
        final Graph graph = this.mockery.mock(Graph.class);
        final RexsterApplicationGraph rag = new RexsterApplicationGraph("graph", graph);
        final RexsterApplicationProvider rap = this.mockery.mock(RexsterApplicationProvider.class);

        final UriInfo uri = this.mockery.mock(UriInfo.class);
        final HttpServletRequest httpServletRequest = this.mockery.mock(HttpServletRequest.class);
        final Vertex v = new MockVertex("1");

        this.mockery.checking(new Expectations() {{
            allowing(httpServletRequest).getParameterMap();
            will(returnValue(new HashMap<String, String>()));
            allowing(graph).getVertex(with(any(Object.class)));
            will(returnValue(v));
            allowing(graph).removeVertex(v);
            allowing(rap).getApplicationGraph(with(any(String.class)));
            will(returnValue(rag));
            allowing(uri).getAbsolutePath();
            will(returnValue(requestUriPath));
        }});

        VertexResource resource = new VertexResource(uri, httpServletRequest, rap);
        Response response = resource.deleteVertex("graph", "1");

        Assert.assertNotNull(response);
        Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assert.assertNotNull(response.getEntity());
        Assert.assertTrue(response.getEntity() instanceof JSONObject);

        JSONObject json = (JSONObject) response.getEntity();
        Assert.assertTrue(json.has(Tokens.QUERY_TIME));
    }

    @Test(expected = WebApplicationException.class)
    public void deleteVertexNoVertexFound() {
        final Graph graph = this.mockery.mock(Graph.class);
        final RexsterApplicationGraph rag = new RexsterApplicationGraph("graph", graph);
        final RexsterApplicationProvider rap = this.mockery.mock(RexsterApplicationProvider.class);

        final UriInfo uri = this.mockery.mock(UriInfo.class);
        final HttpServletRequest httpServletRequest = this.mockery.mock(HttpServletRequest.class);

        this.mockery.checking(new Expectations() {{
            allowing(httpServletRequest).getParameterMap();
            will(returnValue(new HashMap<String, String>()));
            allowing(graph).getVertex(with(any(Object.class)));
            will(returnValue(null));
            allowing(rap).getApplicationGraph(with(any(String.class)));
            will(returnValue(rag));
            allowing(uri).getAbsolutePath();
            will(returnValue(requestUriPath));
        }});

        VertexResource resource = new VertexResource(uri, httpServletRequest, rap);
        resource.deleteVertex("graph", "1");
    }

    @Test
    public void deleteVertexPropertiesValid() {
        final HashMap<String, String> parameters = new HashMap<String, String>();
        parameters.put("some-property", "");

        final Graph graph = this.mockery.mock(Graph.class);
        final RexsterApplicationGraph rag = new RexsterApplicationGraph("graph", graph);
        final RexsterApplicationProvider rap = this.mockery.mock(RexsterApplicationProvider.class);

        final UriInfo uri = this.mockery.mock(UriInfo.class);
        final HttpServletRequest httpServletRequest = this.mockery.mock(HttpServletRequest.class);
        final Vertex v = new MockVertex("1");
        v.setProperty("some-property", "to-delete");

        this.mockery.checking(new Expectations() {{
            allowing(httpServletRequest).getParameterMap();
            will(returnValue(parameters));
            allowing(graph).getVertex(with(any(Object.class)));
            will(returnValue(v));
            allowing(rap).getApplicationGraph(with(any(String.class)));
            will(returnValue(rag));
            allowing(uri).getAbsolutePath();
            will(returnValue(requestUriPath));
        }});

        VertexResource resource = new VertexResource(uri, httpServletRequest, rap);
        Response response = resource.deleteVertex("graph", "1");

        Assert.assertNotNull(response);
        Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assert.assertNotNull(response.getEntity());
        Assert.assertTrue(response.getEntity() instanceof JSONObject);

        JSONObject json = (JSONObject) response.getEntity();
        Assert.assertTrue(json.has(Tokens.QUERY_TIME));

        Set<String> keys = v.getPropertyKeys();
        Assert.assertEquals(0, keys.size());
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

    private static void assertPostVertexProducesJson(Response response, boolean hasHypermedia, boolean hasTypes) {
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

    private VertexResource constructMockMultiEdgedGraphScenario(int numberOfEdgesToGenerate, HashMap<String, String> parameters) {
        MockVertex v1 = new MockVertex("1");
        MockVertex v2 = new MockVertex("2");

        ArrayList<Edge> v1InEdges = new ArrayList<Edge>();
        ArrayList<Edge> v2OutEdges = new ArrayList<Edge>();

        for (int ix = 0; ix < numberOfEdgesToGenerate; ix++) {
            MockEdge edge = new MockEdge(new Integer(ix).toString(), "label" + new Integer(ix).toString(), new Hashtable<String, Object>(), v1, v2);

            v1InEdges.add(edge);
            v2OutEdges.add(edge);
        }

        v1.setInEdges(v1InEdges);
        v2.setOutEdges(v2OutEdges);

        VertexResource resource = this.constructMockGetSingleVertexScenario(v1, parameters);
        return resource;
    }

    private VertexResource constructMockSimpleGraphScenario() {
        return constructMockSimpleGraphScenario(new HashMap<String, String>());
    }

    /**
     * Creates a simple graph with two vertices and an edge between them.
     *
     * @return
     */
    private VertexResource constructMockSimpleGraphScenario(HashMap parameters) {
        MockVertex v1 = new MockVertex("1");
        MockVertex v2 = new MockVertex("2");
        MockVertex v3 = new MockVertex("3");

        MockEdge edge1 = new MockEdge("1-2", "label12", new Hashtable<String, Object>(), v1, v2);
        MockEdge edge2 = new MockEdge("3-1", "label31", new Hashtable<String, Object>(), v3, v1);

        ArrayList<Edge> v1InEdges = new ArrayList<Edge>();
        v1InEdges.add(edge1);

        ArrayList<Edge> v3InEdges = new ArrayList<Edge>();
        v3InEdges.add(edge2);

        ArrayList<Edge> v1OutEdges = new ArrayList<Edge>();
        v1OutEdges.add(edge2);

        ArrayList<Edge> v2OutEdges = new ArrayList<Edge>();
        v2OutEdges.add(edge1);

        v1.setInEdges(v1InEdges);
        v1.setOutEdges(v1OutEdges);
        v2.setOutEdges(v2OutEdges);
        v3.setInEdges(v3InEdges);

        VertexResource resource = this.constructMockGetSingleVertexScenario(v1, parameters);
        return resource;
    }

    private void assertVerticesOkResponseJsonStructure(int numberOfVerticesReturned, int numberOfVerticesTotal, Response response) {
        Assert.assertNotNull(response);
        Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assert.assertNotNull(response.getEntity());
        Assert.assertTrue(response.getEntity() instanceof JSONObject);

        JSONObject json = (JSONObject) response.getEntity();
        Assert.assertTrue(json.has(Tokens.TOTAL_SIZE));
        //TODO Assert.assertEquals(numberOfVerticesTotal, json.optInt(Tokens.TOTAL_SIZE));
        Assert.assertTrue(json.has(Tokens.QUERY_TIME));

        Assert.assertTrue(json.has(Tokens.RESULTS));
        Assert.assertFalse(json.isNull(Tokens.RESULTS));

        JSONArray jsonResults = json.optJSONArray(Tokens.RESULTS);
        Assert.assertEquals(numberOfVerticesReturned, jsonResults.length());
    }

    private VertexResource constructMockGetSingleVertexScenario(final Vertex vertex, final HashMap<String, String> parameters) {
        final Graph graph = this.mockery.mock(Graph.class);
        final RexsterApplicationGraph rag = new RexsterApplicationGraph("graph", graph);
        final RexsterApplicationProvider rap = this.mockery.mock(RexsterApplicationProvider.class);

        List<String> namespaces = new ArrayList<String>();
        namespaces.add("*:*");
        rag.loadAllowableExtensions(namespaces);

        final UriInfo uri = this.mockery.mock(UriInfo.class);

        final HttpServletRequest httpServletRequest = this.mockery.mock(HttpServletRequest.class);

        this.mockery.checking(new Expectations() {{
            allowing(httpServletRequest).getParameterMap();
            will(returnValue(parameters));
            allowing(graph).getVertex(with(any(Object.class)));
            will(returnValue(vertex));
            allowing(rap).getApplicationGraph(with(any(String.class)));
            will(returnValue(rag));
            allowing(uri).getAbsolutePath();
            will(returnValue(requestUriPath));
        }});

        VertexResource resource = new VertexResource(uri, httpServletRequest, rap);
        return resource;
    }

    private VertexResource constructMockGetVerticesScenario(final int numberOfVertices) {
        return this.constructMockGetVerticesScenario(numberOfVertices, new HashMap<String, String>());
    }

    private VertexResource constructMockGetVerticesScenario(final int numberOfVertices, final HashMap<String, String> parameters) {
        final Graph graph = this.mockery.mock(Graph.class);
        final RexsterApplicationGraph rag = new RexsterApplicationGraph("graph", graph);
        final RexsterApplicationProvider rap = this.mockery.mock(RexsterApplicationProvider.class);

        final UriInfo uri = this.mockery.mock(UriInfo.class);

        final HttpServletRequest httpServletRequest = this.mockery.mock(HttpServletRequest.class);

        this.mockery.checking(new Expectations() {{
            allowing(httpServletRequest).getParameterMap();
            will(returnValue(parameters));
            allowing(graph).getVertices();
            will(returnValue(generateMockedVertices(numberOfVertices)));
            allowing(rap).getApplicationGraph(with(any(String.class)));
            will(returnValue(rag));
            allowing(uri).getAbsolutePath();
            will(returnValue(requestUriPath));
        }});

        VertexResource resource = new VertexResource(uri, httpServletRequest, rap);
        return resource;
    }

    private static Iterable<Vertex> generateMockedVertices(int numberOfVertices) {
        ArrayList<Vertex> vertices = new ArrayList<Vertex>();

        for (int ix = 0; ix < numberOfVertices; ix++) {
            MockVertex v = new MockVertex(new Integer(ix).toString());
            vertices.add(v);
        }

        return vertices;
    }
}
