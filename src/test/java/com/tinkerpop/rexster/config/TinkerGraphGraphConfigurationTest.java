package com.tinkerpop.rexster.config;

import com.tinkerpop.rexster.Tokens;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.junit.Test;

public class TinkerGraphGraphConfigurationTest {

    private GraphConfiguration configuration = new TinkerGraphGraphConfiguration();

    @Test(expected = GraphConfigurationException.class)
    public void configureGraphInstanceMissingGraphFileConfig() throws GraphConfigurationException {
        Configuration graphConfig = new HierarchicalConfiguration();
        graphConfig.addProperty(Tokens.REXSTER_GRAPH_FILE, "some-file-that-does-not-exist");
        configuration.configureGraphInstance(graphConfig);
    }

}
