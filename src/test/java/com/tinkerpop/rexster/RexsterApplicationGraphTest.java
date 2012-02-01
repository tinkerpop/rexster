package com.tinkerpop.rexster;

import com.tinkerpop.blueprints.pgm.Graph;
import com.tinkerpop.blueprints.pgm.impls.tg.TinkerGraphFactory;
import com.tinkerpop.blueprints.pgm.util.wrappers.event.EventGraph;
import com.tinkerpop.blueprints.pgm.util.wrappers.readonly.ReadOnlyGraph;
import com.tinkerpop.rexster.extension.ExtensionConfiguration;
import com.tinkerpop.rexster.extension.ExtensionPoint;
import com.tinkerpop.rexster.extension.ExtensionSegmentSet;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Assert;
import org.junit.Test;

import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriInfo;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RexsterApplicationGraphTest {
    private Mockery mockery = new JUnit4Mockery();

    @Test
    public void isTransactionalGraphFalse() {
        RexsterApplicationGraph rag = new RexsterApplicationGraph("graph", TinkerGraphFactory.createTinkerGraph());
        Assert.assertFalse(rag.isTransactionalGraph());
    }

    @Test
    public void loadAllowableExtensionsNullList() {
        RexsterApplicationGraph rag = new RexsterApplicationGraph("graph", null);
        rag.loadAllowableExtensions(null);

        Assert.assertNotNull(rag.getExtensionAllowables());
        Assert.assertEquals(0, rag.getExtensionAllowables().size());
    }

    @Test
    public void loadAllowableExtensionsValid() {
        RexsterApplicationGraph rag = new RexsterApplicationGraph("graph", null);

        List list = new ArrayList();
        list.add("ns1:*");
        list.add("ns2:go");
        list.add("");
        list.add("somejunkthat won't parse to a namespace");
        rag.loadAllowableExtensions(list);

        Assert.assertNotNull(rag.getExtensionAllowables());
        Assert.assertEquals(2, rag.getExtensionAllowables().size());
    }

    @Test
    public void isExtensionAllowedPass() {
        RexsterApplicationGraph rag = new RexsterApplicationGraph("graph", null);

        List list = new ArrayList();
        list.add("ns:*");
        list.add("ns2:go");
        list.add("");
        list.add("somejunkthat won't parse to a namespace");
        rag.loadAllowableExtensions(list);

        final UriInfo uri = this.mockery.mock(UriInfo.class);
        final List<PathSegment> pathSegments = new ArrayList<PathSegment>();
        final PathSegment graphsPathSegment = this.mockery.mock(PathSegment.class, "graphsSegment");
        final PathSegment graphPathSegment = this.mockery.mock(PathSegment.class, "graphPathSegment");
        final PathSegment namespacePathSegment = this.mockery.mock(PathSegment.class, "namespacePathSegment");
        final PathSegment extensionPathSegment = this.mockery.mock(PathSegment.class, "extensionPathSegment");

        pathSegments.add(graphsPathSegment);
        pathSegments.add(graphPathSegment);
        pathSegments.add(namespacePathSegment);
        pathSegments.add(extensionPathSegment);

        this.mockery.checking(new Expectations() {{
            allowing(namespacePathSegment).getPath();
            will(returnValue("ns"));
            allowing(extensionPathSegment).getPath();
            will(returnValue("ext"));
            allowing(uri).getPathSegments();
            will(returnValue(pathSegments));
        }});

        Assert.assertTrue(rag.isExtensionAllowed(new ExtensionSegmentSet(uri, ExtensionPoint.GRAPH)));
    }

    @Test
    public void isExtensionAllowedFail() {
        RexsterApplicationGraph rag = new RexsterApplicationGraph("graph", null);

        List list = new ArrayList();
        list.add("ns1:*");
        list.add("ns2:go");
        list.add("");
        list.add("somejunkthat won't parse to a namespace");
        rag.loadAllowableExtensions(list);

        final UriInfo uri = this.mockery.mock(UriInfo.class);
        final List<PathSegment> pathSegments = new ArrayList<PathSegment>();
        final PathSegment graphPathSegment = this.mockery.mock(PathSegment.class, "graphPathSegment");
        final PathSegment namespacePathSegment = this.mockery.mock(PathSegment.class, "namespacePathSegment");
        final PathSegment extensionPathSegment = this.mockery.mock(PathSegment.class, "extensionPathSegment");

        pathSegments.add(graphPathSegment);
        pathSegments.add(namespacePathSegment);
        pathSegments.add(extensionPathSegment);

        this.mockery.checking(new Expectations() {{
            allowing(namespacePathSegment).getPath();
            will(returnValue("ns"));
            allowing(extensionPathSegment).getPath();
            will(returnValue("ext"));
            allowing(uri).getPathSegments();
            will(returnValue(pathSegments));
        }});

        Assert.assertFalse(rag.isExtensionAllowed(new ExtensionSegmentSet(uri, ExtensionPoint.GRAPH)));
    }

    @Test
    public void loadExtensionsConfigurations() {

        String xmlString = "<extension><namespace>tp</namespace><name>extensionname</name><configuration><test>1</test></configuration></extension>";

        XMLConfiguration xmlConfig = new XMLConfiguration();

        try {
            xmlConfig.load(new StringReader(xmlString));
        } catch (ConfigurationException ex) {
            Assert.fail(ex.getMessage());
        }

        List<HierarchicalConfiguration> list = new ArrayList<HierarchicalConfiguration>();
        list.add(xmlConfig);

        RexsterApplicationGraph rag = new RexsterApplicationGraph("graph", null);

        List allowables = new ArrayList();
        allowables.add("tp:*");
        rag.loadAllowableExtensions(allowables);

        rag.loadExtensionsConfigurations(list);

        ExtensionConfiguration extConfig = rag.findExtensionConfiguration("tp", "extensionname");
        Assert.assertNotNull(extConfig);
        Assert.assertEquals("extensionname", extConfig.getExtensionName());
        Assert.assertEquals("tp", extConfig.getNamespace());

        Assert.assertNotNull(extConfig.getConfiguration());

        Map map = extConfig.tryGetMapFromConfiguration();
        Assert.assertNotNull(map);
        Assert.assertTrue(map.containsKey("test"));
        Assert.assertEquals("1", map.get("test"));
    }

    @Test
    public void unwrapGraphNoWrapping() {
        Graph g = TinkerGraphFactory.createTinkerGraph();
        Graph unwrapped = RexsterApplicationGraph.unwrapGraph(g);
        Assert.assertEquals(g, unwrapped);
    }

    @Test
    public void unwrapGraphReadonlyWrapping() {
        Graph tg = TinkerGraphFactory.createTinkerGraph();
        Graph g = new ReadOnlyGraph(tg);
        Graph unwrapped = RexsterApplicationGraph.unwrapGraph(g);
        Assert.assertEquals(tg, unwrapped);
    }

    @Test
    public void unwrapGraphEventWrapping() {
        Graph tg = TinkerGraphFactory.createTinkerGraph();
        Graph g = new EventGraph(tg);
        Graph unwrapped = RexsterApplicationGraph.unwrapGraph(g);
        Assert.assertEquals(tg, unwrapped);
    }

    @Test
    public void unwrapGraphReadonlyEventWrapping() {
        Graph tg = TinkerGraphFactory.createTinkerGraph();
        Graph eg = new EventGraph(tg);
        Graph g = new ReadOnlyGraph(eg);
        Graph unwrapped = RexsterApplicationGraph.unwrapGraph(g);
        Assert.assertEquals(tg, unwrapped);
    }
}
