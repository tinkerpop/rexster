package com.tinkerpop.rexster.config;

import com.tinkerpop.blueprints.pgm.Graph;
import com.tinkerpop.blueprints.pgm.impls.orientdb.OrientGraph;
import com.tinkerpop.rexster.Tokens;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.SubnodeConfiguration;

public class OrientGraphConfiguration implements GraphConfiguration {

    public Graph configureGraphInstance(Configuration properties) throws GraphConfigurationException {

        String graphFile = properties.getString(Tokens.REXSTER_GRAPH_LOCATION);

        if (graphFile == null || graphFile.length() == 0) {
            throw new GraphConfigurationException("Check graph configuration. Missing or empty configuration element: " + Tokens.REXSTER_GRAPH_LOCATION);
        }

        // get the <properties> section of the xml configuration
        HierarchicalConfiguration graphSectionConfig = (HierarchicalConfiguration) properties;
        SubnodeConfiguration orientDbSpecificConfiguration;

        try {
            orientDbSpecificConfiguration = graphSectionConfig.configurationAt(Tokens.REXSTER_GRAPH_PROPERTIES);
        } catch (IllegalArgumentException iae) {
            throw new GraphConfigurationException("Check graph configuration. Missing or empty configuration element: " + Tokens.REXSTER_GRAPH_PROPERTIES);
        }

        try {

            String username = orientDbSpecificConfiguration.getString("username", "");
            String password = orientDbSpecificConfiguration.getString("password", "");

            // calling the open method opens the connection to graphdb.  looks like the
            // implementation of shutdown will call the orientdb close method.
            return new OrientGraph(graphFile, username, password);

        } catch (Exception ex) {
            throw new GraphConfigurationException(ex);
        }
    }

}
