package com.tinkerpop.rexster.config;

import com.tinkerpop.blueprints.pgm.Graph;
import com.tinkerpop.blueprints.pgm.impls.tg.TinkerGraph;
import org.apache.commons.configuration.Configuration;

public class MockGraphConfiguration implements GraphConfiguration {

    @Override
    public Graph configureGraphInstance(Configuration properties) throws GraphConfigurationException {
        return new TinkerGraph();
    }

}
