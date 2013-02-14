package com.tinkerpop.rexster;

import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.rexster.extension.ExtensionConfiguration;
import com.tinkerpop.rexster.extension.ExtensionDefinition;
import com.tinkerpop.rexster.extension.ExtensionMethod;
import com.tinkerpop.rexster.extension.ExtensionPoint;
import com.tinkerpop.rexster.extension.ExtensionResponse;
import com.tinkerpop.rexster.extension.ExtensionSegmentSet;
import com.tinkerpop.rexster.extension.HttpMethod;
import com.tinkerpop.rexster.extension.RexsterExtension;
import com.tinkerpop.rexster.gremlin.GremlinExtension;
import com.tinkerpop.rexster.server.RexsterApplication;
import junit.framework.Assert;
import org.codehaus.jettison.json.JSONObject;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriInfo;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class AbstractSubResourceTest {
    private Mockery mockery = new JUnit4Mockery();
    private MockAbstractSubResource mockResource;

    @Before
    public void init() {
        this.mockery = new JUnit4Mockery();

        final UriInfo uriInfo = this.mockery.mock(UriInfo.class);
        final HttpServletRequest req = this.mockery.mock(HttpServletRequest.class);

        final Graph graph = this.mockery.mock(Graph.class);
        final RexsterApplicationGraph rag = new RexsterApplicationGraph("graph", graph);
        final RexsterApplication ra = this.mockery.mock(RexsterApplication.class);

        this.mockery.checking(new Expectations() {{
            allowing(req).getParameterMap();
            will(returnValue(new HashMap<String, String>()));
            allowing(ra).getApplicationGraph(with(any(String.class)));
            will(returnValue(rag));
        }});

        this.mockResource = new MockAbstractSubResource(uriInfo, req, ra);
    }

    @Test
    public void findExtensionGraphExtensionNotPresent() {

        this.mockery = new JUnit4Mockery();
        UriInfo uri = this.mockTheUri("not", "here", "");

        List<RexsterExtension> extensions = this.mockResource.findExtensionExposed(new ExtensionSegmentSet(uri, ExtensionPoint.GRAPH));
        Assert.assertNull(extensions);
    }

    @Test
    public void findExtensionGraphExtensionFound() {

        this.mockery = new JUnit4Mockery();
        UriInfo uri = this.mockTheUri("tp", "gremlin", "");

        List<RexsterExtension> extensions = this.mockResource.findExtensionExposed(new ExtensionSegmentSet(uri, ExtensionPoint.GRAPH));
        Assert.assertNotNull(extensions);
        Assert.assertTrue(extensions.get(0) instanceof GremlinExtension);
    }

    @Test
    public void findExtensionMethodNotPresent() {
        List<RexsterExtension> rexsterExtensions = new ArrayList<RexsterExtension>();
        rexsterExtensions.add(new MockRexsterExtension());
        ExtensionMethod m = this.mockResource.findExtensionMethodExposed(rexsterExtensions, ExtensionPoint.VERTEX, "action", HttpMethod.ANY);
        Assert.assertNull(m);
    }

    @Test
    public void findExtensionMethodFoundRoot() {
        List<RexsterExtension> rexsterExtensions = new ArrayList<RexsterExtension>();
        rexsterExtensions.add(new MockRexsterExtension());
        ExtensionMethod m = this.mockResource.findExtensionMethodExposed(rexsterExtensions, ExtensionPoint.GRAPH, "", HttpMethod.ANY);
        Assert.assertNotNull(m);

        Method methodFound = m.getMethod();
        Assert.assertNotNull(methodFound);
        Assert.assertEquals("doRoot", methodFound.getName());
    }

    @Test
    public void findExtensionMethodFoundSpecificAction() {
        List<RexsterExtension> rexsterExtensions = new ArrayList<RexsterExtension>();
        rexsterExtensions.add(new MockRexsterExtension());
        ExtensionMethod m = this.mockResource.findExtensionMethodExposed(rexsterExtensions, ExtensionPoint.GRAPH, "action", HttpMethod.ANY);
        Assert.assertNotNull(m);

        Method methodFound = m.getMethod();
        Assert.assertNotNull(methodFound);
        Assert.assertEquals("doAction", methodFound.getName());
    }

    @Test
    public void findExtensionMethodFoundSpecificActionMultipleExtensionClasses() {
        List<RexsterExtension> rexsterExtensions = new ArrayList<RexsterExtension>();
        rexsterExtensions.add(new MockRexsterExtension());
        rexsterExtensions.add(new MockAddOnRexsterExtension());
        ExtensionMethod m = this.mockResource.findExtensionMethodExposed(rexsterExtensions, ExtensionPoint.GRAPH, "action", HttpMethod.ANY);
        Assert.assertNotNull(m);

        Method methodFound = m.getMethod();
        Assert.assertNotNull(methodFound);
        Assert.assertEquals("doAction", methodFound.getName());

        m = this.mockResource.findExtensionMethodExposed(rexsterExtensions, ExtensionPoint.GRAPH, "addon", HttpMethod.ANY);
        Assert.assertNotNull(m);

        methodFound = m.getMethod();
        Assert.assertNotNull(methodFound);
        Assert.assertEquals("doAddOnAction", methodFound.getName());
    }

    @Test
    public void findExtensionMethodFoundUseRootMethod() {
        List<RexsterExtension> rexsterExtensions = new ArrayList<RexsterExtension>();
        rexsterExtensions.add(new MockRexsterExtension());
        ExtensionMethod m = this.mockResource.findExtensionMethodExposed(rexsterExtensions, ExtensionPoint.GRAPH, "headonly", HttpMethod.POST);
        Assert.assertNotNull(m);

        Method methodFound = m.getMethod();
        Assert.assertNotNull(methodFound);
        Assert.assertEquals("doRoot", methodFound.getName());
    }

    @Test
    public void findExtensionMethodMultipleRootCheck() {
        List<RexsterExtension> rexsterExtensions = new ArrayList<RexsterExtension>();
        rexsterExtensions.add(new MockMultiRootRexsterExtension());
        ExtensionMethod m = this.mockResource.findExtensionMethodExposed(rexsterExtensions, ExtensionPoint.GRAPH, "", HttpMethod.GET);
        Assert.assertNotNull(m);

        Method methodFound = m.getMethod();
        Assert.assertNotNull(methodFound);
        Assert.assertEquals("doRootGet", methodFound.getName());

        // validates that the second method will get found
        m = this.mockResource.findExtensionMethodExposed(rexsterExtensions, ExtensionPoint.GRAPH, "", HttpMethod.POST);
        Assert.assertNotNull(m);

        methodFound = m.getMethod();
        Assert.assertNotNull(methodFound);
        Assert.assertEquals("doRootPost", methodFound.getName());
    }

    @Test
    public void findExtensionMethodFoundSpecificActionAndMethod() {
        List<RexsterExtension> rexsterExtensions = new ArrayList<RexsterExtension>();
        rexsterExtensions.add(new MockRexsterExtension());
        ExtensionMethod m = this.mockResource.findExtensionMethodExposed(rexsterExtensions, ExtensionPoint.GRAPH, "headonly", HttpMethod.HEAD);
        Assert.assertNotNull(m);

        Method methodFound = m.getMethod();
        Assert.assertNotNull(methodFound);
        Assert.assertEquals("headAccessOnly", methodFound.getName());
    }

    @Test
    public void tryAppendRexsterAttributesIfJsonNonJsonMediaType() {
        ExtensionResponse responseFromExtension = ExtensionResponse.noContent();
        ExtensionResponse extResponse = this.mockResource.tryAppendRexsterAttributesIfJsonExposed(
                responseFromExtension, null, MediaType.APPLICATION_FORM_URLENCODED);

        Assert.assertNotNull(extResponse);
        Assert.assertEquals(responseFromExtension, extResponse);
    }

    @Test
    public void tryAppendRexsterAttributesIfJsonConfiguredNo() {
        ExtensionResponse responseFromExtension = ExtensionResponse.noContent();

        final ExtensionDefinition extensionDefinition = this.mockery.mock(ExtensionDefinition.class);
        this.mockery.checking(new Expectations() {{
            allowing(extensionDefinition).tryIncludeRexsterAttributes();
            will(returnValue(false));
        }});

        ExtensionMethod extensionMethod = new ExtensionMethod(null, extensionDefinition, null, new MockRexsterExtension());
        ExtensionResponse extResponse = this.mockResource.tryAppendRexsterAttributesIfJsonExposed(
                responseFromExtension, extensionMethod, MediaType.APPLICATION_JSON);

        Assert.assertNotNull(extResponse);
        Assert.assertEquals(responseFromExtension, extResponse);
    }

    @Test
    public void tryAppendRexsterAttributesIfJsonNotJsonInEntity() {
        ExtensionResponse responseFromExtension = ExtensionResponse.noContent();

        final ExtensionDefinition extensionDefinition = this.mockery.mock(ExtensionDefinition.class);
        this.mockery.checking(new Expectations() {{
            allowing(extensionDefinition).tryIncludeRexsterAttributes();
            will(returnValue(true));
        }});

        ExtensionMethod extensionMethod = new ExtensionMethod(null, extensionDefinition, null, new MockRexsterExtension());
        ExtensionResponse extResponse = this.mockResource.tryAppendRexsterAttributesIfJsonExposed(
                responseFromExtension, extensionMethod, MediaType.APPLICATION_JSON);

        Assert.assertNotNull(extResponse);
        Assert.assertEquals(responseFromExtension, extResponse);
    }

    @Test
    public void tryAppendRexsterAttributesIfJsonValid() {
        HashMap map = new HashMap() {{
            put("me", "you");
        }};

        ExtensionResponse responseFromExtension = ExtensionResponse.ok(new JSONObject(map));

        final ExtensionDefinition extensionDefinition = this.mockery.mock(ExtensionDefinition.class);
        this.mockery.checking(new Expectations() {{
            allowing(extensionDefinition).tryIncludeRexsterAttributes();
            will(returnValue(true));
        }});

        ExtensionMethod extensionMethod = new ExtensionMethod(null, extensionDefinition, null, new MockRexsterExtension());
        ExtensionResponse extResponse = this.mockResource.tryAppendRexsterAttributesIfJsonExposed(
                responseFromExtension, extensionMethod, MediaType.APPLICATION_JSON);

        Assert.assertNotNull(extResponse);

        JSONObject jsonObjectWithAttributes = (JSONObject) extResponse.getJerseyResponse().getEntity();
        Assert.assertNotNull(jsonObjectWithAttributes);
        Assert.assertTrue(jsonObjectWithAttributes.has("me"));
        Assert.assertTrue(jsonObjectWithAttributes.has(Tokens.VERSION));
        Assert.assertTrue(jsonObjectWithAttributes.has(Tokens.QUERY_TIME));
    }

    private UriInfo mockTheUri(final String namespace, final String extension, final String method) {
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

    private class MockAbstractSubResource extends AbstractSubResource {

        public MockAbstractSubResource(UriInfo ui, HttpServletRequest req, RexsterApplication ra) {
            super(ra);
            this.httpServletRequest = req;
            this.uriInfo = ui;

            extensionCache.clear();
        }

        public List<RexsterExtension> findExtensionExposed(ExtensionSegmentSet extensionSegmentSet) {
            return findExtensionClasses(extensionSegmentSet);
        }

        public ExtensionMethod findExtensionMethodExposed(
                List<RexsterExtension> rexsterExtensions, ExtensionPoint extensionPoint, String extensionAction, HttpMethod httpMethodRequested) {
            return findExtensionMethod(rexsterExtensions, extensionPoint, extensionAction, httpMethodRequested);
        }

        public ExtensionSegmentSet parseUriForExtensionSegmentExposed(String graphName, ExtensionPoint extensionPoint) {
            return parseUriForExtensionSegment(graphName, extensionPoint);
        }

        public ExtensionResponse tryAppendRexsterAttributesIfJsonExposed(ExtensionResponse extResponse, ExtensionMethod methodToCall, String mediaType) {
            return tryAppendRexsterAttributesIfJson(extResponse, methodToCall, mediaType);
        }
    }

    private class MockRexsterExtension implements RexsterExtension {

        @ExtensionDefinition(extensionPoint = ExtensionPoint.GRAPH)
        public ExtensionResponse doRoot() {
            return null;
        }

        @ExtensionDefinition(extensionPoint = ExtensionPoint.GRAPH, path = "action")
        public ExtensionResponse doAction() {
            return null;
        }

        @ExtensionDefinition(extensionPoint = ExtensionPoint.GRAPH, path = "headonly", method = HttpMethod.HEAD)
        public ExtensionResponse headAccessOnly() {
            return null;
        }

        public void justIgnoreMe() {
            // ensuring no fails when no ExtensionDefinition annotation is supplied
        }

        public boolean isConfigurationValid(ExtensionConfiguration extensionConfiguration) {
            return true;
        }
    }

    private class MockAddOnRexsterExtension implements RexsterExtension {
        @ExtensionDefinition(extensionPoint = ExtensionPoint.GRAPH, path = "addon")
        public ExtensionResponse doAddOnAction() {
            return null;
        }

        public boolean isConfigurationValid(ExtensionConfiguration extensionConfiguration) {
            return true;
        }
    }

    private class MockMultiRootRexsterExtension implements RexsterExtension {
        @ExtensionDefinition(extensionPoint = ExtensionPoint.GRAPH, method = HttpMethod.GET)
        public ExtensionResponse doRootGet() {
            return null;
        }

        @ExtensionDefinition(extensionPoint = ExtensionPoint.GRAPH, path = "action")
        public ExtensionResponse justSomeJunkToBeADistraction() {
            return null;
        }

        @ExtensionDefinition(extensionPoint = ExtensionPoint.GRAPH, method = HttpMethod.POST)
        public ExtensionResponse doRootPost() {
            return null;
        }

        public boolean isConfigurationValid(ExtensionConfiguration extensionConfiguration) {
            return true;
        }
    }
}
