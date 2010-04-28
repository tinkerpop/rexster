package com.tinkerpop.rexster;

import com.tinkerpop.blueprints.pgm.Graph;
import com.tinkerpop.blueprints.pgm.impls.neo4j.Neo4jGraph;
import com.tinkerpop.blueprints.pgm.impls.tg.TinkerGraph;
import com.tinkerpop.blueprints.pgm.parser.GraphMLReader;
import com.tinkerpop.rexster.traversals.Traversal;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.restlet.Application;
import org.restlet.Restlet;
import org.restlet.routing.Router;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;

/**
 * @author: Marko A. Rodriguez (http://markorodriguez.com)
 */
public class RexsterApplication extends Application {

    protected static final Logger logger = Logger.getLogger(RexsterApplication.class);
    private Graph graph;
    private ResultObjectCache resultObjectCache;

    static {
        PropertyConfigurator.configure(RexsterApplication.class.getResource("log4j.properties"));
    }

    public RexsterApplication(final Graph graph) {
        this.graph = graph;
        logger.info("Graph " + this.graph + " loaded");
        this.resultObjectCache = new ResultObjectCache();
    }

    public RexsterApplication(final Properties properties) {
        try {
            this.graph = createGraphFromProperties(properties);
            logger.info("Graph " + this.graph + " loaded");
            this.resultObjectCache = new ResultObjectCache(properties);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Restlet createRoot() {
        Router router = new Router(getContext());
        router.attachDefault(RexsterResource.class);
        ServiceLoader<Traversal> traversalServices = ServiceLoader.load(Traversal.class);
        for (Traversal traversalService : traversalServices) {
            logger.info("loading traversal: /" + traversalService.getResourceName() + " [" + traversalService.getClass().getName() + "]");
            router.attach("/" + traversalService.getResourceName(), traversalService.getClass());
        }
        return router;
    }

    public Graph getGraph() {
        return this.graph;
    }

    public ResultObjectCache getResultObjectCache() {
        return this.resultObjectCache;
    }

    public void stop() throws Exception {
        super.stop();
        logger.info("Shutting down " + this.graph);
        this.graph.shutdown();
    }

    private static Graph createGraphFromProperties(final Properties properties) throws Exception {
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
