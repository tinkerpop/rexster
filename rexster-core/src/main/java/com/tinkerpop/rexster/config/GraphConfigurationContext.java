package com.tinkerpop.rexster.config;

import com.tinkerpop.rexster.RexsterApplicationGraph;
import org.apache.commons.configuration.Configuration;

import java.util.Map;

/**
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

    public Configuration getProperties() {
        return properties;
    }

    public Map<String, RexsterApplicationGraph> getGraphs() {
        return graphs;
    }
}
