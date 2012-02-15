package com.tinkerpop.rexster.config;

import com.tinkerpop.blueprints.pgm.Graph;
import org.apache.commons.configuration.Configuration;

public interface GraphConfiguration {
    Graph configureGraphInstance(Configuration properties) throws GraphConfigurationException;
}
