package com.tinkerpop.rexster.config.hinted;

import com.tinkerpop.rexster.config.GraphConfiguration;

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public interface HintedGraphConfiguration extends GraphConfiguration {
    public HintedGraph getDistributedGraph();
}
