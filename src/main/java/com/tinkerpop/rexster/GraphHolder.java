package com.tinkerpop.rexster;

import com.tinkerpop.blueprints.pgm.Graph;
import com.tinkerpop.blueprints.pgm.impls.neo4j.Neo4jGraph;
import com.tinkerpop.blueprints.pgm.impls.tg.TinkerGraph;
import com.tinkerpop.blueprints.pgm.parser.GraphMLReader;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * author: Marko A. Rodriguez (http://markorodriguez.com)
 */
public class GraphHolder {

    private static Graph graph;

    public static void putGraph(final Graph graph) {
        GraphHolder.graph = graph;
    }

    public static Graph getGraph() {
        return GraphHolder.graph;
    }

    public static Graph loadGraphFromProperties(Properties properties) throws Exception {
        String graphType = properties.getProperty(RexsterTokens.REXSTER_GRAPH_TYPE);
        String graphFile = properties.getProperty(RexsterTokens.REXSTER_GRAPH_FILE);
        Graph graph;
        if (graphType.equals("neo4j")) {
            try {
                Properties neo4jProperties = new Properties();
                neo4jProperties.load(RexsterApplication.class.getResourceAsStream("neo4j.properties"));
                //logger.info("Loading Neo4j properties: " + neo4jProperties.toString());
                graph = new Neo4jGraph(graphFile, new HashMap<String, String>((Map) neo4jProperties));
            } catch (IOException e) {
                graph = new Neo4jGraph(graphFile);
            }
            ((Neo4jGraph) graph).setAutoTransactions(false);
        } else if (graphType.equals("tinkergraph")) {
            graph = new TinkerGraph();
            GraphMLReader.inputGraph(graph, new FileInputStream(graphFile));

        } else {
            throw new Exception(graphType + " is not a supported graph type");
        }
        return graph;
    }
}
