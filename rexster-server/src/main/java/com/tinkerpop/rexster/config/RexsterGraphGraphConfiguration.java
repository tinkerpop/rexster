package com.tinkerpop.rexster.config;

import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.impls.rexster.RexsterGraph;
import com.tinkerpop.rexster.Tokens;
import org.apache.commons.configuration.Configuration;

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class RexsterGraphGraphConfiguration implements GraphConfiguration {

    public static final int DEFAULT_BUFFER_SIZE = 100;

    public Graph configureGraphInstance(final Configuration properties) throws GraphConfigurationException {

        final String rexsterGraphUriToConnectTo;
        final int bufferSize;

        try {
            rexsterGraphUriToConnectTo = properties.getString(Tokens.REXSTER_GRAPH_LOCATION, null);
            bufferSize = properties.getInt(Tokens.REXSTER_GRAPH_BUFFER_SIZE, DEFAULT_BUFFER_SIZE);
        } catch (Exception ex) {
            throw new GraphConfigurationException(ex);
        }

        RexsterGraph graph = null;
        try {
            graph = new RexsterGraph(rexsterGraphUriToConnectTo, bufferSize);
        } catch (RuntimeException rte) {
            // if the remote server is down just ignore the error for the moment.  let
            // Rexster think the graph configuration is good.  the server may be up later.
        }

        return graph;
    }
}
