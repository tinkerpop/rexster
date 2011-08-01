package com.tinkerpop.rexster.config;

import com.tinkerpop.blueprints.pgm.Graph;
import com.tinkerpop.blueprints.pgm.impls.tg.TinkerGraph;
import com.tinkerpop.rexster.Tokens;
import org.apache.commons.configuration.Configuration;

public class TinkerGraphGraphConfiguration implements GraphConfiguration {

    public Graph configureGraphInstance(Configuration properties) throws GraphConfigurationException {

        String graphFile = properties.getString(Tokens.REXSTER_GRAPH_FILE, null);

        try {
            if (graphFile == null || graphFile.length() == 0) {
                // pure in memory if graph file is specified
                return new TinkerGraph();
            } else {
                return new TinkerGraph(graphFile);
            }
        } catch (Exception ex) {
            throw new GraphConfigurationException(ex);
        }
    }
}
