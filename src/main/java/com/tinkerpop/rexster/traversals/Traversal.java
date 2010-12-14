package com.tinkerpop.rexster.traversals;

import com.tinkerpop.rexster.RexsterResourceContext;
import org.codehaus.jettison.json.JSONObject;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public interface Traversal {

    public JSONObject evaluate(RexsterResourceContext ctx) throws TraversalException;

    public String getTraversalName();
}

