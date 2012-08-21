package com.tinkerpop.rexster;

import com.sun.jersey.api.client.ClientRequest;
import com.sun.jersey.api.client.ClientResponse;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

public class GraphResourceIntegrationTest extends AbstractGraphResourceIntegrationTest {

    public GraphResourceIntegrationTest() throws Exception {
        super();
    }

    @Test
    public void getGraph() {
        for (GraphTestHolder tg : this.testGraphs) {
            ClientRequest request = ClientRequest.create().build(createUri("/" + tg.getGraphName()), "GET");
            ClientResponse response = this.client.handle(request);

            Assert.assertNotNull(response);
            Assert.assertEquals(ClientResponse.Status.OK, response.getClientResponseStatus());

            JSONObject json = response.getEntity(JSONObject.class);
            Assert.assertNotNull(json);
            Assert.assertTrue(json.has("name"));
            Assert.assertEquals(tg.getGraphName(), json.optString("name"));
            Assert.assertTrue(json.has(Tokens.QUERY_TIME));
            Assert.assertTrue(json.optDouble(Tokens.QUERY_TIME) > 0);
            Assert.assertTrue(json.has(Tokens.UP_TIME));
            Assert.assertTrue(json.has(Tokens.READ_ONLY));
            Assert.assertTrue(json.has("version"));
            Assert.assertTrue(json.has("type"));
            Assert.assertEquals(tg.getGraphType(), json.optString("type"));
        }
    }
}
