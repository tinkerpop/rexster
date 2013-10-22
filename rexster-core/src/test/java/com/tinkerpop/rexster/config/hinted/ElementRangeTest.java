package com.tinkerpop.rexster.config.hinted;

import com.tinkerpop.blueprints.Vertex;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class ElementRangeTest {
    @Test
    public void shouldContainValueInclusive() {
        final ElementRange<Long, Vertex> range = new ElementRange<Long, Vertex>(Vertex.class, 100l, 200l, 1);
        assertTrue(range.contains(100l));
    }

    @Test
    public void shouldContainValueMiddle() {
        final ElementRange<Long, Vertex> range = new ElementRange<Long, Vertex>(Vertex.class, 100l, 200l, 1);
        assertTrue(range.contains(101l));
    }

    @Test
    public void shouldNotContainValueExclusive() {
        final ElementRange<Long, Vertex> range = new ElementRange<Long, Vertex>(Vertex.class, 100l, 200l, 1);
        assertFalse(range.contains(200l));
    }

    @Test
    public void shouldNotContainValueOutOfBoundsOver() {
        final ElementRange<Long, Vertex> range = new ElementRange<Long, Vertex>(Vertex.class, 100l, 200l, 1);
        assertFalse(range.contains(201l));
    }

    @Test
    public void shouldNotContainValueOutOfBoundsUnder() {
        final ElementRange<Long, Vertex> range = new ElementRange<Long, Vertex>(Vertex.class, 100l, 200l, 1);
        assertFalse(range.contains(99l));
    }
}
