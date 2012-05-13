package com.tinkerpop.rexster.config;

import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.impls.tg.TinkerGraph;
import org.apache.commons.configuration.Configuration;

public class MockIndexableGraphConfiguration implements GraphConfiguration {

    public Graph configureGraphInstance(Configuration properties) throws GraphConfigurationException {
        return new TinkerGraph();
    }

}
