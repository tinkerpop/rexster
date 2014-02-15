package com.tinkerpop.rexster.config;

import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.rexster.RexsterApplicationGraph;
import org.apache.commons.configuration.Configuration;

import java.util.Map;

public class MockBadGraphConfiguration implements GraphConfiguration {
    public Graph configureGraphInstance(Configuration properties,
                                        Map<String, RexsterApplicationGraph> graphs) throws GraphConfigurationException {
        throw new GraphConfigurationException("busted");
    }
}
