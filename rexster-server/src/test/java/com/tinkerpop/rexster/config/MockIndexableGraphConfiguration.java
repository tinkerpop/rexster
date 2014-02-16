package com.tinkerpop.rexster.config;

import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.impls.tg.TinkerGraph;

public class MockIndexableGraphConfiguration implements GraphConfiguration {

    public Graph configureGraphInstance(final GraphConfigurationContext context) throws GraphConfigurationException {
        return new TinkerGraph();
    }

}
