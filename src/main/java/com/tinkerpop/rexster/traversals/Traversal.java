package com.tinkerpop.rexster.traversals;

import org.codehaus.jettison.json.JSONObject;

import com.tinkerpop.rexster.RexsterResourceContext;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public interface Traversal {
   
    public JSONObject evaluate(RexsterResourceContext ctx) throws TraversalException;

    public String getTraversalName();
}

