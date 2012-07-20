package com.tinkerpop.rexster.config;

import com.tinkerpop.blueprints.util.wrappers.readonly.ReadOnlyGraph;
import com.tinkerpop.blueprints.util.wrappers.readonly.ReadOnlyIndexableGraph;
import com.tinkerpop.rexster.Tokens;
import junit.framework.Assert;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class GraphConfigurationContainerTest {

    private List<HierarchicalConfiguration> configList = new ArrayList<HierarchicalConfiguration>();

    @Before
    public void setUp() {
        configList.clear();
    }

    @Test(expected = GraphConfigurationException.class)
    public void getApplicationGraphsNullConfiguration() throws GraphConfigurationException {
        new GraphConfigurationContainer(null);
    }

    @Test
    public void getApplicationGraphsNoGraphName() {
        configList.add(constructTinkerGraphHierarchicalConfiguration(""));

        try {
            GraphConfigurationContainer container = new GraphConfigurationContainer(configList);
            Assert.assertEquals(1, container.getFailedConfigurations().size());
            Assert.assertEquals(0, container.getApplicationGraphs().size());
        } catch (Exception ex) {
            Assert.fail(ex.getMessage());
        }

    }

    @Test
    public void getApplicationGraphsNoGraphProperty() {
        HierarchicalConfiguration graphConfig = new HierarchicalConfiguration();
        configList.add(graphConfig);

        try {
            GraphConfigurationContainer container = new GraphConfigurationContainer(configList);
            Assert.assertEquals(1, container.getFailedConfigurations().size());
            Assert.assertEquals(0, container.getApplicationGraphs().size());
        } catch (Exception ex) {
            Assert.fail(ex.getMessage());
        }

    }

    @Test
    public void getApplicationGraphsDuplicateGraphNames() {

        configList.add(constructDefaultTinkerGraphHierarchicalConfiguration("test"));
        configList.add(constructDefaultTinkerGraphHierarchicalConfiguration("test"));

        try {
            GraphConfigurationContainer container = new GraphConfigurationContainer(configList);
            Assert.assertEquals(1, container.getFailedConfigurations().size());
            Assert.assertEquals(1, container.getApplicationGraphs().size());
        } catch (Exception ex) {
            Assert.fail(ex.getMessage());
        }

    }

    @Test
    public void getApplicationGraphsBadGraphs() {
        configList.add(constructDefaultTinkerGraphHierarchicalConfiguration("test1"));
        configList.add(constructBadHierarchicalConfiguration());

        try {
            GraphConfigurationContainer container = new GraphConfigurationContainer(configList);
            Assert.assertEquals(1, container.getFailedConfigurations().size());
            Assert.assertEquals(1, container.getApplicationGraphs().size());
            Assert.assertTrue(container.getApplicationGraphs().containsKey("test1"));
        } catch (Exception ex) {
            Assert.fail(ex.getMessage());
        }
    }

    @Test
    public void getApplicationGraphsSupportedGraphs() {
        configList.add(constructDefaultTinkerGraphHierarchicalConfiguration("test-tinkergraph-shorthand"));

        try {
            GraphConfigurationContainer container = new GraphConfigurationContainer(configList);
            Assert.assertEquals(0, container.getFailedConfigurations().size());
            Assert.assertEquals(configList.size(), container.getApplicationGraphs().size());
        } catch (Exception ex) {
            Assert.fail(ex.getMessage());
        }
    }

    @Test
    public void getApplicationGraphsDynamicGraphs() {
        configList.add(constructHierarchicalConfiguration("test", "com.tinkerpop.rexster.config.MockGraphConfiguration", false));

        try {
            GraphConfigurationContainer container = new GraphConfigurationContainer(configList);
            Assert.assertEquals(0, container.getFailedConfigurations().size());
            Assert.assertEquals(configList.size(), container.getApplicationGraphs().size());
        } catch (Exception ex) {
            Assert.fail(ex.getMessage());
        }
    }

    @Test
    public void getApplicationGraphsReadOnlyGraphs() {
        configList.add(constructHierarchicalConfiguration("test", "com.tinkerpop.rexster.config.MockGraphConfiguration", true));

        try {
            GraphConfigurationContainer container = new GraphConfigurationContainer(configList);
            Assert.assertEquals(0, container.getFailedConfigurations().size());
            Assert.assertEquals(configList.size(), container.getApplicationGraphs().size());
            Assert.assertTrue(container.getApplicationGraphs().get("test").getGraph() instanceof ReadOnlyGraph);
            Assert.assertFalse(container.getApplicationGraphs().get("test").getGraph() instanceof ReadOnlyIndexableGraph);
        } catch (Exception ex) {
            Assert.fail(ex.getMessage());
        }
    }

    @Test
    public void getApplicationGraphsReadOnlyIndexableGraphs() {
        configList.add(constructHierarchicalConfiguration("test", "com.tinkerpop.rexster.config.MockIndexableGraphConfiguration", true));

        try {
            GraphConfigurationContainer container = new GraphConfigurationContainer(configList);
            Assert.assertEquals(0, container.getFailedConfigurations().size());
            Assert.assertEquals(configList.size(), container.getApplicationGraphs().size());
            Assert.assertTrue(container.getApplicationGraphs().get("test").getGraph() instanceof ReadOnlyIndexableGraph);
        } catch (Exception ex) {
            Assert.fail(ex.getMessage());
        }
    }

    private HierarchicalConfiguration constructBadHierarchicalConfiguration() {
        HierarchicalConfiguration graphConfig = new HierarchicalConfiguration();
        graphConfig.addProperty(Tokens.REXSTER_GRAPH_NAME, "junk");
        graphConfig.addProperty(Tokens.REXSTER_GRAPH_TYPE, "com.tinkerpop.rexster.config.MockBadGraphConfiguration");
        graphConfig.addProperty(Tokens.REXSTER_GRAPH_READ_ONLY, false);

        return graphConfig;
    }

    private HierarchicalConfiguration constructDefaultTinkerGraphHierarchicalConfiguration(String graphName) {
        return constructHierarchicalConfiguration(graphName, "tinkergraph", false);
    }

    private HierarchicalConfiguration constructTinkerGraphHierarchicalConfiguration(String graphName) {
        return constructHierarchicalConfiguration(graphName, "tinkergraph", false);
    }

    private HierarchicalConfiguration constructHierarchicalConfiguration(String graphName, String graphType, boolean readOnly) {
        HierarchicalConfiguration graphConfig = new HierarchicalConfiguration();
        graphConfig.addProperty(Tokens.REXSTER_GRAPH_NAME, graphName);
        graphConfig.addProperty(Tokens.REXSTER_GRAPH_TYPE, graphType);
        graphConfig.addProperty(Tokens.REXSTER_GRAPH_READ_ONLY, readOnly);

        ArrayList<HierarchicalConfiguration.Node> listOfNodes = new ArrayList<HierarchicalConfiguration.Node>();
        listOfNodes.add(new HierarchicalConfiguration.Node("username", "me"));
        listOfNodes.add(new HierarchicalConfiguration.Node("password", "pass"));
        graphConfig.addNodes(Tokens.REXSTER_GRAPH_PROPERTIES, listOfNodes);

        return graphConfig;
    }
}
