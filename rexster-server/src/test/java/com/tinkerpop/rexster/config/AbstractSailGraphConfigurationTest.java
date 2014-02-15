package com.tinkerpop.rexster.config;

import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.impls.sail.impls.LinkedDataSailGraph;
import com.tinkerpop.blueprints.impls.sail.impls.MemoryStoreSailGraph;
import com.tinkerpop.rexster.RexsterApplicationGraph;
import com.tinkerpop.rexster.Tokens;
import junit.framework.Assert;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertTrue;

public class AbstractSailGraphConfigurationTest {

    private MockSailGraphConfiguration configuration = new MockSailGraphConfiguration();

    @Test(expected = GraphConfigurationException.class)
    public void configureGraphInstanceNativeSailTypeNoGraphFile() throws GraphConfigurationException {
        HierarchicalConfiguration graphConfig = new HierarchicalConfiguration();
        Map<String, RexsterApplicationGraph> graphs = new HashMap<String, RexsterApplicationGraph>();

        this.configuration.setSailType(AbstractSailGraphConfiguration.SAIL_TYPE_NATIVE);

        this.configuration.configureGraphInstance(graphConfig, graphs);
    }

    @Test(expected = GraphConfigurationException.class)
    public void configureGraphInstanceNativeSailTypeEmptyGraphFile() throws GraphConfigurationException {
        HierarchicalConfiguration graphConfig = new HierarchicalConfiguration();
        graphConfig.setProperty(Tokens.REXSTER_GRAPH_LOCATION, "");
        Map<String, RexsterApplicationGraph> graphs = new HashMap<String, RexsterApplicationGraph>();

        this.configuration.setSailType(AbstractSailGraphConfiguration.SAIL_TYPE_NATIVE);

        this.configuration.configureGraphInstance(graphConfig, graphs);
    }

    @Test(expected = GraphConfigurationException.class)
    public void configureGraphInstanceSparqlRepoSailTypeNoGraphFile() throws GraphConfigurationException {
        HierarchicalConfiguration graphConfig = new HierarchicalConfiguration();
        Map<String, RexsterApplicationGraph> graphs = new HashMap<String, RexsterApplicationGraph>();

        this.configuration.setSailType(AbstractSailGraphConfiguration.SAIL_TYPE_SPARQL);

        this.configuration.configureGraphInstance(graphConfig, graphs);
    }

    @Test(expected = GraphConfigurationException.class)
    public void configureGraphInstanceSparqlRepoSailTypeEmptyGraphFile() throws GraphConfigurationException {
        HierarchicalConfiguration graphConfig = new HierarchicalConfiguration();
        graphConfig.setProperty(Tokens.REXSTER_GRAPH_LOCATION, "");
        Map<String, RexsterApplicationGraph> graphs = new HashMap<String, RexsterApplicationGraph>();

        this.configuration.setSailType(AbstractSailGraphConfiguration.SAIL_TYPE_SPARQL);

        this.configuration.configureGraphInstance(graphConfig, graphs);
    }

    @Test
    public void configureGraphInstanceMemorySailTypeEmptyGraphFile() throws GraphConfigurationException {
        HierarchicalConfiguration graphConfig = new HierarchicalConfiguration();
        graphConfig.setProperty(Tokens.REXSTER_GRAPH_LOCATION, "");
        Map<String, RexsterApplicationGraph> graphs = new HashMap<String, RexsterApplicationGraph>();

        this.configuration.setSailType(AbstractSailGraphConfiguration.SAIL_TYPE_MEMORY);

        Graph graph = this.configuration.configureGraphInstance(graphConfig, graphs);
        Assert.assertNotNull(graph);
        Assert.assertTrue(graph instanceof MemoryStoreSailGraph);
    }

    @Test
    public void configureGraphInstanceMemorySailTypeNoGraphFileProperty() throws GraphConfigurationException {
        HierarchicalConfiguration graphConfig = new HierarchicalConfiguration();
        Map<String, RexsterApplicationGraph> graphs = new HashMap<String, RexsterApplicationGraph>();

        this.configuration.setSailType(AbstractSailGraphConfiguration.SAIL_TYPE_MEMORY);

        Graph graph = this.configuration.configureGraphInstance(graphConfig, graphs);
        Assert.assertNotNull(graph);
        Assert.assertTrue(graph instanceof MemoryStoreSailGraph);
    }

    public void configureGraphInstanceLinkedDataSailType() throws GraphConfigurationException {
        // first configure the base graph
        HierarchicalConfiguration graphConfig = new HierarchicalConfiguration();
        graphConfig.setProperty(Tokens.REXSTER_GRAPH_LOCATION, "");
        Map<String, RexsterApplicationGraph> graphs = new HashMap<String, RexsterApplicationGraph>();
        this.configuration.setSailType(AbstractSailGraphConfiguration.SAIL_TYPE_MEMORY);
        Graph graph = this.configuration.configureGraphInstance(graphConfig, graphs);

        graphs.put("ldbase", new RexsterApplicationGraph("ldbase", graph));

        // now configure LinkedDataSailGraph
        graphConfig = new HierarchicalConfiguration();
        graphConfig.setProperty(Tokens.REXSTER_GRAPH_LOCATION, "ldbase");
        this.configuration.setSailType(AbstractSailGraphConfiguration.SAIL_TYPE_LINKED_DATA);
        this.configuration.configureGraphInstance(graphConfig, graphs);

        graph = this.configuration.configureGraphInstance(graphConfig, graphs);
        assertTrue(graph instanceof LinkedDataSailGraph);
    }

    @Test(expected = GraphConfigurationException.class)
    public void configureGraphInstanceLinkedDataSailTypeNoBaseGraph() throws GraphConfigurationException {
        HierarchicalConfiguration graphConfig = new HierarchicalConfiguration();
        graphConfig.setProperty(Tokens.REXSTER_GRAPH_LOCATION, "");
        Map<String, RexsterApplicationGraph> graphs = new HashMap<String, RexsterApplicationGraph>();

        this.configuration.setSailType(AbstractSailGraphConfiguration.SAIL_TYPE_LINKED_DATA);

        this.configuration.configureGraphInstance(graphConfig, graphs);
    }

    @Test(expected = GraphConfigurationException.class)
    public void configureGraphInstanceLinkedDataSailTypeBaseGraphDoesNotExist() throws GraphConfigurationException {
        HierarchicalConfiguration graphConfig = new HierarchicalConfiguration();
        graphConfig.setProperty(Tokens.REXSTER_GRAPH_LOCATION, "thisbasegraphdoesnotexist");
        Map<String, RexsterApplicationGraph> graphs = new HashMap<String, RexsterApplicationGraph>();

        this.configuration.setSailType(AbstractSailGraphConfiguration.SAIL_TYPE_LINKED_DATA);

        this.configuration.configureGraphInstance(graphConfig, graphs);
    }
}
