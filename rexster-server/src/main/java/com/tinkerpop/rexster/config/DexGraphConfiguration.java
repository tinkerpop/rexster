package com.tinkerpop.rexster.config;

import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.impls.dex.DexGraph;
import com.tinkerpop.rexster.Tokens;
import org.apache.commons.configuration.Configuration;

public class DexGraphConfiguration implements GraphConfiguration {

    public Graph configureGraphInstance(final Configuration properties) throws GraphConfigurationException {

        final String graphFile = properties.getString(Tokens.REXSTER_GRAPH_LOCATION, null);

        try {
            return new DexGraph(graphFile);
        } catch (Exception ex) {
            throw new GraphConfigurationException(ex);
        }
    }
}
