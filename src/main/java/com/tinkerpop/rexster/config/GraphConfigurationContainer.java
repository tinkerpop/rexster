package com.tinkerpop.rexster.config;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.log4j.Logger;

import com.tinkerpop.blueprints.pgm.Graph;
import com.tinkerpop.rexster.RexsterApplicationGraph;
import com.tinkerpop.rexster.Tokens;

public class GraphConfigurationContainer {
	
	protected static final Logger logger = Logger.getLogger(GraphConfigurationContainer.class);
	
	private List<HierarchicalConfiguration> configurations;
	
	private Map<String, RexsterApplicationGraph> graphs = new HashMap<String, RexsterApplicationGraph>();
	
	public GraphConfigurationContainer(List<HierarchicalConfiguration> configurations) {		
		
		// the set of graph configurations
		this.configurations = configurations;
		
		// create one graph for each configuration for each <graph> element
        Iterator<HierarchicalConfiguration> it = this.configurations.iterator();
        while (it.hasNext()) {
            HierarchicalConfiguration graphConfig = it.next();
            String graphName = graphConfig.getString(Tokens.REXSTER_GRAPH_NAME);
            boolean enabled = graphConfig.getBoolean(Tokens.REXSTER_GRAPH_ENABLED, true);

            if (enabled) {
            	
                // one graph failing initialization will not prevent the rest in
                // their attempt to be created
                try {
                    Graph graph = getGraphFromConfiguration(graphConfig);
                    RexsterApplicationGraph rag = new RexsterApplicationGraph(graphName, graph);
                    rag.loadPackageNames(graphConfig.getString(Tokens.REXSTER_PACKAGES_ALLOWED));

                    this.graphs.put(rag.getGraphName(), rag);

                    logger.info("Graph " + graphName + " - " + graph + " loaded");
                } catch (Exception e) {
                    logger.warn("Could not load graph " + graphName + ". Please check the XML configuration.", e);
                }
            } else {
                logger.info("Graph " + graphName + " - " + " not enabled and not loaded.");
            }
        }
	}
	
	public Map<String, RexsterApplicationGraph> getApplicationGraphs(){
		return this.graphs;
	}
	
	private Graph getGraphFromConfiguration(HierarchicalConfiguration graphConfiguration) throws GraphConfigurationException {
		String graphConfigurationType = graphConfiguration.getString(Tokens.REXSTER_GRAPH_TYPE);

        if (graphConfigurationType.equals("neo4j")) {
        	graphConfigurationType = Neo4jGraphConfiguration.class.getName();
        } else if (graphConfigurationType.equals("orientdb")) {
        	graphConfigurationType = OrientGraphConfiguration.class.getName();
        } else if (graphConfigurationType.equals("tinkergraph")) {
            graphConfigurationType = TinkerGraphGraphConfiguration.class.getName();
        } 
        
        Graph graph = null;
        Class clazz = null;
        GraphConfiguration graphConfigInstance = null;
        try {
        	clazz = Class.forName(graphConfigurationType, true, Thread.currentThread().getContextClassLoader());
        	graphConfigInstance = (GraphConfiguration) clazz.newInstance();
        	graph = graphConfigInstance.configureGraphInstance(graphConfiguration);
        } catch (Exception ex) {
        	throw new GraphConfigurationException(
        			"GraphConfiguration could not be found or otherwise instantiated:." + graphConfigurationType, ex);
        }
        
        return graph;
	}
}
