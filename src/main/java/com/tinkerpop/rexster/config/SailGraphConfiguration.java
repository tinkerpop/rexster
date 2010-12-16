package com.tinkerpop.rexster.config;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.SubnodeConfiguration;

import com.tinkerpop.blueprints.pgm.Graph;
import com.tinkerpop.blueprints.pgm.impls.sail.SailGraph;
import com.tinkerpop.blueprints.pgm.impls.sail.impls.MemoryStoreSailGraph;
import com.tinkerpop.blueprints.pgm.impls.sail.impls.NativeStoreSailGraph;
import com.tinkerpop.blueprints.pgm.impls.sail.impls.Neo4jSailGraph;
import com.tinkerpop.rexster.Tokens;

public class SailGraphConfiguration implements GraphConfiguration {
	
	private static final String SAIL_TYPE_MEMORY = "memory";
	private static final String SAIL_TYPE_NATIVE = "native";
	private static final String SAIL_TYPE_NEO4J = "neo4j";

	@Override
	public Graph configureGraphInstance(Configuration properties) throws GraphConfigurationException {
		String graphFile = properties.getString(Tokens.REXSTER_GRAPH_FILE, null);

		// get the <properties> section of the xml configuration
        HierarchicalConfiguration graphSectionConfig = (HierarchicalConfiguration) properties;
        SubnodeConfiguration sailSpecificConfiguration;

        try {
        	sailSpecificConfiguration = graphSectionConfig.configurationAt(Tokens.REXSTER_GRAPH_PROPERTIES);
        } catch (IllegalArgumentException iae) {
            throw new GraphConfigurationException("Check graph configuration. Missing or empty configuration element: " + Tokens.REXSTER_GRAPH_PROPERTIES);
        }
        
        String sailType = sailSpecificConfiguration.getString("blueprints-sail-type", "");
        if (!sailType.equals(SAIL_TYPE_MEMORY) 
        	&& !sailType.equals(SAIL_TYPE_NATIVE) 
        	&& !sailType.equals(SAIL_TYPE_NEO4J)) {
            throw new GraphConfigurationException("Check graph configuration. Missing, empty or incorrect configuration element: properties/blueprints-sail-type");
        }
        
        // graphfile must be present for native and neo4j
        if ((sailType.equals(SAIL_TYPE_NATIVE) || sailType.equals(SAIL_TYPE_NEO4J) 
        		&& (graphFile == null || graphFile.trim().length() == 0))){
        	throw new GraphConfigurationException("Check graph configuration. Missing or empty configuration element: " + Tokens.REXSTER_GRAPH_FILE);
    	}
	
        try {
        	
            SailGraph graph = null;
            
            if (sailType.equals(SAIL_TYPE_MEMORY)) {
            	if (graphFile != null && graphFile.trim().length() > 0) {
            		graph = new MemoryStoreSailGraph(graphFile);
            	} else {
            		graph = new MemoryStoreSailGraph();
            	}
            } else if (sailType.equals(SAIL_TYPE_NATIVE)) {
            	String configTripleIndices = sailSpecificConfiguration.getString("triple-indices", "");
                
            	if (configTripleIndices  != null && configTripleIndices.trim().length() > 0){
            		graph = new NativeStoreSailGraph(graphFile, configTripleIndices);
            	} else {
            		graph = new NativeStoreSailGraph(graphFile);
            	}
            } else if (sailType.equals(SAIL_TYPE_NEO4J)) {
            	graph = new Neo4jSailGraph(graphFile);
            }

            return graph;
        } catch (Exception ex) {
            throw new GraphConfigurationException(ex);
        }
	}

}
