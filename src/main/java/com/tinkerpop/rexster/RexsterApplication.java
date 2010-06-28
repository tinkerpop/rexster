package com.tinkerpop.rexster;

import com.tinkerpop.blueprints.pgm.Graph;
import com.tinkerpop.blueprints.pgm.impls.neo4j.Neo4jGraph;
import com.tinkerpop.blueprints.pgm.impls.tg.TinkerGraph;
import com.tinkerpop.blueprints.pgm.parser.GraphMLReader;
import com.tinkerpop.rexster.traversals.Traversal;
import com.tinkerpop.rexster.util.EdgeResource;
import com.tinkerpop.rexster.util.VertexResource;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.restlet.Application;
import org.restlet.Restlet;
import org.restlet.routing.Router;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class RexsterApplication extends Application {

    //private static final String version = "0.1-SNAPSHOT";
    private final long startTime = System.currentTimeMillis();
    protected static final Logger logger = Logger.getLogger(RexsterApplication.class);
    private Graph graph;
    private ResultObjectCache resultObjectCache;
    private Properties properties;
    private Map<String, Class> loadedTraversals = new HashMap<String, Class>();

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
            this.properties = properties;
            this.graph = createGraphFromProperties(this.properties);
            logger.info("Graph " + this.graph + " loaded");
            this.resultObjectCache = new ResultObjectCache(this.properties);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // todo: clean up
    public Restlet createRoot() {
        Router router = new Router(getContext());
        router.attachDefault(RexsterResource.class);
        ServiceLoader<Traversal> traversalServices = ServiceLoader.load(Traversal.class);
        String packages = this.properties.getProperty(RexsterTokens.REXSTER_PACKAGES_ALLOWED);
        if (null != packages) {
            Set<String> packageNames = new HashSet<String>(Arrays.asList(packages.substring(1, packages.length() - 1).split(",")));
            //System.out.println(packageNames);
            for (Traversal traversalService : traversalServices) {
                if (-1 == traversalService.getTraversalName().indexOf("/")) {
                    if (packageNames.contains("")) {
                        logger.info("loading traversal: /" + traversalService.getTraversalName() + " [" + traversalService.getClass().getName() + "]");
                        router.attach("/" + traversalService.getTraversalName(), traversalService.getClass());
                        this.loadedTraversals.put(traversalService.getTraversalName(), traversalService.getClass());
                    }
                } else {
                    if (packageNames.contains(traversalService.getTraversalName().substring(0, traversalService.getTraversalName().indexOf("/")))) {
                        logger.info("loading traversal: /" + traversalService.getTraversalName() + " [" + traversalService.getClass().getName() + "]");
                        router.attach("/" + traversalService.getTraversalName(), traversalService.getClass());
                        this.loadedTraversals.put(traversalService.getTraversalName(), traversalService.getClass());
                    }
                }
            }
        } else {
            for (Traversal traversalService : traversalServices) {
                logger.info("loading traversal: /" + traversalService.getTraversalName() + " [" + traversalService.getClass().getName() + "]");
                router.attach("/" + traversalService.getTraversalName(), traversalService.getClass());
                this.loadedTraversals.put(traversalService.getTraversalName(), traversalService.getClass());
            }
        }
        router.attach("/vertices/{id}", VertexResource.class);
        router.attach("/vertices", VertexResource.class);
        router.attach("/edges", EdgeResource.class);
        router.attach("/vertices/{id}/{direction}", VertexResource.class);
        return router;
    }

    public Map<String, Class> getLoadedTraversalServices() {
        return this.loadedTraversals;
    }

    public Graph getGraph() {
        return this.graph;
    }

    public ResultObjectCache getResultObjectCache() {
        return this.resultObjectCache;
    }

    public long getStartTime() {
        return this.startTime;
    }

    public void stop() throws Exception {
        super.stop();
        logger.info("Shutting down " + this.graph);
        this.graph.shutdown();
    }

    protected static Graph createGraphFromProperties(final Properties properties) throws Exception {
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
