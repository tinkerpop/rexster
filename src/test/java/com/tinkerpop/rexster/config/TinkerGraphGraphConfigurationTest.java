package com.tinkerpop.rexster.config;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.junit.Test;

import com.tinkerpop.rexster.Tokens;

public class TinkerGraphGraphConfigurationTest {

	private GraphConfiguration configuration = new TinkerGraphGraphConfiguration(); 
	
	@Test(expected= GraphConfigurationException.class)
	public void configureGraphInstanceNoGraphFile() throws GraphConfigurationException {
		Configuration graphConfig = new HierarchicalConfiguration();
		configuration.configureGraphInstance(graphConfig);
	}
	
	@Test(expected= GraphConfigurationException.class)
	public void configureGraphInstanceNoOrientConfig() throws GraphConfigurationException {
		Configuration graphConfig = new HierarchicalConfiguration();
		graphConfig.addProperty(Tokens.REXSTER_GRAPH_FILE, "some-file");
		configuration.configureGraphInstance(graphConfig);
	}
}
