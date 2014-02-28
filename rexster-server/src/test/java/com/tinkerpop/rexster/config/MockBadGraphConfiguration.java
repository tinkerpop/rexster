package com.tinkerpop.rexster.config;

import com.tinkerpop.blueprints.Graph;

public class MockBadGraphConfiguration implements GraphConfiguration {
    public Graph configureGraphInstance(final GraphConfigurationContext context) throws GraphConfigurationException {
        throw new GraphConfigurationException("busted");
    }
}
