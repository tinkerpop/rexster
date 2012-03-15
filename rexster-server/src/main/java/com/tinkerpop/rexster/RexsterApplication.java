package com.tinkerpop.rexster;

import com.tinkerpop.blueprints.pgm.Graph;

import java.util.Set;

/**
 * RexsterApplication is the class that Rexster's servlets all delegate to in order to actually
 * retrieve or query graphs. A RexsterApplication owns all of the Graph instances that Rexster
 * serves.
 *
 * Users interested in embedding Rexster into their graph store may want provide a custom
 * implementation of RexsterApplication that has logic to expose a Blueprints graph that
 * delegates to the relevant internal graph representations of their graph store.
 */
public interface RexsterApplication {
    /**
     * Retrieve the graph contained by the application graph with the given name
     * @param graphName the name of the graph to retrieve
     * @return the Graph whose name is graphName, or null if it doesn't exist
     */
    Graph getGraph(String graphName);

    /**
     * Retrieve a specific application graph
     * @param graphName the name of the application graph to retrieve
     * @return the RexsterApplicationGraph whose name is graphName, or null if it doesn't exist
     */
    RexsterApplicationGraph getApplicationGraph(String graphName);

    /**
     * Retrieve the names of all graphs that we are serving
     * @return a set of the names of all graphs that we are serving
     */
    Set<String> getGraphNames();

    /**
     * Retrieve the time at which we started serving graphs
     * @return a long containing the time at which we starting serving graphs, in milliseconds
     */
    long getStartTime();

    /**
     * Stop serving graphs. Shuts down each of the graphs that we are serving.
     */
    void stop();
}
