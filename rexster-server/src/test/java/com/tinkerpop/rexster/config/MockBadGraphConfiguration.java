package com.tinkerpop.rexster.config;

import com.tinkerpop.blueprints.pgm.Graph;
import org.apache.commons.configuration.Configuration;

public class MockBadGraphConfiguration implements GraphConfiguration {
    public Graph configureGraphInstance(Configuration properties) throws GraphConfigurationException {
        throw new GraphConfigurationException("busted");
    }
}
