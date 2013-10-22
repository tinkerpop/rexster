package com.tinkerpop.rexster.config.hinted;

import com.tinkerpop.blueprints.Element;

import java.io.Serializable;

/**
 * An ElementRange specifies the range of vertex or edge ids (as qualified by the elementType) that are hosted
 * locally. This makes the assumption that vertex and edge ids are comparable - in other words that they can be
 * ordered. This is true for all graph databases that I am aware of.
 *
 * @author Matthias Broecheler (me@matthiasb.com)
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class ElementRange<U extends Comparable, E extends Element> implements Serializable {

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

    public boolean contains(U item) {
        return startRange.compareTo(item) <= 0 && endRange.compareTo(item) == 1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ElementRange that = (ElementRange) o;

        if (priority != that.priority) return false;
        if (!elementType.equals(that.elementType)) return false;
        if (!endRange.equals(that.endRange)) return false;
        if (!startRange.equals(that.startRange)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = elementType.hashCode();
        result = 31 * result + startRange.hashCode();
        result = 31 * result + endRange.hashCode();
        result = 31 * result + priority;
        return result;
    }
}
