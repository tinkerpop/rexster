package com.tinkerpop.rexster;

import org.codehaus.jettison.json.JSONArray;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class RexsterResourceTest {
    protected Mockery mockery = new JUnit4Mockery();
    protected final String baseUri = "http://localhost/mock";

    @Before
    public void init() {
        this.mockery = new JUnit4Mockery();
    }

    @Test
    public void evaluateMultipleGraphs() {
        final RexsterApplication ra = this.mockery.mock(RexsterApplication.class);
        final HttpServletRequest httpServletRequest = this.mockery.mock(HttpServletRequest.class);
        final Set<String> graphNames = new HashSet<String>();
        graphNames.add("graph1");
        graphNames.add("graph2");
        graphNames.add("graph3");

        final long startTime = System.currentTimeMillis() - 1000;

        this.mockery.checking(new Expectations() {{
            allowing(httpServletRequest).getParameterMap();
            will(returnValue(new HashMap<String, String>()));
            allowing(ra).getGraphNames();
            will(returnValue(graphNames));
            allowing(ra).getStartTime();
            will(returnValue(startTime));
        }});

        RexsterResource resource = new RexsterResource(ra);
        Response response = resource.getRexsterRoot();

        Assert.assertNotNull(response);
        Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assert.assertNotNull(response.getEntity());
        Assert.assertTrue(response.getEntity() instanceof JSONObject);

        JSONObject json = (JSONObject) response.getEntity();
        Assert.assertTrue(json.has(Tokens.QUERY_TIME));
        Assert.assertTrue(json.has("name"));
        Assert.assertTrue(json.has("graphs"));
        Assert.assertTrue(json.has(Tokens.UP_TIME));
        Assert.assertNotNull(json.optJSONArray("graphs"));

        JSONArray jsonArray = json.optJSONArray("graphs");
        Assert.assertEquals(3, jsonArray.length());
    }

    @Test
    public void evaluateNoGraphs() {
        final RexsterApplication ra = this.mockery.mock(RexsterApplication.class);
        final HttpServletRequest httpServletRequest = this.mockery.mock(HttpServletRequest.class);
        final Set<String> graphNames = new HashSet<String>();

        final long startTime = System.currentTimeMillis() - 1000;

        this.mockery.checking(new Expectations() {{
            allowing(httpServletRequest).getParameterMap();
            will(returnValue(new HashMap<String, String>()));
            allowing(ra).getGraphNames();
            will(returnValue(graphNames));
            allowing(ra).getStartTime();
            will(returnValue(startTime));
        }});

        RexsterResource resource = new RexsterResource(ra);
        Response response = resource.getRexsterRoot();

        Assert.assertNotNull(response);
        Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assert.assertNotNull(response.getEntity());
        Assert.assertTrue(response.getEntity() instanceof JSONObject);

        JSONObject json = (JSONObject) response.getEntity();
        Assert.assertTrue(json.has(Tokens.QUERY_TIME));
        Assert.assertTrue(json.has("name"));
        Assert.assertTrue(json.has("graphs"));
        Assert.assertTrue(json.has(Tokens.UP_TIME));
        Assert.assertNotNull(json.optJSONArray("graphs"));

        JSONArray jsonArray = json.optJSONArray("graphs");
        Assert.assertEquals(0, jsonArray.length());
    }
}
