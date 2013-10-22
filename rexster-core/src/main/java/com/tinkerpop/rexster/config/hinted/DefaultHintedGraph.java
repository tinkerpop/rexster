package com.tinkerpop.rexster.config.hinted;

import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;

import java.util.Arrays;
import java.util.List;

/**
 * A basic implementation for a single standalone Rexster server which basically returns the full range of ids
 * for a given type where the Graph must support Long as the identifier.
 *
 * @author Matthias Broecheler (me@matthiasb.com)
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class DefaultHintedGraph implements HintedGraph<Long> {
    private static final ElementRange<Long, Vertex> LONG_VERTEX_ELEMENT_RANGE
            = new ElementRange<Long, Vertex>(Vertex.class, Long.MIN_VALUE, Long.MAX_VALUE, 1);
    private static final ElementRange<Long, Edge> LONG_EDGE_ELEMENT_RANGE
            = new ElementRange<Long, Edge>(Edge.class, Long.MIN_VALUE, Long.MAX_VALUE, 1);

    @Override
    public List<ElementRange<Long, Vertex>> getVertexRanges() {
        return Arrays.asList(LONG_VERTEX_ELEMENT_RANGE);
    }

    @Override
    public List<ElementRange<Long, Edge>> getEdgeRanges() {
        return Arrays.asList(LONG_EDGE_ELEMENT_RANGE);
    }
}
