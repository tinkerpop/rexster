package com.tinkerpop.rexster.config;

import com.tinkerpop.rexster.Tokens;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.HierarchicalConfiguration;
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
    public void configureGraphInstanceNoNeo4jConfig() throws GraphConfigurationException {
        Configuration graphConfig = new HierarchicalConfiguration();
        graphConfig.addProperty(Tokens.REXSTER_GRAPH_LOCATION, this.neo4jFile);
        configuration.configureGraphInstance(graphConfig);
    }

    @Test(expected = GraphConfigurationException.class)
    public void configureGraphInstanceHaOnNoProperties() throws GraphConfigurationException {
        Configuration graphConfig = new HierarchicalConfiguration();
        graphConfig.addProperty(Tokens.REXSTER_GRAPH_LOCATION, this.neo4jFile);
        graphConfig.addProperty(Tokens.REXSTER_GRAPH_HA, "true");
        configuration.configureGraphInstance(graphConfig);
    }

    @Test(expected = GraphConfigurationException.class)
    public void configureGraphInstanceHaOnNoMachineId() throws GraphConfigurationException {
        HierarchicalConfiguration graphConfig = new HierarchicalConfiguration();
        graphConfig.addProperty(Tokens.REXSTER_GRAPH_LOCATION, this.neo4jFile);
        graphConfig.addProperty(Tokens.REXSTER_GRAPH_HA, "true");

        ArrayList<HierarchicalConfiguration.Node> listOfNodes = new ArrayList<HierarchicalConfiguration.Node>();
        listOfNodes.add(new HierarchicalConfiguration.Node("anything", "nothing"));
        graphConfig.addNodes(Tokens.REXSTER_GRAPH_PROPERTIES, listOfNodes);

        configuration.configureGraphInstance(graphConfig);
    }

    @Test(expected = GraphConfigurationException.class)
    public void configureGraphInstanceHaOnNoServer() throws GraphConfigurationException {
        HierarchicalConfiguration graphConfig = new HierarchicalConfiguration();
        graphConfig.addProperty(Tokens.REXSTER_GRAPH_LOCATION, this.neo4jFile);
        graphConfig.addProperty(Tokens.REXSTER_GRAPH_HA, "true");

        ArrayList<HierarchicalConfiguration.Node> listOfNodes = new ArrayList<HierarchicalConfiguration.Node>();
        listOfNodes.add(new HierarchicalConfiguration.Node("ha.machine_id", "1"));
        graphConfig.addNodes(Tokens.REXSTER_GRAPH_PROPERTIES, listOfNodes);

        configuration.configureGraphInstance(graphConfig);
    }

    @Test(expected = GraphConfigurationException.class)
    public void configureGraphInstanceHaOnNoZkServers() throws GraphConfigurationException {
        HierarchicalConfiguration graphConfig = new HierarchicalConfiguration();
        graphConfig.addProperty(Tokens.REXSTER_GRAPH_LOCATION, this.neo4jFile);
        graphConfig.addProperty(Tokens.REXSTER_GRAPH_HA, "true");

        ArrayList<HierarchicalConfiguration.Node> listOfNodes = new ArrayList<HierarchicalConfiguration.Node>();
        listOfNodes.add(new HierarchicalConfiguration.Node("ha.machine_id", "1"));
        listOfNodes.add(new HierarchicalConfiguration.Node("ha.server", "localhost:9939"));
        graphConfig.addNodes(Tokens.REXSTER_GRAPH_PROPERTIES, listOfNodes);

        configuration.configureGraphInstance(graphConfig);
    }

}
