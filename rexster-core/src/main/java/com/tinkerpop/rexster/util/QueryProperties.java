package com.tinkerpop.rexster.util;

import com.tinkerpop.blueprints.Query;

/**
 * Holder for query properties parsed by the RequestObjectHelper to be translated into a Vertex Query.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class QueryProperties {
    private final Query.Compare compare;
    private final String name;
    private final Comparable value;

    public QueryProperties(final String name, final Query.Compare compare, final Comparable value){
        this.compare = compare;
        this.name = name;
        this.value = value;
    }

    public Query.Compare getCompare() {
        return compare;
    }

    public String getName() {
        return name;
    }

    public Comparable getValue() {
        return value;
    }
}
