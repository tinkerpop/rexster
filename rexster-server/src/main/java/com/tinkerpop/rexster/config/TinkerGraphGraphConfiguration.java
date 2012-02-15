package com.tinkerpop.rexster.config;

import com.tinkerpop.blueprints.pgm.Graph;
import com.tinkerpop.blueprints.pgm.impls.tg.TinkerGraph;
import com.tinkerpop.rexster.Tokens;
import com.tinkerpop.rexster.util.MockTinkerTransactionalGraph;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConversionException;
import org.apache.log4j.Logger;

public class TinkerGraphGraphConfiguration implements GraphConfiguration {
    private static final Logger logger = Logger.getLogger(GraphConfigurationContainer.class);

    public Graph configureGraphInstance(Configuration properties) throws GraphConfigurationException {

        String graphFile = properties.getString(Tokens.REXSTER_GRAPH_LOCATION, null);

        // determines if a mock transactional graph should be used for testing purposes.
        boolean mockTx;
        try {
            mockTx = properties.getBoolean("graph-mock-tx", false);
        } catch (ConversionException ce) {
            throw new GraphConfigurationException(ce);
        }

        try {
            if (graphFile == null || graphFile.length() == 0) {
                // pure in memory if graph file is specified
                return mockTx ? new MockTinkerTransactionalGraph() : new TinkerGraph();
            } else {
                return mockTx ? new MockTinkerTransactionalGraph(graphFile) : new TinkerGraph(graphFile);
            }
        } catch (Exception ex) {
            throw new GraphConfigurationException(ex);
        }
    }
}
