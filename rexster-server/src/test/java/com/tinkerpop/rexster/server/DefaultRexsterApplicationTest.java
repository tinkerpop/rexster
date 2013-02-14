package com.tinkerpop.rexster.server;

import com.tinkerpop.blueprints.impls.tg.TinkerGraph;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class DefaultRexsterApplicationTest {

    @Test
    public void getGraphDoesNotExistReturnsNull() {
        final TinkerGraph g = new TinkerGraph();
        final String graphName = "test";

        final RexsterApplication ra = new DefaultRexsterApplication(graphName, g);

        Assert.assertNull(ra.getGraph("not-real"));
    }

    @Test
    public void shouldConfiguredSingleGraph() {
        final TinkerGraph g = new TinkerGraph();
        final String graphName = "test";

        final RexsterApplication ra = new DefaultRexsterApplication(graphName, g);

        Assert.assertEquals(graphName, ra.getGraphNames().toArray()[0]);
        Assert.assertSame(g, ra.getGraph(graphName));
        Assert.assertSame(g, ra.getApplicationGraph(graphName).getGraph());
    }
}
