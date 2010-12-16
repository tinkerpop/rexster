package com.tinkerpop.rexster.config;

import java.util.ArrayList;

import junit.framework.Assert;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.junit.Test;

import com.tinkerpop.blueprints.pgm.Graph;
import com.tinkerpop.blueprints.pgm.impls.sail.impls.MemoryStoreSailGraph;
import com.tinkerpop.rexster.Tokens;

public class SailGraphConfigurationTest {
	
	private GraphConfiguration configuration = new SailGraphConfiguration();

    @Test(expected = GraphConfigurationException.class)
    public void configureGraphInstanceNoGraphFile() throws GraphConfigurationException {
    	HierarchicalConfiguration graphConfig = new HierarchicalConfiguration();
        configuration.configureGraphInstance(graphConfig);
    }
    
    @Test(expected = GraphConfigurationException.class)
    public void configureGraphInstanceInvalidSailType() throws GraphConfigurationException {
    	HierarchicalConfiguration graphConfig = new HierarchicalConfiguration();
        
        ArrayList<HierarchicalConfiguration.Node> listOfNodes = new ArrayList<HierarchicalConfiguration.Node>();
        listOfNodes.add(new HierarchicalConfiguration.Node("blueprint-sail-type", "somejunk"));
        graphConfig.addNodes(Tokens.REXSTER_GRAPH_PROPERTIES, listOfNodes);

        configuration.configureGraphInstance(graphConfig);
    }
    
    @Test(expected = GraphConfigurationException.class)
    public void configureGraphInstanceEmptySailType() throws GraphConfigurationException {
    	HierarchicalConfiguration graphConfig = new HierarchicalConfiguration();
        
        ArrayList<HierarchicalConfiguration.Node> listOfNodes = new ArrayList<HierarchicalConfiguration.Node>();
        listOfNodes.add(new HierarchicalConfiguration.Node("blueprint-sail-type", ""));
        graphConfig.addNodes(Tokens.REXSTER_GRAPH_PROPERTIES, listOfNodes);

        configuration.configureGraphInstance(graphConfig);
    }
    
    @Test(expected = GraphConfigurationException.class)
    public void configureGraphInstanceMissingSailTypeProperty() throws GraphConfigurationException {
    	HierarchicalConfiguration graphConfig = new HierarchicalConfiguration();
        
        ArrayList<HierarchicalConfiguration.Node> listOfNodes = new ArrayList<HierarchicalConfiguration.Node>();
        listOfNodes.add(new HierarchicalConfiguration.Node("some-other-property", ""));
        graphConfig.addNodes(Tokens.REXSTER_GRAPH_PROPERTIES, listOfNodes);

        configuration.configureGraphInstance(graphConfig);
    }
    
    @Test(expected = GraphConfigurationException.class)
    public void configureGraphInstanceNeo4jSailTypeNoGraphFile() throws GraphConfigurationException {
    	HierarchicalConfiguration graphConfig = new HierarchicalConfiguration();
        
        ArrayList<HierarchicalConfiguration.Node> listOfNodes = new ArrayList<HierarchicalConfiguration.Node>();
        listOfNodes.add(new HierarchicalConfiguration.Node("blueprints-sail-type", "neo4j"));
        graphConfig.addNodes(Tokens.REXSTER_GRAPH_PROPERTIES, listOfNodes);

        configuration.configureGraphInstance(graphConfig);
    }
    
    @Test(expected = GraphConfigurationException.class)
    public void configureGraphInstanceNativeSailTypeNoGraphFile() throws GraphConfigurationException {
    	HierarchicalConfiguration graphConfig = new HierarchicalConfiguration();
        
        ArrayList<HierarchicalConfiguration.Node> listOfNodes = new ArrayList<HierarchicalConfiguration.Node>();
        listOfNodes.add(new HierarchicalConfiguration.Node("blueprints-sail-type", "native"));
        graphConfig.addNodes(Tokens.REXSTER_GRAPH_PROPERTIES, listOfNodes);

        configuration.configureGraphInstance(graphConfig);
    }
    
    @Test(expected = GraphConfigurationException.class)
    public void configureGraphInstanceNeo4jSailTypeEmptyGraphFile() throws GraphConfigurationException {
    	HierarchicalConfiguration graphConfig = new HierarchicalConfiguration();
    	graphConfig.setProperty(Tokens.REXSTER_GRAPH_FILE, "");
        
        ArrayList<HierarchicalConfiguration.Node> listOfNodes = new ArrayList<HierarchicalConfiguration.Node>();
        listOfNodes.add(new HierarchicalConfiguration.Node("blueprints-sail-type", "neo4j"));
        graphConfig.addNodes(Tokens.REXSTER_GRAPH_PROPERTIES, listOfNodes);

        configuration.configureGraphInstance(graphConfig);
    }
    
    @Test(expected = GraphConfigurationException.class)
    public void configureGraphInstanceNativeSailTypeEmptyGraphFile() throws GraphConfigurationException {
    	HierarchicalConfiguration graphConfig = new HierarchicalConfiguration();
    	graphConfig.setProperty(Tokens.REXSTER_GRAPH_FILE, "");
        
        ArrayList<HierarchicalConfiguration.Node> listOfNodes = new ArrayList<HierarchicalConfiguration.Node>();
        listOfNodes.add(new HierarchicalConfiguration.Node("blueprints-sail-type", "native"));
        graphConfig.addNodes(Tokens.REXSTER_GRAPH_PROPERTIES, listOfNodes);

        configuration.configureGraphInstance(graphConfig);
    }
    
    @Test
    public void configureGraphInstanceMemorySailTypeEmptyGraphFile() throws GraphConfigurationException {
    	HierarchicalConfiguration graphConfig = new HierarchicalConfiguration();
    	graphConfig.setProperty(Tokens.REXSTER_GRAPH_FILE, "");
        
        ArrayList<HierarchicalConfiguration.Node> listOfNodes = new ArrayList<HierarchicalConfiguration.Node>();
        listOfNodes.add(new HierarchicalConfiguration.Node("blueprints-sail-type", "memory"));
        graphConfig.addNodes(Tokens.REXSTER_GRAPH_PROPERTIES, listOfNodes);

        Graph graph = configuration.configureGraphInstance(graphConfig);
        Assert.assertNotNull(graph);
        Assert.assertTrue(graph instanceof MemoryStoreSailGraph);
    }
    
    @Test
    public void configureGraphInstanceMemorySailTypeNoGraphFileProperty() throws GraphConfigurationException {
    	HierarchicalConfiguration graphConfig = new HierarchicalConfiguration();
        
        ArrayList<HierarchicalConfiguration.Node> listOfNodes = new ArrayList<HierarchicalConfiguration.Node>();
        listOfNodes.add(new HierarchicalConfiguration.Node("blueprints-sail-type", "memory"));
        graphConfig.addNodes(Tokens.REXSTER_GRAPH_PROPERTIES, listOfNodes);

        Graph graph = configuration.configureGraphInstance(graphConfig);
        Assert.assertNotNull(graph);
        Assert.assertTrue(graph instanceof MemoryStoreSailGraph);
    }
    
    // TODO: implement tests for all sail graph types.  need some sail data or a way to test without it.
}
