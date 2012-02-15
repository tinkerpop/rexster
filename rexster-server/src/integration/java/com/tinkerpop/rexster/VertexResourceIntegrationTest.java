package com.tinkerpop.rexster;

import com.sun.jersey.api.client.ClientResponse;
import junit.framework.Assert;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;

import java.util.ArrayList;

public class VertexResourceIntegrationTest extends AbstractGraphResourceIntegrationTest {
    public VertexResourceIntegrationTest() throws Exception {
        super();
    }

    @Test
    public void getVertexDoesNotExistStatusNotFound() {
        for (GraphTestHolder testGraph : this.testGraphs) {
            ClientResponse graphResponse = doGraphGet(testGraph, "vertices/123doesnotexist");

            Assert.assertNotNull(graphResponse);
            Assert.assertEquals(ClientResponse.Status.NOT_FOUND, graphResponse.getClientResponseStatus());
        }
    }

    @Test
    public void getVertexFoundStatusOk() {
        for (GraphTestHolder testGraph : this.testGraphs) {
            String id = testGraph.getVertexIdSet().keySet().iterator().next();
            ClientResponse graphResponse = doGraphGet(testGraph, "vertices/" + id);

            Assert.assertNotNull(graphResponse);
            Assert.assertEquals(ClientResponse.Status.OK, graphResponse.getClientResponseStatus());

            JSONObject vertexJson = graphResponse.getEntity(JSONObject.class);
            Assert.assertNotNull(vertexJson);

            JSONObject results = vertexJson.optJSONObject(Tokens.RESULTS);
            Assert.assertEquals(id, results.optString(Tokens._ID));

        }
    }

    @Test
    public void getVerticesAllFoundStatusOk() {
        for (GraphTestHolder testGraph : this.testGraphs) {
            ClientResponse graphResponse = doGraphGet(testGraph, "vertices");

            Assert.assertNotNull(graphResponse);
            Assert.assertEquals(ClientResponse.Status.OK, graphResponse.getClientResponseStatus());

            JSONObject vertexJson = graphResponse.getEntity(JSONObject.class);
            Assert.assertNotNull(vertexJson);

            Assert.assertEquals(6, vertexJson.optJSONArray(Tokens.RESULTS).length());
        }
    }

    @Test
    public void getVerticesPagingStatusOk() {
        for (GraphTestHolder testGraph : this.testGraphs) {

            ArrayList<String> uniqueIds = new ArrayList<String>();

            // get the first two elements
            ClientResponse graphResponse = doGraphGet(testGraph, "vertices", "rexster.offset.start=0&rexster.offset.end=2");

            Assert.assertNotNull(graphResponse);
            Assert.assertEquals(ClientResponse.Status.OK, graphResponse.getClientResponseStatus());

            JSONObject vertexJson = graphResponse.getEntity(JSONObject.class);
            Assert.assertNotNull(vertexJson);

            JSONArray results = vertexJson.optJSONArray(Tokens.RESULTS);
            Assert.assertEquals(2, results.length());

            uniqueIds.add(results.optJSONObject(0).optString(Tokens._ID));

            Assert.assertFalse(uniqueIds.contains(results.optJSONObject(1).optString(Tokens._ID)));
            uniqueIds.add(results.optJSONObject(1).optString(Tokens._ID));

            // get the next two elements
            graphResponse = doGraphGet(testGraph, "vertices", "rexster.offset.start=2&rexster.offset.end=4");

            Assert.assertNotNull(graphResponse);
            Assert.assertEquals(ClientResponse.Status.OK, graphResponse.getClientResponseStatus());

            vertexJson = graphResponse.getEntity(JSONObject.class);
            Assert.assertNotNull(vertexJson);

            results = vertexJson.optJSONArray(Tokens.RESULTS);
            Assert.assertEquals(2, results.length());

            Assert.assertFalse(uniqueIds.contains(results.optJSONObject(1).optString(Tokens._ID)));
            uniqueIds.add(results.optJSONObject(0).optString(Tokens._ID));

            Assert.assertFalse(uniqueIds.contains(results.optJSONObject(1).optString(Tokens._ID)));
            uniqueIds.add(results.optJSONObject(1).optString(Tokens._ID));

            // get the final two elements
            graphResponse = doGraphGet(testGraph, "vertices", "rexster.offset.start=4&rexster.offset.end=6");

            Assert.assertNotNull(graphResponse);
            Assert.assertEquals(ClientResponse.Status.OK, graphResponse.getClientResponseStatus());

            vertexJson = graphResponse.getEntity(JSONObject.class);
            Assert.assertNotNull(vertexJson);

            results = vertexJson.optJSONArray(Tokens.RESULTS);
            Assert.assertEquals(2, results.length());

            Assert.assertFalse(uniqueIds.contains(results.optJSONObject(1).optString(Tokens._ID)));
            uniqueIds.add(results.optJSONObject(0).optString(Tokens._ID));

            Assert.assertFalse(uniqueIds.contains(results.optJSONObject(1).optString(Tokens._ID)));
            uniqueIds.add(results.optJSONObject(1).optString(Tokens._ID));

            // get the final two elements without specifying the end parameter
            graphResponse = doGraphGet(testGraph, "vertices", "rexster.offset.start=4");

            Assert.assertNotNull(graphResponse);
            Assert.assertEquals(ClientResponse.Status.OK, graphResponse.getClientResponseStatus());

            vertexJson = graphResponse.getEntity(JSONObject.class);
            Assert.assertNotNull(vertexJson);

            results = vertexJson.optJSONArray(Tokens.RESULTS);
            Assert.assertEquals(2, results.length());

            Assert.assertEquals(uniqueIds.get(4), results.optJSONObject(0).optString(Tokens._ID));
            Assert.assertEquals(uniqueIds.get(5), results.optJSONObject(1).optString(Tokens._ID));

            // get the first two elements without specifying the start parameter
            graphResponse = doGraphGet(testGraph, "vertices", "rexster.offset.end=2");

            Assert.assertNotNull(graphResponse);
            Assert.assertEquals(ClientResponse.Status.OK, graphResponse.getClientResponseStatus());

            vertexJson = graphResponse.getEntity(JSONObject.class);
            Assert.assertNotNull(vertexJson);

            results = vertexJson.optJSONArray(Tokens.RESULTS);
            Assert.assertEquals(2, results.length());

            Assert.assertEquals(uniqueIds.get(0), results.optJSONObject(0).optString(Tokens._ID));
            Assert.assertEquals(uniqueIds.get(1), results.optJSONObject(1).optString(Tokens._ID));
        }
    }
}
