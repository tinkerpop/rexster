package com.tinkerpop.rexster.config;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.junit.Test;

public class DexGraphConfigurationTest {

    private GraphConfiguration configuration = new DexGraphConfiguration();

    @Test(expected = GraphConfigurationException.class)
    public void configureGraphInstanceMissingGraphFileConfig() throws GraphConfigurationException {
        Configuration graphConfig = new HierarchicalConfiguration();
        configuration.configureGraphInstance(graphConfig);
    }

}
