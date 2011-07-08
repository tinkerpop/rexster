package com.tinkerpop.rexster;

import com.tinkerpop.blueprints.pgm.Edge;
import com.tinkerpop.blueprints.pgm.Index;
import com.tinkerpop.blueprints.pgm.Index.Type;
import com.tinkerpop.blueprints.pgm.Vertex;
import org.codehaus.jettison.json.JSONException;
import org.junit.Assert;
import org.junit.Test;

public class IndexJSONObjectTest {

    @Test
    public void constructorVertexIndexValid() {
        Index idx = new MockIndex("idx", Type.MANUAL, Vertex.class, 0L);

        try {
            com.tinkerpop.rexster.IndexJSONObject indexJsonObject = new com.tinkerpop.rexster.IndexJSONObject(idx);
            Assert.assertEquals("idx", indexJsonObject.optString("name"));
            Assert.assertEquals("vertex", indexJsonObject.optString("class"));
            Assert.assertEquals(Type.MANUAL.toString().toLowerCase(), indexJsonObject.optString("type"));

        } catch (JSONException ex) {
            Assert.fail(ex.getMessage());
        }
    }

    @Test
    public void constructorEdgeIndexValid() {
        Index idx = new MockIndex("idx", Type.MANUAL, Edge.class, 0L);

        try {
            com.tinkerpop.rexster.IndexJSONObject indexJsonObject = new com.tinkerpop.rexster.IndexJSONObject(idx);
            Assert.assertEquals("idx", indexJsonObject.optString("name"));
            Assert.assertEquals("edge", indexJsonObject.optString("class"));
            Assert.assertEquals(Type.MANUAL.toString().toLowerCase(), indexJsonObject.optString("type"));

        } catch (JSONException ex) {
            Assert.fail(ex.getMessage());
        }
    }
}
