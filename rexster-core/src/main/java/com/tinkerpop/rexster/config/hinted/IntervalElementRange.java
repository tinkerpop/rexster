package com.tinkerpop.rexster.config.hinted;

import java.io.Serializable;

import com.tinkerpop.blueprints.Element;

/**
 * An IntervalElementRange specifies the range of vertex or edge ids (as qualified by the elementType) that are hosted
 * locally. This makes the assumption that vertex and edge ids are comparable - in other words that they can be
 * ordered. This is true for all graph databases that I am aware of.
 *
 * @author Matthias Broecheler (me@matthiasb.com)
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class IntervalElementRange<U extends Comparable<U>, E extends Element>
    implements ElementRange<U, E>, Serializable {

    /**
     * Generated uid on Dec 10 2013
     */
    private static final long serialVersionUID = -71905414131570157L;

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
    
    public IntervalElementRange(final Class<E> elementType, final U startRange, final U endRange, final int priority) {
        this.elementType = elementType;
        this.startRange = startRange;
        this.endRange = endRange;
        this.priority = priority;
    }

    @Override
    public Class<E> getElementType() {
        return elementType;
    }

    public U getStartRange() {
        return startRange;
    }

    public U getEndRange() {
        return endRange;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public boolean contains(U item) {
        return startRange.compareTo(item) <= 0 && endRange.compareTo(item) > 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IntervalElementRange<?,?> that = (IntervalElementRange<?,?>) o;

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

    @Override
    public String toString() {
        return "ElementRange[type=" + elementType + ", start="
                + startRange + ", end=" + endRange + ", prio="
                + priority + "]";
    }
}
