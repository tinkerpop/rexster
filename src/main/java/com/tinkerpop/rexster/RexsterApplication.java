package com.tinkerpop.rexster;

import com.tinkerpop.blueprints.pgm.Graph;
import com.tinkerpop.blueprints.pgm.impls.readonly.ReadOnlyGraph;
import com.tinkerpop.rexster.config.GraphConfigurationContainer;
import com.tinkerpop.rexster.config.GraphConfigurationException;
import com.tinkerpop.rexster.extension.ExtensionConfiguration;
import com.tinkerpop.rexster.extension.RexsterExtension;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

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
        this(graphName, graph, new MapResultObjectCache());
    }

    public RexsterApplication(final String graphName, final Graph graph, final ResultObjectCache cache) {
        RexsterApplicationGraph rag = new RexsterApplicationGraph(graphName, graph);
        this.graphs.put(graphName, rag);
        logger.info("Graph " + rag.getGraph() + " loaded");
        this.resultObjectCache = cache;
    }

    public RexsterApplication(final XMLConfiguration properties) {

        // get the graph configurations from the XML config file
        List<HierarchicalConfiguration> graphConfigs = properties.configurationsAt(Tokens.REXSTER_GRAPH_PATH);
        int cacheMaxSize = properties.getInt(Tokens.REXSTER_CACHE_MAXSIZE_PATH, MapResultObjectCache.maxSize);

        try {
            GraphConfigurationContainer container = new GraphConfigurationContainer(graphConfigs);
            this.graphs = container.getApplicationGraphs();
        } catch (GraphConfigurationException gce) {
            logger.error("Graph initialization failed. Check the graph configuration in rexster.xml.");
        }

        Properties cacheProperties = new Properties();
        cacheProperties.put(Tokens.REXSTER_CACHE_MAXSIZE_PATH, cacheMaxSize);
        this.resultObjectCache = new MapResultObjectCache(cacheProperties);

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

    public Set<String> getGraphsNames() {
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
            logger.info("Shutting down " + rag.getGraphName() + " - " + graph);

            // graph may not have been initialized properly if an exception gets tossed in
            // on graph creation
            if (graph != null) {
            	Graph shutdownGraph = graph;
            	
            	if (graph instanceof ReadOnlyGraph) {
            		// can't call shutdown on a readonly graph.
            		shutdownGraph = ((ReadOnlyGraph) graph).getRawGraph();
            	}
            	
                shutdownGraph.shutdown();
            }
        }

    }
}
