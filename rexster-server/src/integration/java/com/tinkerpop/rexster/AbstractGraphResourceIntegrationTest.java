package com.tinkerpop.rexster;

import com.sun.jersey.api.client.ClientRequest;
import com.sun.jersey.api.client.ClientResponse;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.tg.TinkerGraph;
import com.tinkerpop.blueprints.impls.tg.TinkerGraphFactory;
import com.tinkerpop.blueprints.util.io.graphson.GraphSONUtility;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Before;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public abstract class AbstractGraphResourceIntegrationTest extends AbstractResourceIntegrationTest {

    protected List<GraphTestHolder> testGraphs;

    public AbstractGraphResourceIntegrationTest() throws Exception {
        super();
    }

    @Before
    public void setUp() throws JSONException {
        ClientRequest request = ClientRequest.create().build(createUri("/"), "GET");
        ClientResponse response = this.client().handle(request);

        JSONObject json = response.getEntity(JSONObject.class);
        JSONArray jsonArray = json.optJSONArray("graphs");

        TinkerGraph testGraph = TinkerGraphFactory.createTinkerGraph();

        this.testGraphs = new ArrayList<GraphTestHolder>();
        for (int ix = 0; ix < jsonArray.length(); ix++) {
            ClientRequest graphRequest = ClientRequest.create().build(createUri("/" + jsonArray.optString(ix)), "GET");
            ClientResponse graphResponse = this.client().handle(graphRequest);

            JSONObject graphJson = graphResponse.getEntity(JSONObject.class);

            final JSONObject featuresJson = graphJson.optJSONObject("features");
            final Map<String, Boolean> features = new HashMap<String, Boolean>();
            Iterator<String> keys = featuresJson.keys();
            while (keys.hasNext()) {
                final String key = keys.next();
                features.put(key, featuresJson.optBoolean(key));
            }
            
            GraphTestHolder holder = new GraphTestHolder(graphJson.optString("name"),
                    graphJson.optString("type"), features);
            this.testGraphs.add(holder);

            for (Vertex v : testGraph.getVertices()) {
                postVertex(holder, v);
            }

            for (Edge e : testGraph.getEdges()) {
                postEdge(holder, e);
            }
        }
    }

    @After
    public void tearDown() {
        for (GraphTestHolder testGraph : this.testGraphs) {
            ClientResponse response = doGraphGet(testGraph, "vertices");
            JSONObject verticesJson = response.getEntity(JSONObject.class);
            JSONArray verticesToDelete = verticesJson.optJSONArray(Tokens.RESULTS);
            for (int ix = 0; ix < verticesToDelete.length(); ix++) {
                this.client().handle(ClientRequest.create().build(createUri("/" + testGraph.getGraphName() + "/vertices/" + encode(verticesToDelete.optJSONObject(ix).optString(Tokens._ID))), "DELETE"));
            }

            if (testGraph.getFeatures().supportsIndices) {
                response = doGraphGet(testGraph, "indices");
                JSONObject indicesJson = response.getEntity(JSONObject.class);
                JSONArray indicesToDelete = indicesJson.optJSONArray(Tokens.RESULTS);
                for (int ix = 0; ix < indicesToDelete.length(); ix++) {
                    this.client().handle(ClientRequest.create().build(createUri("/" + testGraph.getGraphName() + "/indices/" + indicesToDelete.optJSONObject(ix).optString("name")), "DELETE"));
                }
            }

            // todo: hack around titan inability to drop key indices
            if (!testGraph.getGraphType().equals("com.thinkaurelius.titan.graphdb.database.StandardTitanGraph")) {
                response = doGraphGet(testGraph, "keyindices/vertex");
                JSONObject keyIndicesVertexJson = response.getEntity(JSONObject.class);
                JSONArray keyIndicesVertexToDelete = keyIndicesVertexJson.optJSONArray(Tokens.RESULTS);
                for (int ix = 0; ix < keyIndicesVertexToDelete.length(); ix++) {
                    this.client().handle(ClientRequest.create().build(createUri("/" + testGraph.getGraphName() + "/keyindices/vertex/" + encode(keyIndicesVertexToDelete.optString(ix))), "DELETE"));
                }

                response = doGraphGet(testGraph, "keyindices/edge");
                JSONObject keyIndicesEdgeJson = response.getEntity(JSONObject.class);
                JSONArray keyIndicesEdgeToDelete = keyIndicesEdgeJson.optJSONArray(Tokens.RESULTS);
                for (int ix = 0; ix < keyIndicesEdgeToDelete.length(); ix++) {
                    this.client().handle(ClientRequest.create().build(createUri("/" + testGraph.getGraphName() + "/keyindices/edge/" + keyIndicesEdgeToDelete.optString(ix)), "DELETE"));
                }
            }
        }
    }

    protected void postVertex(GraphTestHolder graphHolder, Vertex v) throws JSONException {
        ClientRequest request = ClientRequest.create().type(RexsterMediaType.APPLICATION_REXSTER_TYPED_JSON).build(createUri("/" + graphHolder.getGraphName() + "/vertices"), "POST");
        request.setEntity(typeTheElement(GraphSONUtility.jsonFromElement(v)));

        ClientResponse response = this.client().handle(request);

        JSONObject jsonObject = response.getEntity(JSONObject.class);
        String id = jsonObject.optJSONObject(Tokens.RESULTS).optString(Tokens._ID);

        graphHolder.getVertexIdSet().put(v.getId().toString(), id);

    }

    protected void postEdge(GraphTestHolder graphHolder, Edge e) throws JSONException {
        ClientRequest request = ClientRequest.create().build(createUri("/" + graphHolder.getGraphName() + "/edges"), "POST");

        JSONObject jsonEdge = typeTheElement(GraphSONUtility.jsonFromElement(e));
        jsonEdge.put(Tokens._IN_V, graphHolder.getVertexIdSet().get(jsonEdge.optString(Tokens._IN_V)));
        jsonEdge.put(Tokens._OUT_V, graphHolder.getVertexIdSet().get(jsonEdge.optString(Tokens._OUT_V)));

        request.setEntity(jsonEdge);
        List<Object> headerValue = new ArrayList<Object>() {{
            add(RexsterMediaType.APPLICATION_REXSTER_TYPED_JSON);
        }};
        request.getHeaders().put("Content-Type", headerValue);

        ClientResponse response = this.client().handle(request);

        JSONObject jsonObject = response.getEntity(JSONObject.class);
        String id = jsonObject.optJSONObject(Tokens.RESULTS).optString(Tokens._ID);

        graphHolder.getEdgeIdSet().put(e.getId().toString(), id);

    }

    public static JSONObject typeTheElement(JSONObject json) {
        // map is only one level deep for the test graph so this doesn't really need to be recursive
        final Iterator it = json.keys();

        try {
            while (it.hasNext()) {
                final String key = (String) it.next();
                if (!key.startsWith("_")) {
                    final Object value = json.opt(key);
                    if (value instanceof String)
                        json.put(key, "(s," + value.toString() + ")");
                    else if (value instanceof Integer)
                        json.put(key, "(i," + value + ")");
                    else if (value instanceof Long)
                        json.put(key, "(l," + value + ")");
                    else if (value instanceof Float)
                        json.put(key, "(f," + value + ")");
                    else if (value instanceof Double)
                        json.put(key, "(d," + value + ")");
                    else
                        json.put(key, value.toString());
                }
            }
        } catch (JSONException jsone) {

        }

        return json;
    }

}
