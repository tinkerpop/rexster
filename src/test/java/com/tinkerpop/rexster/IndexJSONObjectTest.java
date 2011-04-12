package com.tinkerpop.rexster;

import com.tinkerpop.blueprints.pgm.Index;
import com.tinkerpop.blueprints.pgm.Index.Type;
import com.tinkerpop.rexster.*;
import org.codehaus.jettison.json.JSONException;
import org.junit.Assert;
import org.junit.Test;

public class IndexJSONObjectTest {

    @Test
    public void constructorValid() {
        Index idx = new MockIndex("idx", Type.MANUAL, String.class, 0L);

        try {
            com.tinkerpop.rexster.IndexJSONObject indexJsonObject = new com.tinkerpop.rexster.IndexJSONObject(idx);
            Assert.assertEquals("idx", indexJsonObject.optString("name"));
            Assert.assertEquals(String.class.getCanonicalName(), indexJsonObject.optString("class"));
            Assert.assertEquals(Type.MANUAL.toString().toLowerCase(), indexJsonObject.optString("type"));

        } catch (JSONException ex) {
            Assert.fail(ex.getMessage());
        }
    }
}
