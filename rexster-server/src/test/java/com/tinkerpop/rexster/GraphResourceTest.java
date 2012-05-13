package com.tinkerpop.rexster;

import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.impls.tg.TinkerGraph;
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
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class GraphResourceTest {
    protected Mockery mockery = new JUnit4Mockery();
    protected final String baseUri = "http://localhost/mock";
    protected final URI requestUriPath = URI.create("http://localhost/graphs/mock");

    @Before
    public void init() {
        this.mockery = new JUnit4Mockery();
    }

    @Test
    public void getGraphProducesJsonValid() {
        GraphResource resource = this.constructMockGetGraphScenario(new HashMap<String, String>());
        Response response = resource.getGraphProducesJson("graph");

        Assert.assertNotNull(response);
        Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assert.assertNotNull(response.getEntity());
        Assert.assertTrue(response.getEntity() instanceof JSONObject);

        JSONObject json = (JSONObject) response.getEntity();
        Assert.assertTrue(json.has("name"));
        Assert.assertEquals("graph", json.optString("name"));
        Assert.assertTrue(json.has(Tokens.QUERY_TIME));
        Assert.assertTrue(json.has(Tokens.UP_TIME));
        Assert.assertTrue(json.has(Tokens.READ_ONLY));
        Assert.assertTrue(json.has(Tokens.VERSION));
        Assert.assertTrue(json.has(Tokens.TYPE));
        Assert.assertTrue(json.has(Tokens.FEATURES));

    }

    private GraphResource constructMockGetGraphScenario(final HashMap<String, String> parameters) {
        final Graph graph = this.mockery.mock(Graph.class);
        final RexsterApplicationGraph rag = new RexsterApplicationGraph("graph", graph);

        List<String> namespaces = new ArrayList<String>();
        namespaces.add("*:*");
        rag.loadAllowableExtensions(namespaces);

        final UriInfo uri = this.mockery.mock(UriInfo.class);

        final RexsterApplication ra = this.mockery.mock(RexsterApplication.class);
        final HttpServletRequest httpServletRequest = this.mockery.mock(HttpServletRequest.class);

        this.mockery.checking(new Expectations() {{
            allowing(httpServletRequest).getParameterMap();
            will(returnValue(parameters));
            allowing(ra).getApplicationGraph(with(any(String.class)));
            will(returnValue(rag));
            allowing(ra).getStartTime();
            will(returnValue(System.currentTimeMillis() - 10000));
            allowing(uri).getAbsolutePath();
            will(returnValue(requestUriPath));
            allowing(graph).getFeatures();
            will(returnValue(new TinkerGraph().getFeatures()));
        }});

        GraphResource resource = new GraphResource(uri, httpServletRequest, ra);
        return resource;
    }
}