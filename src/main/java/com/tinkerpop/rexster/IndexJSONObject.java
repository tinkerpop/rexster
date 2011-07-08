package com.tinkerpop.rexster;

import com.tinkerpop.blueprints.pgm.Edge;
import com.tinkerpop.blueprints.pgm.Index;
import com.tinkerpop.blueprints.pgm.Vertex;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class IndexJSONObject extends JSONObject {

    public IndexJSONObject(Index index) throws JSONException {
        this.put("name", index.getIndexName());

        // as there is no intention that i know of to have more classes than vertex/edge and
        // since the POST of a new index takes "vertex" or "edge" as the class parameter
        // it makes sense for the class attribute to return as such (prior to this it was
        // returning the index.getIndexClass().getCanonicalName()
        if (Vertex.class.isAssignableFrom(index.getIndexClass())) {
            this.put("class", "vertex");
        } else if (Edge.class.isAssignableFrom(index.getIndexClass())) {
            this.put("class", "edge");
        }

        this.put("type", index.getIndexType().toString().toLowerCase());
    }
}
