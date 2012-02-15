package com.tinkerpop.rexster;

import com.sun.jersey.api.client.ClientRequest;
import com.sun.jersey.api.client.ClientResponse;
import com.tinkerpop.blueprints.pgm.Edge;
import com.tinkerpop.blueprints.pgm.Vertex;
import com.tinkerpop.blueprints.pgm.impls.tg.TinkerGraph;
import com.tinkerpop.blueprints.pgm.impls.tg.TinkerGraphFactory;
import com.tinkerpop.blueprints.pgm.util.io.graphson.GraphSONFactory;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Before;

import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;

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

            GraphTestHolder holder = new GraphTestHolder(graphJson.optString("name"), graphJson.optString("type"));
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
            ClientRequest graphRequest = ClientRequest.create().build(createUri("/" + testGraph.getGraphName()), "DELETE");
            this.client().handle(graphRequest);
        }
    }

    protected void postVertex(GraphTestHolder graphHolder, Vertex v) throws JSONException {
        ClientRequest request = ClientRequest.create().type(MediaType.APPLICATION_JSON_TYPE).build(createUri("/" + graphHolder.getGraphName() + "/vertices"), "POST");
        request.setEntity(GraphSONFactory.createJSONElement(v));

        ClientResponse response = this.client().handle(request);

        JSONObject jsonObject = response.getEntity(JSONObject.class);
        String id = jsonObject.optJSONObject(Tokens.RESULTS).optString(Tokens._ID);

        graphHolder.getVertexIdSet().put(v.getId().toString(), id);

    }

    protected void postEdge(GraphTestHolder graphHolder, Edge e) throws JSONException {
        ClientRequest request = ClientRequest.create().build(createUri("/" + graphHolder.getGraphName() + "/edges"), "POST");

        JSONObject jsonEdge = GraphSONFactory.createJSONElement(e);
        jsonEdge.put(Tokens._IN_V, graphHolder.getVertexIdSet().get(jsonEdge.optString(Tokens._IN_V)));
        jsonEdge.put(Tokens._OUT_V, graphHolder.getVertexIdSet().get(jsonEdge.optString(Tokens._OUT_V)));

        request.setEntity(jsonEdge);
        List<Object> headerValue = new ArrayList<Object>() {{
            add(MediaType.APPLICATION_JSON);
        }};
        request.getHeaders().put("Content-Type", headerValue);

        ClientResponse response = this.client().handle(request);

        JSONObject jsonObject = response.getEntity(JSONObject.class);
        String id = jsonObject.optJSONObject(Tokens.RESULTS).optString(Tokens._ID);

        graphHolder.getEdgeIdSet().put(e.getId().toString(), id);

    }
}
