package com.tinkerpop.rexster.config;

import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.impls.sail.impls.MemoryStoreSailGraph;
import com.tinkerpop.rexster.Tokens;
import junit.framework.Assert;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.junit.Test;

public class AbstractSailGraphConfigurationTest {

    private MockSailGraphConfiguration configuration = new MockSailGraphConfiguration();

    @Test(expected = GraphConfigurationException.class)
    public void configureGraphInstanceNativeSailTypeNoGraphFile() throws GraphConfigurationException {
        HierarchicalConfiguration graphConfig = new HierarchicalConfiguration();

        this.configuration.setSailType(AbstractSailGraphConfiguration.SAIL_TYPE_NATIVE);

        this.configuration.configureGraphInstance(graphConfig);
    }

    @Test(expected = GraphConfigurationException.class)
    public void configureGraphInstanceNativeSailTypeEmptyGraphFile() throws GraphConfigurationException {
        HierarchicalConfiguration graphConfig = new HierarchicalConfiguration();
        graphConfig.setProperty(Tokens.REXSTER_GRAPH_LOCATION, "");

        this.configuration.setSailType(AbstractSailGraphConfiguration.SAIL_TYPE_NATIVE);

        this.configuration.configureGraphInstance(graphConfig);
    }

    @Test(expected = GraphConfigurationException.class)
    public void configureGraphInstanceSparqlRepoSailTypeNoGraphFile() throws GraphConfigurationException {
        HierarchicalConfiguration graphConfig = new HierarchicalConfiguration();

        this.configuration.setSailType(AbstractSailGraphConfiguration.SAIL_TYPE_SPARQL);

        this.configuration.configureGraphInstance(graphConfig);
    }

    @Test(expected = GraphConfigurationException.class)
    public void configureGraphInstanceSparqlRepoSailTypeEmptyGraphFile() throws GraphConfigurationException {
        HierarchicalConfiguration graphConfig = new HierarchicalConfiguration();
        graphConfig.setProperty(Tokens.REXSTER_GRAPH_LOCATION, "");

        this.configuration.setSailType(AbstractSailGraphConfiguration.SAIL_TYPE_SPARQL);

        this.configuration.configureGraphInstance(graphConfig);
    }

    @Test
    public void configureGraphInstanceMemorySailTypeEmptyGraphFile() throws GraphConfigurationException {
        HierarchicalConfiguration graphConfig = new HierarchicalConfiguration();
        graphConfig.setProperty(Tokens.REXSTER_GRAPH_LOCATION, "");

        this.configuration.setSailType(AbstractSailGraphConfiguration.SAIL_TYPE_MEMORY);

        Graph graph = this.configuration.configureGraphInstance(graphConfig);
        Assert.assertNotNull(graph);
        Assert.assertTrue(graph instanceof MemoryStoreSailGraph);
    }

    @Test
    public void configureGraphInstanceMemorySailTypeNoGraphFileProperty() throws GraphConfigurationException {
        HierarchicalConfiguration graphConfig = new HierarchicalConfiguration();

        this.configuration.setSailType(AbstractSailGraphConfiguration.SAIL_TYPE_MEMORY);

        Graph graph = this.configuration.configureGraphInstance(graphConfig);
        Assert.assertNotNull(graph);
        Assert.assertTrue(graph instanceof MemoryStoreSailGraph);
    }
}
