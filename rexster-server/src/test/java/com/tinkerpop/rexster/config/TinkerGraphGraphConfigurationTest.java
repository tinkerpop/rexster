package com.tinkerpop.rexster.config;

import com.tinkerpop.rexster.Tokens;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.junit.Test;

public class TinkerGraphGraphConfigurationTest {

    private GraphConfiguration configuration = new TinkerGraphGraphConfiguration();

    @Test
    public void configureGraphInstanceNoFileConfig() throws GraphConfigurationException {
        Configuration graphConfig = new HierarchicalConfiguration();
        configuration.configureGraphInstance(graphConfig);
    }

    @Test
    public void configureGraphInstanceNoFileTypeConfig() throws GraphConfigurationException {
        Configuration graphConfig = new HierarchicalConfiguration();
        graphConfig.addProperty(Tokens.REXSTER_GRAPH_LOCATION, "some-file");
        configuration.configureGraphInstance(graphConfig);
    }
}
