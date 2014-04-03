package com.tinkerpop.rexster.config;

import com.tinkerpop.rexster.RexsterApplicationGraph;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class SparkseeGraphConfigurationTest {

    private GraphConfiguration configuration = new SparkseeGraphConfiguration();

    @Test(expected = GraphConfigurationException.class)
    public void configureGraphInstanceMissingGraphFileConfig() throws GraphConfigurationException {
        Configuration graphConfig = new HierarchicalConfiguration();
        Map<String, RexsterApplicationGraph> graphs = new HashMap<String, RexsterApplicationGraph>();
        GraphConfigurationContext context = new GraphConfigurationContext(graphConfig, graphs);

        configuration.configureGraphInstance(context);
    }

}
