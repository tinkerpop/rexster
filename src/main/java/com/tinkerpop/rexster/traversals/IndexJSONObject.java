package com.tinkerpop.rexster.traversals;

import com.tinkerpop.blueprints.pgm.Index;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class IndexJSONObject extends JSONObject {

    public IndexJSONObject(Index index) throws JSONException {
        this.put("name", index.getIndexName());
        this.put("class", index.getIndexClass().getCanonicalName());
        this.put("type", index.getIndexType().toString().toLowerCase());
    }
}
