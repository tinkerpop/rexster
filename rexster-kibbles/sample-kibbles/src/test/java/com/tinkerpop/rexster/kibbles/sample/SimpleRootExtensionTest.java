package com.tinkerpop.rexster.kibbles.sample;

import com.tinkerpop.blueprints.pgm.Graph;
import com.tinkerpop.blueprints.pgm.impls.tg.TinkerGraphFactory;
import com.tinkerpop.rexster.extension.ExtensionResponse;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response;

public class SimpleRootExtensionTest {
    private Graph graph;
    private SimpleRootExtension simpleRootExtension = new SimpleRootExtension();

    @Before
    public void beforeTest() {
        // in some cases it may be preferable to mock the Graph but for quick test purposes. the sample
        // graph is good and stable.
        this.graph = TinkerGraphFactory.createTinkerGraph();
    }

    @Test
    public void doWorkOnGraphValid() {
        ExtensionResponse response = this.simpleRootExtension.doWorkOnGraph(this.graph);

        doTests(response, "tinkergraph[vertices:6 edges:6]");
    }

    @Test
    public void doWorkOnEdgeValid() {
        ExtensionResponse response = this.simpleRootExtension.doWorkOnEdge(this.graph.getEdge(11));

        doTests(response, "e[11][4-created->3]");
    }

    @Test
    public void doWorkOnVertexValid() {
        ExtensionResponse response = this.simpleRootExtension.doWorkOnVertex(this.graph.getVertex(1));

        doTests(response, "v[1]");
    }

    private void doTests(ExtensionResponse response, String expectedToString) {
        // the response should never be null
        Assert.assertNotNull(response);

        // the ExtensionResponse really just wraps an underlying jersey response and that
        // should not be null
        Response jerseyResponse = response.getJerseyResponse();
        Assert.assertNotNull(jerseyResponse);

        // the services return an OK status code.
        Assert.assertEquals(Response.Status.OK.getStatusCode(), jerseyResponse.getStatus());

        // JSON is wrapped in the jersey response.
        JSONObject json = (JSONObject) jerseyResponse.getEntity();
        Assert.assertNotNull(json);

        // the JSON has an output property and it contains the data from the toString call on the
        // requested element.
        Assert.assertTrue(json.has("output"));
        Assert.assertEquals(expectedToString, json.optString("output"));
    }
}
