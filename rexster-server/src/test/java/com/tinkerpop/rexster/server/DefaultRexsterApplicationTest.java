package com.tinkerpop.rexster.server;

import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.impls.tg.TinkerGraph;
import com.tinkerpop.rexster.RexsterApplicationGraph;
import com.tinkerpop.rexster.Tokens;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.junit.Assert;
import org.junit.Test;

import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class DefaultRexsterApplicationTest {
    private static final Reader rexsterXmlReader = new StringReader(RexsterXmlData.XML);

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

    @Test
    public void shouldConfiguredFromXmlConfiguration() throws Exception {
        final XMLConfiguration properties = new XMLConfiguration();
        properties.load(rexsterXmlReader);
        final List<HierarchicalConfiguration> graphConfigs = properties.configurationsAt(Tokens.REXSTER_GRAPH_PATH);

        final RexsterApplication ra = new DefaultRexsterApplication(graphConfigs);

        Assert.assertEquals(2, ra.getGraphNames().size());
        final List<String> graphNames = new ArrayList<String>(ra.getGraphNames());
        Assert.assertTrue(graphNames.contains("emptygraph"));
        Assert.assertTrue(graphNames.contains("tinkergraph"));

        for (String graphName : ra.getGraphNames()) {
            final Graph g = ra.getGraph(graphName);
            Assert.assertNotNull(g);
            Assert.assertTrue(g instanceof TinkerGraph);

            final RexsterApplicationGraph rag = ra.getApplicationGraph(graphName);
            Assert.assertNotNull(rag);
            Assert.assertTrue(rag.getGraph() instanceof TinkerGraph);
            Assert.assertEquals(graphName, rag.getGraphName());
        }
    }
}
