package com.tinkerpop.rexster;

import com.tinkerpop.blueprints.pgm.Graph;
import org.codehaus.jettison.json.JSONObject;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import java.util.HashMap;

public class GraphResourceTest {
    protected Mockery mockery = new JUnit4Mockery();
    protected final String baseUri = "http://localhost/mock";

    @Before
    public void init() {
        this.mockery = new JUnit4Mockery();
    }

    @Test
    public void getGraphValid() {
        GraphResource resource = this.constructMockGetGraphScenario(new HashMap<String, String>());
        Response response = resource.getGraph("graph");

        Assert.assertNotNull(response);
        Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assert.assertNotNull(response.getEntity());
        Assert.assertTrue(response.getEntity() instanceof JSONObject);

        JSONObject json = (JSONObject) response.getEntity();
        Assert.assertTrue(json.has("name"));
        Assert.assertEquals("graph", json.optString("name"));
        Assert.assertTrue(json.has(Tokens.QUERY_TIME));
        Assert.assertTrue(json.optDouble(Tokens.QUERY_TIME) > 0);
        Assert.assertTrue(json.has("up_time"));
        Assert.assertTrue(json.has("version"));

    }

    @Test
    public void deleteGraphValid() {
        final Graph graph = this.mockery.mock(Graph.class);
        final RexsterApplicationGraph rag = new RexsterApplicationGraph("graph", graph);

        final UriInfo uri = this.mockery.mock(UriInfo.class);

        final RexsterApplicationProvider rap = this.mockery.mock(RexsterApplicationProvider.class);
        final HttpServletRequest httpServletRequest = this.mockery.mock(HttpServletRequest.class);

        this.mockery.checking(new Expectations() {{
            allowing(httpServletRequest).getParameterMap();
            will(returnValue(new HashMap<String, String>()));
            allowing(rap).getApplicationGraph(with(any(String.class)));
            will(returnValue(rag));
            allowing(graph).clear();
        }});

        GraphResource resource = new GraphResource(uri, httpServletRequest, rap);
        Response response = resource.deleteGraph("graph");

        Assert.assertNotNull(response);
        Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assert.assertNotNull(response.getEntity());
        Assert.assertTrue(response.getEntity() instanceof JSONObject);

        JSONObject json = (JSONObject) response.getEntity();
        Assert.assertTrue(json.has(Tokens.QUERY_TIME));
        Assert.assertTrue(json.optDouble(Tokens.QUERY_TIME) > 0);
    }

    private GraphResource constructMockGetGraphScenario(final HashMap<String, String> parameters) {
        final Graph graph = this.mockery.mock(Graph.class);
        final RexsterApplicationGraph rag = new RexsterApplicationGraph("graph", graph);

        final UriInfo uri = this.mockery.mock(UriInfo.class);

        final RexsterApplicationProvider rap = this.mockery.mock(RexsterApplicationProvider.class);
        final HttpServletRequest httpServletRequest = this.mockery.mock(HttpServletRequest.class);

        this.mockery.checking(new Expectations() {{
            allowing(httpServletRequest).getParameterMap();
            will(returnValue(parameters));
            allowing(rap).getApplicationGraph(with(any(String.class)));
            will(returnValue(rag));
            allowing(rap).getStartTime();
            will(returnValue(System.currentTimeMillis() - 10000));
        }});

        GraphResource resource = new GraphResource(uri, httpServletRequest, rap);
        return resource;
    }
}