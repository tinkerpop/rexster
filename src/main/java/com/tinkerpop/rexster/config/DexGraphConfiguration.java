package com.tinkerpop.rexster.config;

import com.tinkerpop.blueprints.pgm.Graph;
import com.tinkerpop.blueprints.pgm.impls.dex.DexGraph;
import com.tinkerpop.rexster.Tokens;
import org.apache.commons.configuration.Configuration;

public class DexGraphConfiguration implements GraphConfiguration {

    public Graph configureGraphInstance(Configuration properties) throws GraphConfigurationException {

        String graphFile = properties.getString(Tokens.REXSTER_GRAPH_LOCATION, null);

        try {
            return new DexGraph(graphFile);
        } catch (Exception ex) {
            throw new GraphConfigurationException(ex);
        }
    }
}
