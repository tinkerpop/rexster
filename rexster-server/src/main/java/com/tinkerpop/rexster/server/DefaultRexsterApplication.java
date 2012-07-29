package com.tinkerpop.rexster.server;

import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.rexster.RexsterApplicationGraph;
import com.tinkerpop.rexster.Tokens;
import com.tinkerpop.rexster.config.GraphConfigurationContainer;
import com.tinkerpop.rexster.config.GraphConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Initializes and manages Graph instances.  Supplies these instances in the various
 * contexts that Rexster requires them.
 *
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class DefaultRexsterApplication implements RexsterApplication {

    protected static final Logger logger = Logger.getLogger(DefaultRexsterApplication.class);

    private final long startTime = System.currentTimeMillis();

    private final Map<String, RexsterApplicationGraph> graphs = new HashMap<String, RexsterApplicationGraph>();

    public DefaultRexsterApplication(final String graphName, final Graph graph) {
        final RexsterApplicationGraph rag = new RexsterApplicationGraph(graphName, graph);
        this.graphs.put(graphName, rag);
        logger.info(String.format("Graph [%s] loaded", rag.getGraph()));
    }

    public DefaultRexsterApplication(final XMLConfiguration properties) {
        // get the graph configurations from the XML config file
        final List<HierarchicalConfiguration> graphConfigs = properties.configurationsAt(Tokens.REXSTER_GRAPH_PATH);

        try {
            final GraphConfigurationContainer container = new GraphConfigurationContainer(graphConfigs);
            this.graphs.putAll(container.getApplicationGraphs());
        } catch (GraphConfigurationException gce) {
            logger.error("Graph initialization failed. Check the graph configuration in rexster.xml.");
        }

    }

    @Override
    public Graph getGraph(final String graphName) {
        final RexsterApplicationGraph g = getApplicationGraph(graphName);
        if (g != null) {
            return g.getGraph();
        } else {
            return null;
        }
    }

    @Override
    public RexsterApplicationGraph getApplicationGraph(final String graphName) {
        return this.graphs.get(graphName);
    }

    @Override
    public Set<String> getGraphNames() {
        return this.graphs.keySet();
    }

    @Override
    public long getStartTime() {
        return this.startTime;
    }

    @Override
    public void stop() {

        // need to shutdown all the graphs that were started with the web server
        for (RexsterApplicationGraph rag : this.graphs.values()) {

            final Graph graph = rag.getGraph();
            logger.info(String.format("Shutting down [%s] - [%s]", rag.getGraphName(), graph));

            // graph may not have been initialized properly if an exception gets tossed in
            // on graph creation
            if (graph != null) {
                final Graph shutdownGraph = rag.getUnwrappedGraph();
                shutdownGraph.shutdown();
            }
        }

    }

    @Override
    public String toString() {
        return String.format("RexsterServerContext {configured graphs=%s}", graphs.size());
    }
}
