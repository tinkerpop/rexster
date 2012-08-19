package com.tinkerpop.rexster;

import org.codehaus.jettison.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class GraphResourceTest extends BaseTest {

    @Test
    public void getGraphProducesJsonValid() {
        final GraphResource resource = constructGraphResourceWithToyGraph().getResource();
        final Response response = resource.getGraphProducesJson(graphName);

        Assert.assertNotNull(response);
        Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assert.assertNotNull(response.getEntity());
        Assert.assertTrue(response.getEntity() instanceof JSONObject);

        final JSONObject json = (JSONObject) response.getEntity();
        Assert.assertEquals(graphName, json.optString("name"));
        Assert.assertTrue(json.has(Tokens.QUERY_TIME));
        Assert.assertTrue(json.has(Tokens.UP_TIME));
        Assert.assertTrue(json.has(Tokens.READ_ONLY));
        Assert.assertTrue(json.has(Tokens.VERSION));
        Assert.assertTrue(json.has(Tokens.TYPE));
        Assert.assertTrue(json.has(Tokens.FEATURES));

    }
}