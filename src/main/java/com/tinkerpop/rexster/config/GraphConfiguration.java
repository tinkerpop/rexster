package com.tinkerpop.rexster.config;

import org.apache.commons.configuration.Configuration;

import com.tinkerpop.blueprints.pgm.Graph;

public interface GraphConfiguration {
	Graph configureGraphInstance(Configuration properties) throws GraphConfigurationException;
}
