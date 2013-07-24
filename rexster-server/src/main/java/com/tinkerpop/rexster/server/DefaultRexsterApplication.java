package com.tinkerpop.rexster.server;

import com.codahale.metrics.MetricRegistry;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.rexster.RexsterApplicationGraph;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Configure a single existing graph into Rexster.  Useful in configuring Rexster for embedded applications.
 *
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class DefaultRexsterApplication extends AbstractMapRexsterApplication {

    private static final Logger logger = Logger.getLogger(DefaultRexsterApplication.class);

    /**
     * Constructs the DefaultRexsterApplication. With this constructor all extensions are allowed which is likely
     * the expected case in an embedded setting.
     *
     * @param graphName the name the graph will have in various Rexster contexts.
     * @param graph a graph instance.
     */
    public DefaultRexsterApplication(final String graphName, final Graph graph) {
        this(graphName, graph, new ArrayList<String>(){{ add("*:*"); }}, null, null);
    }

    /**
     * Constructs the DefaultRexsterApplication.
     *
     * @param graphName the name the graph will have in various Rexster contexts.
     * @param graph a graph instance.
     * @param allowableNamespaces list of namespace strings that are allowed for this graph.
     * @param extensionConfigurations configurations from rexster.xml for extensions. this value may be null.
     */
    public DefaultRexsterApplication(final String graphName, final Graph graph, final List<String> allowableNamespaces,
                                     final List<HierarchicalConfiguration> extensionConfigurations,
                                     final MetricRegistry metricRegistry) {
        AbstractMapRexsterApplication.metricRegistry = metricRegistry;
        final RexsterApplicationGraph rag = new RexsterApplicationGraph(graphName, graph, allowableNamespaces, extensionConfigurations);
        this.graphs.put(graphName, rag);
        logger.info(String.format("Graph [%s] loaded", rag.getGraph()));
    }
}
