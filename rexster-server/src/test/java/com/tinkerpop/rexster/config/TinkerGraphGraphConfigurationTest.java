package com.tinkerpop.rexster.config;

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
}
