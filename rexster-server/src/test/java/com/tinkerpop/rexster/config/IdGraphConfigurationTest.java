package com.tinkerpop.rexster.config;

import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.impls.sail.impls.LinkedDataSailGraph;
import com.tinkerpop.blueprints.util.wrappers.id.IdGraph;
import com.tinkerpop.rexster.RexsterApplicationGraph;
import com.tinkerpop.rexster.Tokens;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class IdGraphConfigurationTest {

    private IdGraphConfiguration configuration = new IdGraphConfiguration();

    @Test
    public void configureIdGraphOnTinkerGraph() throws Exception {
        // first configure the base graph
        HierarchicalConfiguration graphConfig = new HierarchicalConfiguration();
        graphConfig.setProperty(Tokens.REXSTER_GRAPH_LOCATION, "");
        Map<String, RexsterApplicationGraph> graphs = new HashMap<String, RexsterApplicationGraph>();
        GraphConfigurationContext context = new GraphConfigurationContext(graphConfig, graphs);
        TinkerGraphGraphConfiguration conf = new TinkerGraphGraphConfiguration();
        Graph baseGraph = conf.configureGraphInstance(context);

        graphs.put("idbase", new RexsterApplicationGraph("idbase", baseGraph));

        // now configure IdGraph
        graphConfig = new HierarchicalConfiguration();
        context = new GraphConfigurationContext(graphConfig, graphs);
        graphConfig.setProperty(Tokens.REXSTER_GRAPH_LOCATION, "idbase");
        ArrayList<HierarchicalConfiguration.Node> listOfNodes = new ArrayList<HierarchicalConfiguration.Node>();
        listOfNodes.add(new HierarchicalConfiguration.Node("supportEdgeIds", "false"));
        graphConfig.addNodes(Tokens.REXSTER_GRAPH_PROPERTIES, listOfNodes);
        this.configuration.configureGraphInstance(context);

        Graph graph = this.configuration.configureGraphInstance(context);
        assertTrue(graph instanceof IdGraph);
        assertTrue(((IdGraph) graph).getSupportVertexIds());
        assertFalse(((IdGraph) graph).getSupportEdgeIds());
    }

    @Test(expected = GraphConfigurationException.class)
    public void configureIdGraphNoBaseGraph() throws Exception {
        Map<String, RexsterApplicationGraph> graphs = new HashMap<String, RexsterApplicationGraph>();
        HierarchicalConfiguration graphConfig = new HierarchicalConfiguration();
        GraphConfigurationContext context = new GraphConfigurationContext(graphConfig, graphs);
        this.configuration.configureGraphInstance(context);
    }

    @Test(expected = GraphConfigurationException.class)
    public void configureIdGraphBaseGraphNotFound() throws Exception {
        Map<String, RexsterApplicationGraph> graphs = new HashMap<String, RexsterApplicationGraph>();
        HierarchicalConfiguration graphConfig = new HierarchicalConfiguration();
        GraphConfigurationContext context = new GraphConfigurationContext(graphConfig, graphs);
        graphConfig.setProperty(Tokens.REXSTER_GRAPH_LOCATION, "idbase");
        this.configuration.configureGraphInstance(context);
    }
}
