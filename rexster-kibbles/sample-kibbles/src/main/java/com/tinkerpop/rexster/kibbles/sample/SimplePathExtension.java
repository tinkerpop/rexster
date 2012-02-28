package com.tinkerpop.rexster.kibbles.sample;

import com.tinkerpop.blueprints.pgm.Graph;
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
 * This sample focuses on path-level extensions.  Path-level extensions add the extension
 * to the root of the specified ExtensionPoint followed by the value specified in the
 * "path" parameter of the @ExtensionDefinition.  It is important to ensure that paths
 * remain unique.  In the event of a collision, Rexster will serve the request to the
 * first match it finds.
 */
@ExtensionNaming(name = SimplePathExtension.EXTENSION_NAME, namespace = AbstractSampleExtension.EXTENSION_NAMESPACE)
public class SimplePathExtension extends AbstractSampleExtension {
    public static final String EXTENSION_NAME = "simple-path";

    /**
     * By adding the @RexsterContext attribute to the "graph" parameter, the graph requested gets
     * automatically injected into the extension.  Therefore, when the following URI is requested:
     * <p/>
     * http://localhost:8182/graphs/tinkergraph/tp/simple-path/some-work
     * <p/>
     * the graph called "graphname" will be pushed into this method.
     */
    @ExtensionDefinition(extensionPoint = ExtensionPoint.GRAPH, path = "some-work")
    @ExtensionDescriptor(description = "returns the results of the toString() method on the graph.")
    public ExtensionResponse doSomeWorkOnGraph(@RexsterContext Graph graph) {
        return toStringIt(graph, "some");
    }

    /**
     * By adding the @RexsterContext attribute to the "graph" parameter, the graph requested gets
     * automatically injected into the extension.  Therefore, when the following URI is requested:
     * <p/>
     * http://localhost:8182/graphs/tinkergraph/tp/simple-path/other-work
     * <p/>
     * the graph called "graphname" will be pushed into this method.
     */
    @ExtensionDefinition(extensionPoint = ExtensionPoint.GRAPH, path = "other-work")
    @ExtensionDescriptor(description = "returns the results of the toString() method on the edge.")
    public ExtensionResponse doOtherWorkOnGraph(@RexsterContext Graph graph) {
        return toStringIt(graph, "other");
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
    private ExtensionResponse toStringIt(Object obj, String path) {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("output", obj.toString());
        map.put("work-came-from", path);
        return ExtensionResponse.ok(map);
    }
}
