package com.tinkerpop.rexster.config;

import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.impls.sparksee.SparkseeGraph;
import com.tinkerpop.rexster.Tokens;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.SubnodeConfiguration;

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 * @author Sergio Gomez Vilamor (https://github.com/sgomezvillamor)
 */
public class SparkseeGraphConfiguration implements GraphConfiguration {

    private final static String SPARKSEE_CONFIGURATION_PROPERTY = "config-file";

    public Graph configureGraphInstance(final GraphConfigurationContext context) throws GraphConfigurationException {

        final String graphFile = context.getProperties().getString(Tokens.REXSTER_GRAPH_LOCATION, null);

        if (graphFile == null || graphFile.length() == 0) {
            throw new GraphConfigurationException("Check graph configuration. Missing or empty configuration element: " + Tokens.REXSTER_GRAPH_LOCATION);
        }

        // get the <properties> section of the xml configuration
        final HierarchicalConfiguration graphSectionConfig = (HierarchicalConfiguration) context.getProperties();
        SubnodeConfiguration dexSpecificConfiguration;
        String sparkseeConfig;

        try {
            // allow the properties to be optional
            dexSpecificConfiguration = graphSectionConfig.configurationAt(Tokens.REXSTER_GRAPH_PROPERTIES);
            sparkseeConfig = dexSpecificConfiguration.getString(SPARKSEE_CONFIGURATION_PROPERTY, null);
        } catch (IllegalArgumentException iae) {
            sparkseeConfig = null;
        }

        try {
            return new SparkseeGraph(graphFile, sparkseeConfig);
        } catch (Exception ex) {
            throw new GraphConfigurationException(ex);
        }
    }
}
