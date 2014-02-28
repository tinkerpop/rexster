package com.tinkerpop.rexster.config;

import com.tinkerpop.rexster.RexsterApplicationGraph;
import org.apache.commons.configuration.Configuration;

import java.util.Map;

/**
 * A helper object which contains properties and references for a GraphConfiguration.
 *
 * @author Joshua Shinavier (http://fortytwo.net)
 */
public class GraphConfigurationContext {
    private final Configuration properties;
    private final Map<String, RexsterApplicationGraph> graphs;

    public GraphConfigurationContext(final Configuration properties,
                                     final Map<String, RexsterApplicationGraph> graphs) {
        this.properties = properties;
        this.graphs = graphs;
    }

    /**
     * @return the configuration properties for the graph under construction
     */
    public Configuration getProperties() {
        return properties;
    }

    /**
     * @return a map of references, by name, to all graphs constructed up to this point.
     * This allows the graph under construction to build upon other graphs.
     */
    public Map<String, RexsterApplicationGraph> getGraphs() {
        return graphs;
    }
}
