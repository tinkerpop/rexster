package com.tinkerpop.rexster.kibbles.sample;

import com.tinkerpop.blueprints.pgm.Edge;
import com.tinkerpop.blueprints.pgm.Graph;
import com.tinkerpop.blueprints.pgm.Vertex;
import com.tinkerpop.blueprints.pgm.impls.tg.TinkerGraphFactory;
import com.tinkerpop.rexster.extension.ExtensionResponse;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response;

public class ProducesXmlExtensionTest {
    private Graph graph;
    private ProducesXmlExtension extension = new ProducesXmlExtension();

    @Before
    public void beforeTest() {
        // in some cases it may be preferable to mock the Graph but for quick test purposes. the sample
        // graph is good and stable.
        this.graph = TinkerGraphFactory.createTinkerGraph();
    }

    @Test
    public void doVertexToXml() {
        Vertex v = this.graph.getVertex(1);
        ExtensionResponse extensionResponse = this.extension.doVertexToXml(v);

        Assert.assertNotNull(extensionResponse);
        Response response = extensionResponse.getJerseyResponse();
        Assert.assertNotNull(response);
        Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        String xml = (String) response.getEntity();
        Assert.assertEquals("<vertex><id>1</id></vertex>", xml);
    }

    @Test
    public void doEdgeToXml() {
        Edge e = this.graph.getEdge(11);
        ExtensionResponse extensionResponse = this.extension.doEdgeToXml(e);

        Assert.assertNotNull(extensionResponse);
        Response response = extensionResponse.getJerseyResponse();
        Assert.assertNotNull(response);
        Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        String xml = (String) response.getEntity();
        Assert.assertEquals("<edge><id>11</id><label>created</label></edge>", xml);
    }
}
