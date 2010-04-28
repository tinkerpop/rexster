package com.tinkerpop.rexster.traversals;

import org.restlet.representation.Representation;

/**
 * author: Marko A. Rodriguez (http://markorodriguez.com)
 */
public interface Traversal {
    public Representation evaluate(final String jsonString);

    public Representation evaluate();

    public void traverse();

    public void addApiToResultObject();

    public String getResourceName();
}

