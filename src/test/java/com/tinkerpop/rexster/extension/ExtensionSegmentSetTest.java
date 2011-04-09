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
    public void isValidFormatNoNamespace() {

        final UriInfo uri = this.mockery.mock(UriInfo.class);
        final List<PathSegment> pathSegments = new ArrayList<PathSegment>();
        final PathSegment graphPathSegment = this.mockery.mock(PathSegment.class, "graphPathSegment");

        pathSegments.add(graphPathSegment);

        this.mockery.checking(new Expectations() {{
            allowing(uri).getPathSegments();
            will(returnValue(pathSegments));
        }});


        ExtensionSegmentSet extensionSegmentSet = new ExtensionSegmentSet(uri);
        Assert.assertFalse(extensionSegmentSet.isValidFormat());
        Assert.assertEquals("", extensionSegmentSet.getNamespace());
        Assert.assertEquals("", extensionSegmentSet.getExtension());
        Assert.assertEquals("", extensionSegmentSet.getExtensionMethod());
        Assert.assertEquals("", extensionSegmentSet.getNamespaceAndExtension());
        Assert.assertEquals("[parse error]:[parse error]+*", extensionSegmentSet.toString());
    }

    @Test
    public void isValidFormatNoExtension() {

        final UriInfo uri = this.mockery.mock(UriInfo.class);
        final List<PathSegment> pathSegments = new ArrayList<PathSegment>();
        final PathSegment graphPathSegment = this.mockery.mock(PathSegment.class, "graphPathSegment");
        final PathSegment namespacePathSegment = this.mockery.mock(PathSegment.class, "namespacePathSegment");

        pathSegments.add(graphPathSegment);
        pathSegments.add(namespacePathSegment);

        this.mockery.checking(new Expectations() {{
            allowing(namespacePathSegment).getPath();
            will(returnValue("ns"));
            allowing(uri).getPathSegments();
            will(returnValue(pathSegments));
        }});


        ExtensionSegmentSet extensionSegmentSet = new ExtensionSegmentSet(uri);
        Assert.assertFalse(extensionSegmentSet.isValidFormat());
        Assert.assertEquals("ns", extensionSegmentSet.getNamespace());
        Assert.assertEquals("", extensionSegmentSet.getExtension());
        Assert.assertEquals("", extensionSegmentSet.getExtensionMethod());
        Assert.assertEquals("", extensionSegmentSet.getNamespaceAndExtension());
        Assert.assertEquals("ns:[parse error]+*", extensionSegmentSet.toString());
    }

    @Test
    public void isValidFormatValidNoMethod() {

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

        ExtensionSegmentSet extensionSegmentSet = new ExtensionSegmentSet(uri);
        Assert.assertTrue(extensionSegmentSet.isValidFormat());
        Assert.assertEquals("ns", extensionSegmentSet.getNamespace());
        Assert.assertEquals("ext", extensionSegmentSet.getExtension());
        Assert.assertEquals("", extensionSegmentSet.getExtensionMethod());
        Assert.assertEquals("ns:ext", extensionSegmentSet.getNamespaceAndExtension());
        Assert.assertEquals("ns:ext+*", extensionSegmentSet.toString());
    }

    @Test
    public void isValidFormatValidWithMethod() {

        final UriInfo uri = this.mockery.mock(UriInfo.class);
        final List<PathSegment> pathSegments = new ArrayList<PathSegment>();
        final PathSegment graphPathSegment = this.mockery.mock(PathSegment.class, "graphPathSegment");
        final PathSegment namespacePathSegment = this.mockery.mock(PathSegment.class, "namespacePathSegment");
        final PathSegment extensionPathSegment = this.mockery.mock(PathSegment.class, "extensionPathSegment");
        final PathSegment methodPathSegment = this.mockery.mock(PathSegment.class, "methodPathSegment");

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

        ExtensionSegmentSet extensionSegmentSet = new ExtensionSegmentSet(uri);
        Assert.assertTrue(extensionSegmentSet.isValidFormat());
        Assert.assertEquals("ns", extensionSegmentSet.getNamespace());
        Assert.assertEquals("ext", extensionSegmentSet.getExtension());
        Assert.assertEquals("meth", extensionSegmentSet.getExtensionMethod());
        Assert.assertEquals("ns:ext", extensionSegmentSet.getNamespaceAndExtension());
        Assert.assertEquals("ns:ext+meth", extensionSegmentSet.toString());
    }

}
