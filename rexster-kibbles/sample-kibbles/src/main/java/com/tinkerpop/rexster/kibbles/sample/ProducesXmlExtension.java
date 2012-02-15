package com.tinkerpop.rexster.kibbles.sample;

import com.tinkerpop.blueprints.pgm.Edge;
import com.tinkerpop.blueprints.pgm.Vertex;
import com.tinkerpop.rexster.extension.ExtensionDefinition;
import com.tinkerpop.rexster.extension.ExtensionDescriptor;
import com.tinkerpop.rexster.extension.ExtensionNaming;
import com.tinkerpop.rexster.extension.ExtensionPoint;
import com.tinkerpop.rexster.extension.ExtensionResponse;
import com.tinkerpop.rexster.extension.RexsterContext;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * An extension that returns an XML representation of a vertex and an edge.
 */
@ExtensionNaming(name = ProducesXmlExtension.EXTENSION_NAME, namespace = AbstractSampleExtension.EXTENSION_NAMESPACE)
public class ProducesXmlExtension extends AbstractSampleExtension {
    public static final String EXTENSION_NAME = "produces-xml";

    /**
     * Takes a vertex and converts the "standard" properties to XML.
     */
    @ExtensionDefinition(extensionPoint = ExtensionPoint.VERTEX, produces = MediaType.APPLICATION_XML)
    @ExtensionDescriptor(description = "returns standard properties of a vertex as XML.")
    public ExtensionResponse doVertexToXml(@RexsterContext Vertex vertex) {
        String xml = "<vertex><id>" + vertex.getId().toString() + "</id></vertex>";
        return new ExtensionResponse(Response.ok(xml).build());
    }

    /**
     * Takes a edge and converts the "standard" properties to XML.
     */
    @ExtensionDefinition(extensionPoint = ExtensionPoint.EDGE, produces = MediaType.APPLICATION_XML)
    @ExtensionDescriptor(description = "returns standard properties of an edge as XML.")
    public ExtensionResponse doEdgeToXml(@RexsterContext Edge edge) {
        String xml = "<edge><id>" + edge.getId().toString() + "</id><label>" + edge.getLabel() + "</label></edge>";
        return new ExtensionResponse(Response.ok(xml).build());
    }
}
