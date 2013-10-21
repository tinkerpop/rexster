package com.tinkerpop.rexster.config.distributed;

import com.tinkerpop.blueprints.Element;

/**
 * An ElementRange specifies the range of vertex or edge ids (as qualified by the elementType) that are hosted
 * locally. This makes the assumption that vertex and edge ids are comparable - in other words that they can be
 * ordered. This is true for all graph databases that I am aware of.
 *
 * @author Matthias Broecheler (me@matthiasb.com)
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class ElementRange<U extends Comparable, E extends Element> {

    /**
     * Either Vertex.class or Edge.class
     */
    private final Class<E> elementType;

    /**
     * Inclusive.
     */
    private final U startRange;

    /**
     * Exclusive
     */
    private final U endRange;

    /**
     * The priority specifies the priority this local machine has in answering queries for vertices/edges that fall
     * in this range.
     */
    private final int priority;

    public ElementRange(final Class<E> elementType, final U startRange, final U endRange, final int priority) {
        this.elementType = elementType;
        this.startRange = startRange;
        this.endRange = endRange;
        this.priority = priority;
    }

    public Class<E> getElementType() {
        return elementType;
    }

    public U getStartRange() {
        return startRange;
    }

    public U getEndRange() {
        return endRange;
    }

    public int getPriority() {
        return priority;
    }
}
