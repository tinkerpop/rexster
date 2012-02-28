package com.tinkerpop.rexster.kibbles.sample;

import com.tinkerpop.blueprints.pgm.Edge;
import com.tinkerpop.blueprints.pgm.Graph;
import com.tinkerpop.blueprints.pgm.Vertex;
import com.tinkerpop.rexster.extension.ExtensionDefinition;
import com.tinkerpop.rexster.extension.ExtensionDescriptor;
import com.tinkerpop.rexster.extension.ExtensionNaming;
import com.tinkerpop.rexster.extension.ExtensionPoint;
import com.tinkerpop.rexster.extension.ExtensionResponse;
import com.tinkerpop.rexster.extension.RexsterContext;

import java.util.HashMap;

/**
 * An extension that showcases the methods available to those who wish to extend Rexster.
 * <p/>
 * This sample focuses on root-level extensions.  Root-level extensions add the extension
 * to the root of the specified ExtensionPoint.  No additional pathing is taken into consideration
 * when routing to the service method, therefore, if more than one root-level extension is specified
 * then Rexster may appear to misbehave.  Rexster will choose the first extension method match that
 * it can find when processing a request.
 */
@ExtensionNaming(name = SimpleRootExtension.EXTENSION_NAME, namespace = AbstractSampleExtension.EXTENSION_NAMESPACE)
public class SimpleRootExtension extends AbstractSampleExtension {

    public static final String EXTENSION_NAME = "simple-root";

    /**
     * By adding the @RexsterContext attribute to the "graph" parameter, the graph requested gets
     * automatically injected into the extension.  Therefore, when the following URI is requested:
     * <p/>
     * http://localhost:8182/graphs/tinkergraph/tp/simple-root
     * <p/>
     * the graph called "graphname" will be pushed into this method.
     */
    @ExtensionDefinition(extensionPoint = ExtensionPoint.GRAPH)
    @ExtensionDescriptor(description = "returns the results of the toString() method on the graph.")
    public ExtensionResponse doWorkOnGraph(@RexsterContext Graph graph) {
        return toStringIt(graph);
    }

    /**
     * By adding the @RexsterContext attribute to the "edge" parameter, the edge requested gets
     * automatically injected into the extension.  Therefore, when the following URI is requested:
     * <p/>
     * http://localhost:8182/graphs/tinkergraph/edges/11/tp/simple-root
     * <p/>
     * the edge with an ID of 1 in the graph called "tinkergraph" will be pushed into this method.
     */
    @ExtensionDefinition(extensionPoint = ExtensionPoint.EDGE)
    @ExtensionDescriptor(description = "returns the results of the toString() method on the edge.")
    public ExtensionResponse doWorkOnEdge(@RexsterContext Edge edge) {
        return toStringIt(edge);
    }

    /**
     * By adding the @RexsterContext attribute to the "vertex" parameter, the edge requested gets
     * automatically injected into the extension.  Therefore, when the following URI is requested:
     * <p/>
     * http://localhost:8182/graphs/tinkergraph/vertices/1/tp/simple-root
     * <p/>
     * the edge with an ID of 1 in the graph called "tinkergraph" will be pushed into this method.
     */
    @ExtensionDefinition(extensionPoint = ExtensionPoint.VERTEX)
    @ExtensionDescriptor(description = "returns the results of the toString() method on the vertex.")
    public ExtensionResponse doWorkOnVertex(@RexsterContext Vertex vertex) {
        return toStringIt(vertex);
    }

    /**
     * This method helps the root methods by wrapping the output of the toString of the graph element
     * in JSON to be returned in the ExtensionResponse.  ExtensionResponse has numerous helper methods
     * to make it easy to build the response object.
     * <p/>
     * Outputted JSON (if the object is a graph) will look like this:
     * <p/>
     * {"output":"tinkergraph[vertices:6 edges:6]","version":"0.3","queryTime":38.02189}
     * <p/>
     * Note the "version" and "queryTime" properties within the JSON.  Rexster will attempt to automatically
     * add these items when it understands the output to be JSON.  It is possible to override this default
     * behavior by setting the tryIncludeRexsterAttributes on the @Extension definition to false.
     */
    private ExtensionResponse toStringIt(Object obj) {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("output", obj.toString());
        return ExtensionResponse.ok(map);
    }
}