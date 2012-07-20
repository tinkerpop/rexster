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
            String id = testGraph.getVertexIdSet().values().iterator().next();
            ClientResponse graphResponse = doGraphGet(testGraph, "vertices/" + encode(id));

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
            if (testGraph.getFeatures().supportsVertexIteration) {
                ClientResponse graphResponse = doGraphGet(testGraph, "vertices");

                Assert.assertNotNull(graphResponse);
                Assert.assertEquals(ClientResponse.Status.OK, graphResponse.getClientResponseStatus());

                JSONObject vertexJson = graphResponse.getEntity(JSONObject.class);
                Assert.assertNotNull(vertexJson);

                Assert.assertEquals(6, vertexJson.optJSONArray(Tokens.RESULTS).length());
            }
        }
    }

    @Test
    public void getVerticesPagingStatusOk() {
        for (GraphTestHolder testGraph : this.testGraphs) {
            if (testGraph.getFeatures().supportsVertexIteration) {
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

    @Test
    public void getVerticesQueryStatusOk() {
        for (GraphTestHolder testGraph : this.testGraphs) {
            final String id = testGraph.getVertexIdSet().get("1");

            // get out vertices
            ClientResponse graphResponse = doGraphGet(testGraph, "vertices/" + encode(id) + "/out");

            Assert.assertNotNull(graphResponse);
            Assert.assertEquals(ClientResponse.Status.OK, graphResponse.getClientResponseStatus());

            JSONObject json = graphResponse.getEntity(JSONObject.class);
            Assert.assertNotNull(json);

            Assert.assertEquals(3, json.optJSONArray(Tokens.RESULTS).length());

            // get out edges
            graphResponse = doGraphGet(testGraph, "vertices/" + encode(id) + "/outE");
            Assert.assertEquals(Tokens.VERTEX, json.optJSONArray(Tokens.RESULTS).optJSONObject(0).optString(Tokens._TYPE));

            Assert.assertNotNull(graphResponse);
            Assert.assertEquals(ClientResponse.Status.OK, graphResponse.getClientResponseStatus());

            json = graphResponse.getEntity(JSONObject.class);
            Assert.assertNotNull(json);

            Assert.assertEquals(3, json.optJSONArray(Tokens.RESULTS).length());
            Assert.assertEquals(Tokens.EDGE, json.optJSONArray(Tokens.RESULTS).optJSONObject(0).optString(Tokens._TYPE));

            // get out vertices filtered by label
            graphResponse = doGraphGet(testGraph, "vertices/" + encode(id) + "/out", "_label=[knows]");

            Assert.assertNotNull(graphResponse);
            Assert.assertEquals(ClientResponse.Status.OK, graphResponse.getClientResponseStatus());

            json = graphResponse.getEntity(JSONObject.class);
            Assert.assertNotNull(json);

            Assert.assertEquals(2, json.optJSONArray(Tokens.RESULTS).length());
            Assert.assertEquals(Tokens.VERTEX, json.optJSONArray(Tokens.RESULTS).optJSONObject(0).optString(Tokens._TYPE));

            // get out vertices filtered by label and limited
            graphResponse = doGraphGet(testGraph, "vertices/" + encode(id) + "/out", "_label=[knows]&_limit=1");

            Assert.assertNotNull(graphResponse);
            Assert.assertEquals(ClientResponse.Status.OK, graphResponse.getClientResponseStatus());

            json = graphResponse.getEntity(JSONObject.class);
            Assert.assertNotNull(json);

            Assert.assertEquals(1, json.optJSONArray(Tokens.RESULTS).length());

            // get out vertices filtered by label and filtered by property (we lose "float" in graph creation)
            graphResponse = doGraphGet(testGraph, "vertices/" + encode(id) + "/out", "_label=[knows]&_properties=[[weight,=,(d,1)]]");

            Assert.assertNotNull(graphResponse);
            Assert.assertEquals(ClientResponse.Status.OK, graphResponse.getClientResponseStatus());

            json = graphResponse.getEntity(JSONObject.class);
            Assert.assertNotNull(json);

            Assert.assertEquals(1, json.optJSONArray(Tokens.RESULTS).length());

            // get out count filtered by label counted
            graphResponse = doGraphGet(testGraph, "vertices/" + encode(id) + "/outCount", "_label=[knows]");

            Assert.assertNotNull(graphResponse);
            Assert.assertEquals(ClientResponse.Status.OK, graphResponse.getClientResponseStatus());

            json = graphResponse.getEntity(JSONObject.class);
            Assert.assertNotNull(json);

            Assert.assertEquals(2, json.optInt(Tokens.TOTAL_SIZE));

            // get out vertex ids filtered by label and weight
            graphResponse = doGraphGet(testGraph, "vertices/" + encode(id) + "/outIds", "_label=[knows]&_properties=[[weight,=,(d,1)]]");

            Assert.assertNotNull(graphResponse);
            Assert.assertEquals(ClientResponse.Status.OK, graphResponse.getClientResponseStatus());

            json = graphResponse.getEntity(JSONObject.class);
            Assert.assertNotNull(json);

            Assert.assertEquals(1, json.optJSONArray(Tokens.RESULTS).length());

            final String thisGuy = testGraph.getVertexIdSet().get("4");
            Assert.assertEquals(thisGuy, json.optJSONArray(Tokens.RESULTS).optString(0));
        }
    }
}
