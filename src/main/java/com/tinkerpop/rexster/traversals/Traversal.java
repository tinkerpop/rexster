package com.tinkerpop.rexster.traversals;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.tinkerpop.rexster.RexsterResourceContext;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public interface Traversal {
   
    public JSONObject evaluate(RexsterResourceContext ctx) throws JSONException;

    public void traverse()throws JSONException;

    public void addApiToResultObject();

    public String getTraversalName();
}

