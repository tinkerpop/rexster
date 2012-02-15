package com.tinkerpop.rexster.extension;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Assert;
import org.junit.Test;

import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.List;

public class ExtensionSegmentSetTest {
    private Mockery mockery = new JUnit4Mockery();

    @Test
    public void isValidFormatGraphExtensionNoNamespace() {

        final UriInfo uri = this.mockery.mock(UriInfo.class);
        final List<PathSegment> pathSegments = new ArrayList<PathSegment>();
        final PathSegment graphPathSegment = this.mockery.mock(PathSegment.class, "graphPathSegment");

        pathSegments.add(graphPathSegment);

        this.mockery.checking(new Expectations() {{
            allowing(uri).getPathSegments();
            will(returnValue(pathSegments));
        }});


        ExtensionSegmentSet extensionSegmentSet = new ExtensionSegmentSet(uri, ExtensionPoint.GRAPH);
        Assert.assertFalse(extensionSegmentSet.isValidFormat());
        Assert.assertEquals("", extensionSegmentSet.getNamespace());
        Assert.assertEquals("", extensionSegmentSet.getExtension());
        Assert.assertEquals("", extensionSegmentSet.getExtensionMethod());
        Assert.assertEquals("", extensionSegmentSet.getNamespaceAndExtension());
        Assert.assertEquals("[parse error]:[parse error]+*", extensionSegmentSet.toString());
    }

    @Test
    public void isValidFormatGraphExtensionNoExtension() {

        final UriInfo uri = this.mockery.mock(UriInfo.class);
        final List<PathSegment> pathSegments = new ArrayList<PathSegment>();
        final PathSegment graphsPathSegment = this.mockery.mock(PathSegment.class, "graphsSegment");
        final PathSegment graphPathSegment = this.mockery.mock(PathSegment.class, "graphPathSegment");
        final PathSegment namespacePathSegment = this.mockery.mock(PathSegment.class, "namespacePathSegment");

        pathSegments.add(graphsPathSegment);
        pathSegments.add(graphPathSegment);
        pathSegments.add(namespacePathSegment);

        this.mockery.checking(new Expectations() {{
            allowing(namespacePathSegment).getPath();
            will(returnValue("ns"));
            allowing(uri).getPathSegments();
            will(returnValue(pathSegments));
        }});


        ExtensionSegmentSet extensionSegmentSet = new ExtensionSegmentSet(uri, ExtensionPoint.GRAPH);
        Assert.assertFalse(extensionSegmentSet.isValidFormat());
        Assert.assertEquals("ns", extensionSegmentSet.getNamespace());
        Assert.assertEquals("", extensionSegmentSet.getExtension());
        Assert.assertEquals("", extensionSegmentSet.getExtensionMethod());
        Assert.assertEquals("", extensionSegmentSet.getNamespaceAndExtension());
        Assert.assertEquals("ns:[parse error]+*", extensionSegmentSet.toString());
    }

    @Test
    public void isValidFormatGraphExtensionValidNoMethod() {

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

        ExtensionSegmentSet extensionSegmentSet = new ExtensionSegmentSet(uri, ExtensionPoint.GRAPH);
        Assert.assertTrue(extensionSegmentSet.isValidFormat());
        Assert.assertEquals("ns", extensionSegmentSet.getNamespace());
        Assert.assertEquals("ext", extensionSegmentSet.getExtension());
        Assert.assertEquals("", extensionSegmentSet.getExtensionMethod());
        Assert.assertEquals("ns:ext", extensionSegmentSet.getNamespaceAndExtension());
        Assert.assertEquals("ns:ext+*", extensionSegmentSet.toString());
    }

    @Test
    public void isValidFormatGraphExtensionValidWithMethod() {

        final UriInfo uri = this.mockery.mock(UriInfo.class);
        final List<PathSegment> pathSegments = new ArrayList<PathSegment>();
        final PathSegment graphsPathSegment = this.mockery.mock(PathSegment.class, "graphsSegment");
        final PathSegment graphPathSegment = this.mockery.mock(PathSegment.class, "graphPathSegment");
        final PathSegment namespacePathSegment = this.mockery.mock(PathSegment.class, "namespacePathSegment");
        final PathSegment extensionPathSegment = this.mockery.mock(PathSegment.class, "extensionPathSegment");
        final PathSegment methodPathSegment = this.mockery.mock(PathSegment.class, "methodPathSegment");

        pathSegments.add(graphsPathSegment);
        pathSegments.add(graphPathSegment);
        pathSegments.add(namespacePathSegment);
        pathSegments.add(extensionPathSegment);
        pathSegments.add(methodPathSegment);

        this.mockery.checking(new Expectations() {{
            allowing(namespacePathSegment).getPath();
            will(returnValue("ns"));
            allowing(extensionPathSegment).getPath();
            will(returnValue("ext"));
            allowing(methodPathSegment).getPath();
            will(returnValue("meth"));
            allowing(uri).getPathSegments();
            will(returnValue(pathSegments));
        }});

        ExtensionSegmentSet extensionSegmentSet = new ExtensionSegmentSet(uri, ExtensionPoint.GRAPH);
        Assert.assertTrue(extensionSegmentSet.isValidFormat());
        Assert.assertEquals("ns", extensionSegmentSet.getNamespace());
        Assert.assertEquals("ext", extensionSegmentSet.getExtension());
        Assert.assertEquals("meth", extensionSegmentSet.getExtensionMethod());
        Assert.assertEquals("ns:ext", extensionSegmentSet.getNamespaceAndExtension());
        Assert.assertEquals("ns:ext+meth", extensionSegmentSet.toString());
    }

    @Test
    public void isValidFormatVertexExtensionNoNamespace() {

        final UriInfo uri = this.mockery.mock(UriInfo.class);
        final List<PathSegment> pathSegments = new ArrayList<PathSegment>();
        final PathSegment graphsPathSegment = this.mockery.mock(PathSegment.class, "graphsSegment");
        final PathSegment graphPathSegment = this.mockery.mock(PathSegment.class, "graphPathSegment");
        final PathSegment elementPathSegment = this.mockery.mock(PathSegment.class, "elementPathSegment");
        final PathSegment idPathSegment = this.mockery.mock(PathSegment.class, "idPathSegment");

        pathSegments.add(graphsPathSegment);
        pathSegments.add(graphPathSegment);
        pathSegments.add(elementPathSegment);
        pathSegments.add(idPathSegment);

        this.mockery.checking(new Expectations() {{
            allowing(uri).getPathSegments();
            will(returnValue(pathSegments));
        }});


        ExtensionSegmentSet extensionSegmentSet = new ExtensionSegmentSet(uri, ExtensionPoint.VERTEX);
        Assert.assertFalse(extensionSegmentSet.isValidFormat());
        Assert.assertEquals("", extensionSegmentSet.getNamespace());
        Assert.assertEquals("", extensionSegmentSet.getExtension());
        Assert.assertEquals("", extensionSegmentSet.getExtensionMethod());
        Assert.assertEquals("", extensionSegmentSet.getNamespaceAndExtension());
        Assert.assertEquals("[parse error]:[parse error]+*", extensionSegmentSet.toString());
    }

    @Test
    public void isValidFormatVertexExtensionNoExtension() {

        final UriInfo uri = this.mockery.mock(UriInfo.class);
        final List<PathSegment> pathSegments = new ArrayList<PathSegment>();
        final PathSegment graphsPathSegment = this.mockery.mock(PathSegment.class, "graphsSegment");
        final PathSegment graphPathSegment = this.mockery.mock(PathSegment.class, "graphPathSegment");
        final PathSegment elementPathSegment = this.mockery.mock(PathSegment.class, "elementPathSegment");
        final PathSegment idPathSegment = this.mockery.mock(PathSegment.class, "idPathSegment");
        final PathSegment namespacePathSegment = this.mockery.mock(PathSegment.class, "namespacePathSegment");

        pathSegments.add(graphsPathSegment);
        pathSegments.add(graphPathSegment);
        pathSegments.add(elementPathSegment);
        pathSegments.add(idPathSegment);
        pathSegments.add(namespacePathSegment);

        this.mockery.checking(new Expectations() {{
            allowing(namespacePathSegment).getPath();
            will(returnValue("ns"));
            allowing(uri).getPathSegments();
            will(returnValue(pathSegments));
        }});


        ExtensionSegmentSet extensionSegmentSet = new ExtensionSegmentSet(uri, ExtensionPoint.VERTEX);
        Assert.assertFalse(extensionSegmentSet.isValidFormat());
        Assert.assertEquals("ns", extensionSegmentSet.getNamespace());
        Assert.assertEquals("", extensionSegmentSet.getExtension());
        Assert.assertEquals("", extensionSegmentSet.getExtensionMethod());
        Assert.assertEquals("", extensionSegmentSet.getNamespaceAndExtension());
        Assert.assertEquals("ns:[parse error]+*", extensionSegmentSet.toString());
    }

    @Test
    public void isValidFormatVertexExtensionValidNoMethod() {

        final UriInfo uri = this.mockery.mock(UriInfo.class);
        final List<PathSegment> pathSegments = new ArrayList<PathSegment>();
        final PathSegment graphsPathSegment = this.mockery.mock(PathSegment.class, "graphsSegment");
        final PathSegment graphPathSegment = this.mockery.mock(PathSegment.class, "graphPathSegment");
        final PathSegment elementPathSegment = this.mockery.mock(PathSegment.class, "elementPathSegment");
        final PathSegment idPathSegment = this.mockery.mock(PathSegment.class, "idPathSegment");
        final PathSegment namespacePathSegment = this.mockery.mock(PathSegment.class, "namespacePathSegment");
        final PathSegment extensionPathSegment = this.mockery.mock(PathSegment.class, "extensionPathSegment");

        pathSegments.add(graphsPathSegment);
        pathSegments.add(graphPathSegment);
        pathSegments.add(elementPathSegment);
        pathSegments.add(idPathSegment);
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

        ExtensionSegmentSet extensionSegmentSet = new ExtensionSegmentSet(uri, ExtensionPoint.VERTEX);
        Assert.assertTrue(extensionSegmentSet.isValidFormat());
        Assert.assertEquals("ns", extensionSegmentSet.getNamespace());
        Assert.assertEquals("ext", extensionSegmentSet.getExtension());
        Assert.assertEquals("", extensionSegmentSet.getExtensionMethod());
        Assert.assertEquals("ns:ext", extensionSegmentSet.getNamespaceAndExtension());
        Assert.assertEquals("ns:ext+*", extensionSegmentSet.toString());
    }

    @Test
    public void isValidFormatVertexExtensionValidWithMethod() {

        final UriInfo uri = this.mockery.mock(UriInfo.class);
        final List<PathSegment> pathSegments = new ArrayList<PathSegment>();
        final PathSegment graphsPathSegment = this.mockery.mock(PathSegment.class, "graphsSegment");
        final PathSegment graphPathSegment = this.mockery.mock(PathSegment.class, "graphPathSegment");
        final PathSegment elementPathSegment = this.mockery.mock(PathSegment.class, "elementPathSegment");
        final PathSegment idPathSegment = this.mockery.mock(PathSegment.class, "idPathSegment");
        final PathSegment namespacePathSegment = this.mockery.mock(PathSegment.class, "namespacePathSegment");
        final PathSegment extensionPathSegment = this.mockery.mock(PathSegment.class, "extensionPathSegment");
        final PathSegment methodPathSegment = this.mockery.mock(PathSegment.class, "methodPathSegment");

        pathSegments.add(graphsPathSegment);
        pathSegments.add(graphPathSegment);
        pathSegments.add(elementPathSegment);
        pathSegments.add(idPathSegment);
        pathSegments.add(namespacePathSegment);
        pathSegments.add(extensionPathSegment);
        pathSegments.add(methodPathSegment);

        this.mockery.checking(new Expectations() {{
            allowing(namespacePathSegment).getPath();
            will(returnValue("ns"));
            allowing(extensionPathSegment).getPath();
            will(returnValue("ext"));
            allowing(methodPathSegment).getPath();
            will(returnValue("meth"));
            allowing(uri).getPathSegments();
            will(returnValue(pathSegments));
        }});

        ExtensionSegmentSet extensionSegmentSet = new ExtensionSegmentSet(uri, ExtensionPoint.VERTEX);
        Assert.assertTrue(extensionSegmentSet.isValidFormat());
        Assert.assertEquals("ns", extensionSegmentSet.getNamespace());
        Assert.assertEquals("ext", extensionSegmentSet.getExtension());
        Assert.assertEquals("meth", extensionSegmentSet.getExtensionMethod());
        Assert.assertEquals("ns:ext", extensionSegmentSet.getNamespaceAndExtension());
        Assert.assertEquals("ns:ext+meth", extensionSegmentSet.toString());
    }

    @Test
    public void isValidFormatEdgeExtensionNoNamespace() {

        final UriInfo uri = this.mockery.mock(UriInfo.class);
        final List<PathSegment> pathSegments = new ArrayList<PathSegment>();
        final PathSegment graphsPathSegment = this.mockery.mock(PathSegment.class, "graphsSegment");
        final PathSegment graphPathSegment = this.mockery.mock(PathSegment.class, "graphPathSegment");
        final PathSegment elementPathSegment = this.mockery.mock(PathSegment.class, "elementPathSegment");
        final PathSegment idPathSegment = this.mockery.mock(PathSegment.class, "idPathSegment");

        pathSegments.add(graphsPathSegment);
        pathSegments.add(graphPathSegment);
        pathSegments.add(elementPathSegment);
        pathSegments.add(idPathSegment);

        this.mockery.checking(new Expectations() {{
            allowing(uri).getPathSegments();
            will(returnValue(pathSegments));
        }});


        ExtensionSegmentSet extensionSegmentSet = new ExtensionSegmentSet(uri, ExtensionPoint.EDGE);
        Assert.assertFalse(extensionSegmentSet.isValidFormat());
        Assert.assertEquals("", extensionSegmentSet.getNamespace());
        Assert.assertEquals("", extensionSegmentSet.getExtension());
        Assert.assertEquals("", extensionSegmentSet.getExtensionMethod());
        Assert.assertEquals("", extensionSegmentSet.getNamespaceAndExtension());
        Assert.assertEquals("[parse error]:[parse error]+*", extensionSegmentSet.toString());
    }

    @Test
    public void isValidFormatEdgeExtensionNoExtension() {

        final UriInfo uri = this.mockery.mock(UriInfo.class);
        final List<PathSegment> pathSegments = new ArrayList<PathSegment>();
        final PathSegment graphsPathSegment = this.mockery.mock(PathSegment.class, "graphsSegment");
        final PathSegment graphPathSegment = this.mockery.mock(PathSegment.class, "graphPathSegment");
        final PathSegment elementPathSegment = this.mockery.mock(PathSegment.class, "elementPathSegment");
        final PathSegment idPathSegment = this.mockery.mock(PathSegment.class, "idPathSegment");
        final PathSegment namespacePathSegment = this.mockery.mock(PathSegment.class, "namespacePathSegment");

        pathSegments.add(graphsPathSegment);
        pathSegments.add(graphPathSegment);
        pathSegments.add(elementPathSegment);
        pathSegments.add(idPathSegment);
        pathSegments.add(namespacePathSegment);

        this.mockery.checking(new Expectations() {{
            allowing(namespacePathSegment).getPath();
            will(returnValue("ns"));
            allowing(uri).getPathSegments();
            will(returnValue(pathSegments));
        }});


        ExtensionSegmentSet extensionSegmentSet = new ExtensionSegmentSet(uri, ExtensionPoint.EDGE);
        Assert.assertFalse(extensionSegmentSet.isValidFormat());
        Assert.assertEquals("ns", extensionSegmentSet.getNamespace());
        Assert.assertEquals("", extensionSegmentSet.getExtension());
        Assert.assertEquals("", extensionSegmentSet.getExtensionMethod());
        Assert.assertEquals("", extensionSegmentSet.getNamespaceAndExtension());
        Assert.assertEquals("ns:[parse error]+*", extensionSegmentSet.toString());
    }

    @Test
    public void isValidFormatEdgeExtensionValidNoMethod() {

        final UriInfo uri = this.mockery.mock(UriInfo.class);
        final List<PathSegment> pathSegments = new ArrayList<PathSegment>();
        final PathSegment graphsPathSegment = this.mockery.mock(PathSegment.class, "graphsSegment");
        final PathSegment graphPathSegment = this.mockery.mock(PathSegment.class, "graphPathSegment");
        final PathSegment elementPathSegment = this.mockery.mock(PathSegment.class, "elementPathSegment");
        final PathSegment idPathSegment = this.mockery.mock(PathSegment.class, "idPathSegment");
        final PathSegment namespacePathSegment = this.mockery.mock(PathSegment.class, "namespacePathSegment");
        final PathSegment extensionPathSegment = this.mockery.mock(PathSegment.class, "extensionPathSegment");

        pathSegments.add(graphsPathSegment);
        pathSegments.add(graphPathSegment);
        pathSegments.add(elementPathSegment);
        pathSegments.add(idPathSegment);
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

        ExtensionSegmentSet extensionSegmentSet = new ExtensionSegmentSet(uri, ExtensionPoint.EDGE);
        Assert.assertTrue(extensionSegmentSet.isValidFormat());
        Assert.assertEquals("ns", extensionSegmentSet.getNamespace());
        Assert.assertEquals("ext", extensionSegmentSet.getExtension());
        Assert.assertEquals("", extensionSegmentSet.getExtensionMethod());
        Assert.assertEquals("ns:ext", extensionSegmentSet.getNamespaceAndExtension());
        Assert.assertEquals("ns:ext+*", extensionSegmentSet.toString());
    }

    @Test
    public void isValidFormatEdgeExtensionValidWithMethod() {

        final UriInfo uri = this.mockery.mock(UriInfo.class);
        final List<PathSegment> pathSegments = new ArrayList<PathSegment>();
        final PathSegment graphsPathSegment = this.mockery.mock(PathSegment.class, "graphsSegment");
        final PathSegment graphPathSegment = this.mockery.mock(PathSegment.class, "graphPathSegment");
        final PathSegment elementPathSegment = this.mockery.mock(PathSegment.class, "elementPathSegment");
        final PathSegment idPathSegment = this.mockery.mock(PathSegment.class, "idPathSegment");
        final PathSegment namespacePathSegment = this.mockery.mock(PathSegment.class, "namespacePathSegment");
        final PathSegment extensionPathSegment = this.mockery.mock(PathSegment.class, "extensionPathSegment");
        final PathSegment methodPathSegment = this.mockery.mock(PathSegment.class, "methodPathSegment");

        pathSegments.add(graphsPathSegment);
        pathSegments.add(graphPathSegment);
        pathSegments.add(elementPathSegment);
        pathSegments.add(idPathSegment);
        pathSegments.add(namespacePathSegment);
        pathSegments.add(extensionPathSegment);
        pathSegments.add(methodPathSegment);

        this.mockery.checking(new Expectations() {{
            allowing(namespacePathSegment).getPath();
            will(returnValue("ns"));
            allowing(extensionPathSegment).getPath();
            will(returnValue("ext"));
            allowing(methodPathSegment).getPath();
            will(returnValue("meth"));
            allowing(uri).getPathSegments();
            will(returnValue(pathSegments));
        }});

        ExtensionSegmentSet extensionSegmentSet = new ExtensionSegmentSet(uri, ExtensionPoint.EDGE);
        Assert.assertTrue(extensionSegmentSet.isValidFormat());
        Assert.assertEquals("ns", extensionSegmentSet.getNamespace());
        Assert.assertEquals("ext", extensionSegmentSet.getExtension());
        Assert.assertEquals("meth", extensionSegmentSet.getExtensionMethod());
        Assert.assertEquals("ns:ext", extensionSegmentSet.getNamespaceAndExtension());
        Assert.assertEquals("ns:ext+meth", extensionSegmentSet.toString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorNullMethod() {
        new ExtensionSegmentSet("ns", "ext", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorNullExtensions() {
        new ExtensionSegmentSet("ns", null, "meth");
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorNullNamespace() {
        new ExtensionSegmentSet(null, "ext", "meth");
    }

    @Test
    public void equalsValid() {
        ExtensionSegmentSet extensionSegmentSet = new ExtensionSegmentSet("ns", "ext", "meth");
        ExtensionSegmentSet extensionSegmentSetSame = new ExtensionSegmentSet("ns", "ext", "meth");

        Assert.assertTrue(extensionSegmentSet.equals(extensionSegmentSetSame));
    }

    @Test
    public void equalsInvalid() {
        ExtensionSegmentSet extensionSegmentSet = new ExtensionSegmentSet("ns", "ext", "meth");
        ExtensionSegmentSet extensionSegmentSetSame = new ExtensionSegmentSet("ns", "ext", "different method");

        Assert.assertFalse(extensionSegmentSet.equals(extensionSegmentSetSame));
    }
}
