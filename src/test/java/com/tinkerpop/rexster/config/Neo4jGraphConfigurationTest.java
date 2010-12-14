package com.tinkerpop.rexster.config;

import com.tinkerpop.blueprints.pgm.Graph;
import com.tinkerpop.rexster.Tokens;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;

public class Neo4jGraphConfigurationTest {

    private final String neo4jFile = "some-file";

    private GraphConfiguration configuration = new Neo4jGraphConfiguration();

    @Test(expected = GraphConfigurationException.class)
    public void configureGraphInstanceNoGraphFile() throws GraphConfigurationException {
        Configuration graphConfig = new HierarchicalConfiguration();
        configuration.configureGraphInstance(graphConfig);
    }

    @Test(expected = GraphConfigurationException.class)
    public void configureGraphInstanceNoOrientConfig() throws GraphConfigurationException {
        Configuration graphConfig = new HierarchicalConfiguration();
        graphConfig.addProperty(Tokens.REXSTER_GRAPH_FILE, this.neo4jFile);
        configuration.configureGraphInstance(graphConfig);
    }

    @Test(expected = GraphConfigurationException.class)
    @Ignore(value = "Can't figure out how to cleanup the files created by instantiating the neo4j graph.")
    public void configureGraphInstanceConfigValidButNoGraphFound() throws GraphConfigurationException {
        HierarchicalConfiguration graphConfig = new HierarchicalConfiguration();
        graphConfig.addProperty(Tokens.REXSTER_GRAPH_FILE, this.neo4jFile);

        ArrayList<HierarchicalConfiguration.Node> listOfNodes = new ArrayList<HierarchicalConfiguration.Node>();
        listOfNodes.add(new HierarchicalConfiguration.Node("username", "me"));
        listOfNodes.add(new HierarchicalConfiguration.Node("password", "pass"));
        graphConfig.addNodes(Tokens.REXSTER_GRAPH_PROPERTIES, listOfNodes);

        Graph graph = configuration.configureGraphInstance(graphConfig);
        graph.shutdown();
    }
}
