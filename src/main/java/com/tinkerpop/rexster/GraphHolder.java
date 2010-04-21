package com.tinkerpop.rexster;

import com.tinkerpop.blueprints.pgm.Graph;
import com.tinkerpop.blueprints.pgm.impls.neo4j.Neo4jGraph;
import com.tinkerpop.blueprints.pgm.impls.tg.TinkerGraph;
import com.tinkerpop.blueprints.pgm.parser.GraphMLReader;
import org.apache.log4j.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * author: Marko A. Rodriguez (http://markorodriguez.com)
 */
public class GraphHolder {

    protected static Logger logger = Logger.getLogger(GraphHolder.class);
    private static Graph graph;

    static {
        try {
            Properties properties = new Properties();
            properties.load(RexsterApplication.class.getResourceAsStream(RexsterTokens.REXSTER_PROPERTIES_FILE));
            String graphType = properties.getProperty(RexsterTokens.REXSTER_GRAPH_TYPE);
            String graphFile = properties.getProperty(RexsterTokens.REXSTER_GRAPH_FILE);
            if (graphType.equals("neo4j")) {
                Neo4jGraph tempGraph;
                try {
                    Properties neo4jProperties = new Properties();
                    neo4jProperties.load(RexsterApplication.class.getResourceAsStream("neo4j.properties"));
                    // todo: remove when happy.
                    logger.info("Loading Neo4j properties: " + neo4jProperties.toString());
                    tempGraph = new Neo4jGraph(graphFile, new HashMap<String, String>((Map) neo4jProperties));
                } catch (IOException e) {
                    tempGraph = new Neo4jGraph(graphFile);
                }
                tempGraph.setAutoTransactions(false);
                GraphHolder.graph = tempGraph;
            } else if (graphType.equals("tinkergraph")) {
                TinkerGraph tempGraph = new TinkerGraph();
                try {
                    GraphMLReader.inputGraph(tempGraph, new FileInputStream(graphFile));
                } catch (Exception e) {
                    logger.error(e.getMessage());
                }
                GraphHolder.graph = tempGraph;
            } else {
                logger.error(graphType + " is not a supported graph type");
                throw new Exception(graphType + " is not a supported graph type");
            }
            logger.info("Graph " + GraphHolder.graph + " loaded");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static void putGraph(final Graph graph) {
        GraphHolder.graph = graph;
    }

    public static Graph getGraph() {
        return GraphHolder.graph;
    }
}
