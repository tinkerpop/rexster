package com.tinkerpop.rexster.config;

import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.impls.tg.TinkerGraph;
import com.tinkerpop.rexster.Tokens;
import com.tinkerpop.rexster.util.MockTinkerTransactionalGraph;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConversionException;

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class TinkerGraphGraphConfiguration implements GraphConfiguration {

    public Graph configureGraphInstance(final Configuration properties) throws GraphConfigurationException {

        final String graphFile = properties.getString(Tokens.REXSTER_GRAPH_LOCATION, null);

        // determines if a mock transactional graph should be used for testing purposes.
        boolean mockTx;
        try {
            mockTx = properties.getBoolean("graph-mock-tx", false);
        } catch (ConversionException ce) {
            throw new GraphConfigurationException(ce);
        }

        String fileType;
        try {
            fileType = properties.getString("graph-storage", "");
        } catch (IllegalArgumentException iae) {
            // default to java serialization
            fileType = "";
        }

        try {
            if (graphFile == null || graphFile.length() == 0) {
                // pure in memory if graph file is specified
                return mockTx ? new MockTinkerTransactionalGraph() : new TinkerGraph();
            } else {
                return mockTx ? new MockTinkerTransactionalGraph(graphFile, getFileType(fileType)) : new TinkerGraph(graphFile, getFileType(fileType));
            }
        } catch (Exception ex) {
            throw new GraphConfigurationException(ex);
        }
    }

    private TinkerGraph.FileType getFileType(final String fileType)
    {
        String fileTypeLower = fileType.toLowerCase();

        if (fileTypeLower.equals("graphson")) {
            return TinkerGraph.FileType.GRAPHSON;
        }
        else if (fileTypeLower.equals("graphml")) {
            return TinkerGraph.FileType.GRAPHML;
        }
        else if (fileTypeLower.equals(("gml"))) {
            return TinkerGraph.FileType.GML;
        }
        else {
            return TinkerGraph.FileType.JAVA;
        }
    }
}
