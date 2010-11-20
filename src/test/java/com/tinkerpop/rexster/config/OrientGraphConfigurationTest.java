package com.tinkerpop.rexster.config;

import java.util.ArrayList;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.junit.Test;

import com.tinkerpop.rexster.Tokens;

public class OrientGraphConfigurationTest {

	private GraphConfiguration configuration = new OrientGraphConfiguration(); 
	
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
	
	@Test(expected= GraphConfigurationException.class)
	public void configureGraphInstanceConfigValidButNoGraphFound()  throws GraphConfigurationException {
		HierarchicalConfiguration graphConfig = new HierarchicalConfiguration();
		graphConfig.addProperty(Tokens.REXSTER_GRAPH_FILE, "some-file");

		ArrayList<HierarchicalConfiguration.Node> listOfNodes = new ArrayList<HierarchicalConfiguration.Node>();
		listOfNodes.add(new HierarchicalConfiguration.Node("username", "me"));
		listOfNodes.add(new HierarchicalConfiguration.Node("password", "pass"));
		graphConfig.addNodes(Tokens.REXSTER_GRAPH_PROPERTIES, listOfNodes);
		
		configuration.configureGraphInstance(graphConfig);
	}
}
