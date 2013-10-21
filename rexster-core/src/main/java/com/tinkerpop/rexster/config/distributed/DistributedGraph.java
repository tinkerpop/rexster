package com.tinkerpop.rexster.config.distributed;

import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;

import java.io.Serializable;
import java.util.List;

/**
 * @author Matthias Broecheler (me@matthiasb.com)
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public interface  DistributedGraph<U extends Comparable> extends Serializable {
    public List<ElementRange<U,Vertex>> getVertexRanges();
    public List<ElementRange<U,Edge>> getEdgeRanges();
}
