package com.tinkerpop.rexster.config.distributed;

import com.tinkerpop.rexster.config.GraphConfiguration;

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public interface DistributedGraphConfiguration extends GraphConfiguration {
    public DistributedGraph getDistributedGraph();
}
