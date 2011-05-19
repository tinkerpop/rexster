package com.tinkerpop.rexster.config;

import com.tinkerpop.blueprints.pgm.Graph;
import com.tinkerpop.blueprints.pgm.impls.rexster.RexsterGraph;
import com.tinkerpop.blueprints.pgm.util.graphml.GraphMLReader;
import com.tinkerpop.rexster.Tokens;
import org.apache.commons.configuration.Configuration;

public class RexsterGraphGraphConfiguration implements GraphConfiguration {

    public Graph configureGraphInstance(Configuration properties) throws GraphConfigurationException {

        String rexsterGraphUriToConnectTo = properties.getString(Tokens.REXSTER_GRAPH_FILE, null);

        try {
            RexsterGraph graph = new RexsterGraph(rexsterGraphUriToConnectTo);

            return graph;
        } catch (Exception ex) {
            throw new GraphConfigurationException(ex);
        }
    }
}
