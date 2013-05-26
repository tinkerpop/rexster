package com.tinkerpop.rexster.server;

import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.rexster.RexsterApplicationGraph;
import com.codahale.metrics.MetricRegistry;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Base class for implementations that need to use a Map to hold graphs served by Rexster.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public abstract class AbstractMapRexsterApplication implements RexsterApplication {

    private static final Logger logger = Logger.getLogger(AbstractMapRexsterApplication.class);

    private static MetricRegistry metricRegistry;

    protected final long startTime = System.currentTimeMillis();

    protected final Map<String, RexsterApplicationGraph> graphs = new HashMap<String, RexsterApplicationGraph>();

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
    public MetricRegistry getMetricRegistry() {
        if (metricRegistry == null) {
            metricRegistry = new MetricRegistry();
        }

        return metricRegistry;
    }

    @Override
    public String toString() {
        return String.format("RexsterServerContext {configured graphs=%s}", graphs.size());
    }
}
