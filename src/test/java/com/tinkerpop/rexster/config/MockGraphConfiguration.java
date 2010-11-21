package com.tinkerpop.rexster.config;

import org.apache.commons.configuration.Configuration;

import com.tinkerpop.blueprints.pgm.Graph;
import com.tinkerpop.blueprints.pgm.impls.tg.TinkerGraph;

public class MockGraphConfiguration implements GraphConfiguration {

	@Override
	public Graph configureGraphInstance(Configuration properties) throws GraphConfigurationException {
		return new TinkerGraph();
	}

}
