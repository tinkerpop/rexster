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

public class ExtensionAllowedTest {
    private Mockery mockery = new JUnit4Mockery();

    @Test
    public void isExtensionAllowedAllowAll() {
        ExtensionAllowed configuration = new ExtensionAllowed("*:*");
        ExtensionSegmentSet extensionSegmentSet = new ExtensionSegmentSet(this.mockTheUri("ns", "extension", ""), ExtensionPoint.GRAPH);
        Assert.assertTrue(configuration.isExtensionAllowed(extensionSegmentSet));
    }

    @Test
    public void isExtensionAllowedAllowAllInNamespace() {
        ExtensionAllowed configuration = new ExtensionAllowed("ns:*");
        ExtensionSegmentSet extensionSegmentSet = new ExtensionSegmentSet(this.mockTheUri("ns", "extension", ""), ExtensionPoint.GRAPH);
        Assert.assertTrue(configuration.isExtensionAllowed(extensionSegmentSet));

        extensionSegmentSet = new ExtensionSegmentSet(this.mockTheUri("bs", "extension", ""), ExtensionPoint.GRAPH);
        Assert.assertFalse(configuration.isExtensionAllowed(extensionSegmentSet));
    }

    @Test
    public void isExtensionAllowedAllowSpecificExtension() {
        ExtensionAllowed configuration = new ExtensionAllowed("ns:allowed");
        ExtensionSegmentSet extensionSegmentSet = new ExtensionSegmentSet(this.mockTheUri("ns", "allowed", ""), ExtensionPoint.GRAPH);
        Assert.assertTrue(configuration.isExtensionAllowed(extensionSegmentSet));

        extensionSegmentSet = new ExtensionSegmentSet(this.mockTheUri("ns", "not_allowed", ""), ExtensionPoint.GRAPH);
        Assert.assertFalse(configuration.isExtensionAllowed(extensionSegmentSet));
    }

    @Test
    public void isExtensionAllowedAllowDashedExtension() {
        ExtensionAllowed configuration = new ExtensionAllowed("ns-dash:allowed-dash");
        ExtensionSegmentSet extensionSegmentSet = new ExtensionSegmentSet(this.mockTheUri("ns-dash", "allowed-dash", ""), ExtensionPoint.GRAPH);
        Assert.assertTrue(configuration.isExtensionAllowed(extensionSegmentSet));

        extensionSegmentSet = new ExtensionSegmentSet(this.mockTheUri("ns", "not_allowed", ""), ExtensionPoint.GRAPH);
        Assert.assertFalse(configuration.isExtensionAllowed(extensionSegmentSet));
    }

    private UriInfo mockTheUri(final String namespace, final String extension, final String method) {
        this.mockery = new JUnit4Mockery();

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
            will(returnValue(namespace));
            allowing(extensionPathSegment).getPath();
            will(returnValue(extension));
            allowing(methodPathSegment).getPath();
            will(returnValue(method));
            allowing(uri).getPathSegments();
            will(returnValue(pathSegments));
        }});

        return uri;
    }
}
