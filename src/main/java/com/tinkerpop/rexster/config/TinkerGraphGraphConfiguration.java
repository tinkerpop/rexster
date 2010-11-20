package com.tinkerpop.rexster.config;

import java.io.FileInputStream;

import org.apache.commons.configuration.Configuration;

import com.tinkerpop.blueprints.pgm.Graph;
import com.tinkerpop.blueprints.pgm.impls.tg.TinkerGraph;
import com.tinkerpop.blueprints.pgm.parser.GraphMLReader;
import com.tinkerpop.rexster.Tokens;

public class TinkerGraphGraphConfiguration implements GraphConfiguration {

	@Override
	public Graph configureGraphInstance(Configuration properties) throws GraphConfigurationException{
		try {
	        String graphFile = properties.getString(Tokens.REXSTER_GRAPH_FILE);
	        
			TinkerGraph graph = new TinkerGraph();
	        if (null != graphFile){
	            GraphMLReader.inputGraph(graph, new FileInputStream(graphFile));
	        }
	        
	        return graph;
		} catch (Exception ex){
			throw new GraphConfigurationException(ex);
		}
	}
}
