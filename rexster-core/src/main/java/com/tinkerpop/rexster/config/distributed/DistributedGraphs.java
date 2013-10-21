package com.tinkerpop.rexster.config.distributed;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class DistributedGraphs implements Serializable {
    public final Map<String,DistributedGraph> graphs = new HashMap<String, DistributedGraph>();
}
