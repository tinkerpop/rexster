package com.tinkerpop.rexster.config;

import com.tinkerpop.blueprints.pgm.Graph;
import com.tinkerpop.blueprints.pgm.impls.rexster.RexsterGraph;
import com.tinkerpop.blueprints.pgm.util.graphml.GraphMLReader;
import com.tinkerpop.rexster.Tokens;
import org.apache.commons.configuration.Configuration;
import org.omg.CORBA.PUBLIC_MEMBER;

public class RexsterGraphGraphConfiguration implements GraphConfiguration {

    public static final int DEFAULT_BUFFER_SIZE = 100;

    public Graph configureGraphInstance(Configuration properties) throws GraphConfigurationException {

        try {
            String rexsterGraphUriToConnectTo = properties.getString(Tokens.REXSTER_GRAPH_FILE, null);
            int bufferSize = properties.getInt(Tokens.REXSTER_GRAPH_BUFFER_SIZE, DEFAULT_BUFFER_SIZE);
            RexsterGraph graph = new RexsterGraph(rexsterGraphUriToConnectTo, bufferSize);

            return graph;
        } catch (Exception ex) {
            throw new GraphConfigurationException(ex);
        }

    }
}
