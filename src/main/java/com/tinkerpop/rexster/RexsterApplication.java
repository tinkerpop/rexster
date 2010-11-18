package com.tinkerpop.rexster;

import com.tinkerpop.blueprints.pgm.Graph;
import com.tinkerpop.blueprints.pgm.impls.neo4j.Neo4jGraph;
import com.tinkerpop.blueprints.pgm.impls.orientdb.OrientGraph;
import com.tinkerpop.blueprints.pgm.impls.tg.TinkerGraph;
import com.tinkerpop.blueprints.pgm.parser.GraphMLReader;
import com.tinkerpop.rexster.traversals.Traversal;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.io.FileInputStream;
import java.util.*;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class RexsterApplication {

    protected static final Logger logger = Logger.getLogger(RexsterApplication.class);

    private static final String version = Tokens.REXSTER_VERSION;
    private final long startTime = System.currentTimeMillis();
    private ResultObjectCache resultObjectCache;

    private Map<String, RexsterApplicationGraph> graphs = new HashMap<String, RexsterApplicationGraph>();

    static {
        PropertyConfigurator.configure(RexsterApplication.class.getResource("log4j.properties"));
    }

    public RexsterApplication(final String graphName, final Graph graph) {
        RexsterApplicationGraph rag = new RexsterApplicationGraph(graphName, graph);
        this.graphs.put(graphName, rag);
        logger.info("Graph " + rag.getGraph() + " loaded");
        this.resultObjectCache = new ResultObjectCache();
    }

    public RexsterApplication(final XMLConfiguration properties) {

        // get the graph configurations from the XML config file
        List<HierarchicalConfiguration> graphConfigs = properties.configurationsAt(Tokens.REXSTER_GRAPH_PATH);

        // create one graph for each configuration for each <graph> element
        Iterator<HierarchicalConfiguration> it = graphConfigs.iterator();
        while (it.hasNext()) {
            HierarchicalConfiguration graphConfig = it.next();
            String graphName = graphConfig.getString(Tokens.REXSTER_GRAPH_NAME);
            boolean enabled = graphConfig.getBoolean(Tokens.REXSTER_GRAPH_ENABLED, true);

            if (enabled) {
                // one graph failing initialization will not prevent the rest in
                // their attempt to be created
                try {
                    Graph graph = createGraphFromProperties(graphConfig);
                    RexsterApplicationGraph rag = new RexsterApplicationGraph(graphName, graph);
                    rag.loadPackageNames(graphConfig.getString(Tokens.REXSTER_PACKAGES_ALLOWED));

                    this.graphs.put(rag.getGraphName(), rag);

                    logger.info("Graph " + graphName + " - " + graph + " loaded");
                } catch (Exception e) {
                    logger.warn("Could not load graph " + graphName + ". Please check the XML configuration.", e);
                }
            } else {
                logger.info("Graph " + graphName + " - " + " not enabled and not loaded.");
            }
        }

        try {
            initTraversals();
            logger.info("Traversal initialization completed.");
        } catch (Exception e) {
            logger.error("Traversal initialization failed.", e);
        }

        this.resultObjectCache = new ResultObjectCache();

    }

    private void initTraversals() {
        // get a list of all traversal implementations.
        ServiceLoader<? extends Traversal> traversalServices = ServiceLoader.load(Traversal.class);

        for (RexsterApplicationGraph rag : this.graphs.values()) {

            if (rag.hasPackages()) {
                for (Traversal traversalService : traversalServices) {
                    if (-1 == traversalService.getTraversalName().indexOf("/")) {
                        if (rag.getPackageNames().contains("")) {
                            logger.info("loading traversal: /" + rag.getGraphName() + "/" + traversalService.getTraversalName() + " [" + traversalService.getClass().getName() + "]");
                            rag.getLoadedTraversals().put(traversalService.getTraversalName(), traversalService.getClass());
                        }
                    } else {
                        if (rag.getPackageNames().contains(traversalService.getTraversalName().substring(0, traversalService.getTraversalName().indexOf("/")))) {
                            logger.info("loading traversal: /" + rag.getGraphName() + "/" + traversalService.getTraversalName() + " [" + traversalService.getClass().getName() + "]");
                            rag.getLoadedTraversals().put(traversalService.getTraversalName(), traversalService.getClass());
                        }
                    }
                }
            }
        }
    }

    public Map<String, Class<? extends Traversal>> getLoadedTraversalServices(String graphName) {
        return this.graphs.get(graphName).getLoadedTraversals();
    }

    public static String getVersion() {
        return version;
    }

    public Graph getGraph(String graphName) {
        return this.graphs.get(graphName).getGraph();
    }

    public RexsterApplicationGraph getApplicationGraph(String graphName) {
        return this.graphs.get(graphName);
    }
    
    public Set<String> getGraphsNames(){
    	return this.graphs.keySet();
    }

    public ResultObjectCache getResultObjectCache() {
        return this.resultObjectCache;
    }

    public long getStartTime() {
        return this.startTime;
    }

    public void stop() throws Exception {

        // need to shutdown all the graphs that were started with the web server
        for (RexsterApplicationGraph rag : this.graphs.values()) {

            Graph graph = rag.getGraph();
            logger.info("Shutting down " + graph);

            // graph may not have been initialized properly if an exception gets tossed in
            // on graph creation
            if (graph != null) {
                graph.shutdown();
            }
        }

    }

    protected static Graph createGraphFromProperties(final Configuration properties) throws Exception {

        String graphType = properties.getString(Tokens.REXSTER_GRAPH_TYPE);
        String graphFile = properties.getString(Tokens.REXSTER_GRAPH_FILE);

        Graph graph;
        if (graphType.equals("neo4j")) {

            // get the <properties> section of the xml configuration
            HierarchicalConfiguration graphSectionConfig = (HierarchicalConfiguration) properties;
            SubnodeConfiguration neo4jSpecificConfiguration = graphSectionConfig.configurationAt(Tokens.REXSTER_GRAPH_PROPERTIES);

            // properties to initialize the neo4j instance.
            HashMap<String, String> neo4jProperties = new HashMap<String, String>();

            // read the properties from the xml file and convert them to properties
            // to be injected into neo4j.
            Iterator<String> neo4jSpecificConfigurationKeys = neo4jSpecificConfiguration.getKeys();
            while (neo4jSpecificConfigurationKeys.hasNext()) {
                String key = neo4jSpecificConfigurationKeys.next();
                neo4jProperties.put(key, neo4jSpecificConfiguration.getString(key));
            }

            graph = new Neo4jGraph(graphFile, neo4jProperties);
        } else if (graphType.equals("orientdb")) {

            // get the <properties> section of the xml configuration
            HierarchicalConfiguration graphSectionConfig = (HierarchicalConfiguration) properties;
            SubnodeConfiguration orientDbSpecificConfiguration = graphSectionConfig.configurationAt(Tokens.REXSTER_GRAPH_PROPERTIES);

            String username = orientDbSpecificConfiguration.getString("username");
            String password = orientDbSpecificConfiguration.getString("password");

            // calling the open method opens the connection to graphdb.  looks like the 
            // implementation of shutdown will call the orientdb close method.
            graph = new OrientGraph(graphFile, username, password);

        } else if (graphType.equals("tinkergraph")) {
            graph = new TinkerGraph();
            if (null != graphFile)
                GraphMLReader.inputGraph(graph, new FileInputStream(graphFile));
        } else {
            throw new Exception(graphType + " is not a supported graph type");
        }
        return graph;
    }
}
