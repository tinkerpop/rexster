package com.tinkerpop.rexster.traversals.gremlin;

import com.tinkerpop.rexster.RexsterResourceContext;
import com.tinkerpop.rexster.Tokens;
import com.tinkerpop.rexster.traversals.TraversalBaseTest;
import com.tinkerpop.rexster.traversals.TraversalException;
import com.tinkerpop.rexster.traversals.grateful.FollowsScore;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class GremlinTraversalTest extends TraversalBaseTest {

    @Test
    public void evaluateApiIsJson() {
        GremlinTraversal gremlin = new GremlinTraversal();
        RexsterResourceContext ctx = this.createStandardContext();

        try {
            JSONObject result = gremlin.evaluate(ctx);
            Assert.assertNotNull(result);
            Assert.assertTrue(result.has(Tokens.API));
            Assert.assertTrue(result.optJSONObject(Tokens.API) instanceof JSONObject);
        } catch (TraversalException te) {
            te.printStackTrace();
            Assert.fail(te.getMessage());
        }
    }
}
