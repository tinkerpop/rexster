package com.tinkerpop.rexster.config;

import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.impls.dex.DexGraph;
import com.tinkerpop.rexster.Tokens;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.SubnodeConfiguration;

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 * @author Sergio Gomez Vilamor (https://github.com/sgomezvillamor)
 */
public class DexGraphConfiguration implements GraphConfiguration {

    private final static String DEX_CONFIGURATION_PROPERTY = "config-file";

    public Graph configureGraphInstance(final Configuration properties) throws GraphConfigurationException {

        final String graphFile = properties.getString(Tokens.REXSTER_GRAPH_LOCATION, null);

        if (graphFile == null || graphFile.length() == 0) {
            throw new GraphConfigurationException("Check graph configuration. Missing or empty configuration element: " + Tokens.REXSTER_GRAPH_LOCATION);
        }

        // get the <properties> section of the xml configuration
        final HierarchicalConfiguration graphSectionConfig = (HierarchicalConfiguration) properties;
        SubnodeConfiguration dexSpecificConfiguration;
        String dexconfig;

        try {
            // allow the properties to be optional
            dexSpecificConfiguration = graphSectionConfig.configurationAt(Tokens.REXSTER_GRAPH_PROPERTIES);
            dexconfig = dexSpecificConfiguration.getString(DEX_CONFIGURATION_PROPERTY, null);
        } catch (IllegalArgumentException iae) {
            dexconfig = null;
        }

        try {
            return new DexGraph(graphFile, dexconfig);
        } catch (Exception ex) {
            throw new GraphConfigurationException(ex);
        }
    }
}
