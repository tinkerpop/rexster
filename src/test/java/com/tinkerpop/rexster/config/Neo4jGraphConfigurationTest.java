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
    public void configureGraphInstanceNoNeo4jConfig() throws GraphConfigurationException {
        Configuration graphConfig = new HierarchicalConfiguration();
        graphConfig.addProperty(Tokens.REXSTER_GRAPH_FILE, this.neo4jFile);
        configuration.configureGraphInstance(graphConfig);
    }
}
