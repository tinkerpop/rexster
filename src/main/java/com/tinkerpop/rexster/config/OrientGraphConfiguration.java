package com.tinkerpop.rexster.config;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.SubnodeConfiguration;

import com.tinkerpop.blueprints.pgm.Graph;
import com.tinkerpop.blueprints.pgm.impls.orientdb.OrientGraph;
import com.tinkerpop.rexster.Tokens;

public class OrientGraphConfiguration implements GraphConfiguration {

	@Override
	public Graph configureGraphInstance(Configuration properties)  throws GraphConfigurationException {
		try {
	        String graphFile = properties.getString(Tokens.REXSTER_GRAPH_FILE);
	        
			// get the <properties> section of the xml configuration
	        HierarchicalConfiguration graphSectionConfig = (HierarchicalConfiguration) properties;
	        SubnodeConfiguration orientDbSpecificConfiguration = graphSectionConfig.configurationAt(Tokens.REXSTER_GRAPH_PROPERTIES);
	
	        String username = orientDbSpecificConfiguration.getString("username");
	        String password = orientDbSpecificConfiguration.getString("password");
	
	        // calling the open method opens the connection to graphdb.  looks like the 
	        // implementation of shutdown will call the orientdb close method.
	        return new OrientGraph(graphFile, username, password);

		} catch (Exception ex){
			throw new GraphConfigurationException(ex);
		}
	}

}
