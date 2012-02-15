package com.tinkerpop.rexster;

import java.util.Set;

/**
 * The RexsterApplicationProvider makes it possible to abstract the manner in
 * which the RexsterApplication gets delivered to resources.
 */
public interface RexsterApplicationProvider {

    /**
     * Gets the current RexsterApplication instance.
     *
     * @return The RexsterApplication instance.
     */
    RexsterApplication getRexsterApplication();

    RexsterApplicationGraph getApplicationGraph(String graphName);

    Set<String> getGraphsNames();

    long getStartTime();
}
