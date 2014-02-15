package com.tinkerpop.rexster.config;

import com.tinkerpop.rexster.RexsterApplicationGraph;
import com.tinkerpop.rexster.Tokens;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class OrientGraphConfigurationTest {

    private GraphConfiguration configuration = new OrientGraphConfiguration();

    @Test(expected = GraphConfigurationException.class)
    public void configureGraphInstanceNoGraphFile() throws GraphConfigurationException {
        Configuration graphConfig = new HierarchicalConfiguration();
        Map<String, RexsterApplicationGraph> graphs = new HashMap<String, RexsterApplicationGraph>();

        configuration.configureGraphInstance(graphConfig, graphs);
    }

    @Test(expected = GraphConfigurationException.class)
    public void configureGraphInstanceNoOrientConfig() throws GraphConfigurationException {
        Configuration graphConfig = new HierarchicalConfiguration();
        graphConfig.addProperty(Tokens.REXSTER_GRAPH_LOCATION, "some-file");
        Map<String, RexsterApplicationGraph> graphs = new HashMap<String, RexsterApplicationGraph>();

        configuration.configureGraphInstance(graphConfig, graphs);
    }

    @Test(expected = GraphConfigurationException.class)
    public void configureGraphInstanceConfigValidButNoGraphFound() throws GraphConfigurationException {
        HierarchicalConfiguration graphConfig = new HierarchicalConfiguration();
        graphConfig.addProperty(Tokens.REXSTER_GRAPH_LOCATION, "some-file");

        ArrayList<HierarchicalConfiguration.Node> listOfNodes = new ArrayList<HierarchicalConfiguration.Node>();
        listOfNodes.add(new HierarchicalConfiguration.Node("username", "me"));
        listOfNodes.add(new HierarchicalConfiguration.Node("password", "pass"));
        graphConfig.addNodes(Tokens.REXSTER_GRAPH_PROPERTIES, listOfNodes);
        Map<String, RexsterApplicationGraph> graphs = new HashMap<String, RexsterApplicationGraph>();

        configuration.configureGraphInstance(graphConfig, graphs);
    }
}
