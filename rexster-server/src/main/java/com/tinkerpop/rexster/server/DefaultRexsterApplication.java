package com.tinkerpop.rexster.server;

import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.rexster.RexsterApplicationGraph;
import org.apache.log4j.Logger;

/**
 * Configure a single existing graph into Rexster.  Useful in configuring Rexster for embedded applications.
 *
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class DefaultRexsterApplication extends AbstractMapRexsterApplication {

    private static final Logger logger = Logger.getLogger(DefaultRexsterApplication.class);

    /**
     * Constructs the DefaultRexsterApplication.
     *
     * @param graphName the name the graph will have in various Rexster contexts.
     * @param graph a graph instance.
     */
    public DefaultRexsterApplication(final String graphName, final Graph graph) {
        final RexsterApplicationGraph rag = new RexsterApplicationGraph(graphName, graph);
        this.graphs.put(graphName, rag);
        logger.info(String.format("Graph [%s] loaded", rag.getGraph()));
    }
}
