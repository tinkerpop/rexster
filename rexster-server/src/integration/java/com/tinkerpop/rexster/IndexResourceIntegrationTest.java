package com.tinkerpop.rexster;

import com.sun.jersey.api.client.ClientResponse;
import junit.framework.Assert;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;

public class IndexResourceIntegrationTest extends AbstractGraphResourceIntegrationTest {
    public IndexResourceIntegrationTest() throws Exception {
        super();
    }

    @Test
    public void postIndexManualSucceed() {
        for (GraphTestHolder testGraph : this.testGraphs) {
            ClientResponse indexResponse = doGraphPost(testGraph, "indices/newindex", "class=vertex");

            Assert.assertNotNull(indexResponse);
            Assert.assertEquals(ClientResponse.Status.OK, indexResponse.getClientResponseStatus());
        }
    }

    @Test
    public void putElementInIndexIndexNotFound() {
        for (GraphTestHolder testGraph : this.testGraphs) {
            ClientResponse indexResponse = doGraphPut(testGraph, "indices/newindex", "key=x&value=y");

            Assert.assertNotNull(indexResponse);
            Assert.assertEquals(ClientResponse.Status.NOT_FOUND, indexResponse.getClientResponseStatus());
        }
    }

    @Test
    public void putElementInIndexValid() {
        for (GraphTestHolder testGraph : this.testGraphs) {
            doGraphPost(testGraph, "indices/newindex", "class=vertex&keys=name");

            String mappedId = testGraph.getVertexIdSet().get("1");

            ClientResponse indexPutResponse = doGraphPut(testGraph, "indices/newindex", "key=name&value=marko&id=" + encode(mappedId));

            Assert.assertNotNull(indexPutResponse);
            Assert.assertEquals(ClientResponse.Status.OK, indexPutResponse.getClientResponseStatus());

            ClientResponse indexGetResponse = doGraphGet(testGraph, "indices/newindex", "key=name&value=marko");
            Assert.assertNotNull(indexGetResponse);
            Assert.assertEquals(ClientResponse.Status.OK, indexGetResponse.getClientResponseStatus());

            JSONObject result = indexGetResponse.getEntity(JSONObject.class);
            Assert.assertNotNull(result);

            JSONArray results = result.optJSONArray("results");
            Assert.assertNotNull(results);
            Assert.assertEquals(1, results.length());

            JSONObject marko = results.optJSONObject(0);
            Assert.assertEquals(mappedId, marko.optString("_id"));
            Assert.assertEquals("marko", marko.optString("name"));

        }
    }

    @Test
    public void deleteElementInIndexThenIndexItself() {
        for (GraphTestHolder testGraph : this.testGraphs) {
            doGraphPost(testGraph, "indices/newindex", "class=vertex&keys=name");

            String mappedId = testGraph.getVertexIdSet().get("1");

            ClientResponse indexPutResponse = doGraphPut(testGraph, "indices/newindex", "key=name&value=marko&id=" + encode(mappedId));

            ClientResponse indexGetResponse = doGraphGet(testGraph, "indices/newindex", "key=name&value=marko");
            Assert.assertNotNull(indexGetResponse);
            Assert.assertEquals(ClientResponse.Status.OK, indexGetResponse.getClientResponseStatus());

            JSONObject result = indexGetResponse.getEntity(JSONObject.class);
            Assert.assertNotNull(result);

            JSONArray results = result.optJSONArray("results");
            Assert.assertNotNull(results);
            Assert.assertEquals(1, results.length());

            JSONObject marko = results.optJSONObject(0);
            Assert.assertEquals(mappedId, marko.optString("_id"));
            Assert.assertEquals("marko", marko.optString("name"));

            doGraphDelete(testGraph, "indices/newindex", "key=name&value=marko&id=" + encode(mappedId));

            indexGetResponse = doGraphGet(testGraph, "indices/newindex", "key=name&value=marko");
            Assert.assertNotNull(indexGetResponse);
            Assert.assertEquals(ClientResponse.Status.OK, indexGetResponse.getClientResponseStatus());

            result = indexGetResponse.getEntity(JSONObject.class);
            Assert.assertNotNull(result);

            results = result.optJSONArray("results");
            Assert.assertNotNull(results);
            Assert.assertEquals(0, results.length());

            doGraphDelete(testGraph, "indices/newindex");
            ClientResponse indexDeleteResponse = doGraphDelete(testGraph, "indices/newindex");

            Assert.assertNotNull(indexDeleteResponse);
            Assert.assertEquals(ClientResponse.Status.NOT_FOUND, indexDeleteResponse.getClientResponseStatus());
        }
    }
}
