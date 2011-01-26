package com.tinkerpop.rexster.config;

import com.tinkerpop.blueprints.pgm.Graph;
import com.tinkerpop.blueprints.pgm.impls.tg.TinkerGraph;
import com.tinkerpop.blueprints.pgm.util.graphml.GraphMLReader;
import com.tinkerpop.rexster.Tokens;
import org.apache.commons.configuration.Configuration;

import java.io.FileInputStream;

public class TinkerGraphGraphConfiguration implements GraphConfiguration {

    @Override
    public Graph configureGraphInstance(Configuration properties) throws GraphConfigurationException {

        String graphFile = properties.getString(Tokens.REXSTER_GRAPH_FILE, null);

        try {
            TinkerGraph graph = new TinkerGraph();

            if (graphFile != null && graphFile.trim().length() > 0) {
                GraphMLReader.inputGraph(graph, new FileInputStream(graphFile));
            }

            return graph;
        } catch (Exception ex) {
            throw new GraphConfigurationException(ex);
        }
    }
}
