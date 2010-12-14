package com.tinkerpop.rexster.traversals.grateful;

import com.tinkerpop.rexster.RexsterResourceContext;
import com.tinkerpop.rexster.Tokens;
import com.tinkerpop.rexster.traversals.TraversalBaseTest;
import com.tinkerpop.rexster.traversals.TraversalException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

public class CoFollowsRankTest extends TraversalBaseTest {

    @Test
    public void evaluateApiIsJson() {
        CoFollowsRank coFollowsRank = new CoFollowsRank();
        RexsterResourceContext ctx = this.createStandardContext();

        try {
            JSONObject result = coFollowsRank.evaluate(ctx);
            Assert.assertNotNull(result);
            Assert.assertTrue(result.has(Tokens.API));
            Assert.assertTrue(result.optJSONObject(Tokens.API) instanceof JSONObject);
        } catch (TraversalException te) {
            te.printStackTrace();
            Assert.fail(te.getMessage());
        }
    }
}
