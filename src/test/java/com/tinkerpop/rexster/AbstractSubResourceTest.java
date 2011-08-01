package com.tinkerpop.rexster;

import com.tinkerpop.blueprints.pgm.Graph;
import com.tinkerpop.rexster.extension.ExtensionConfiguration;
import com.tinkerpop.rexster.extension.ExtensionDefinition;
import com.tinkerpop.rexster.extension.ExtensionMethod;
import com.tinkerpop.rexster.extension.ExtensionPoint;
import com.tinkerpop.rexster.extension.ExtensionResponse;
import com.tinkerpop.rexster.extension.ExtensionSegmentSet;
import com.tinkerpop.rexster.extension.GremlinExtension;
import com.tinkerpop.rexster.extension.HttpMethod;
import com.tinkerpop.rexster.extension.RexsterExtension;
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
import java.util.Map;

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
        final RexsterApplicationProvider rap = this.mockery.mock(RexsterApplicationProvider.class);

        this.mockery.checking(new Expectations() {{
            allowing(req).getParameterMap();
            will(returnValue(new HashMap<String, String>()));
            allowing(rap).getApplicationGraph(with(any(String.class)));
            will(returnValue(rag));
        }});

        this.mockResource = new MockAbstractSubResource(uriInfo, req, rap);
    }

    @Test
    public void getTypedPropertyValueNullPropertyValue() {
        Object emptyString = this.mockResource.getTypedPropertyValue(null);
        Assert.assertNotNull(emptyString);
        Assert.assertEquals("", emptyString);
    }

    @Test
    public void getTypedPropertyValueEmptyPropertyValue() {
        Object emptyString = this.mockResource.getTypedPropertyValue("");
        Assert.assertNotNull(emptyString);
        Assert.assertEquals("", emptyString);
    }

    @Test
    public void getTypedPropertyValueNonTyped() {
        Object typedPropertyValue = this.mockResource.getTypedPropertyValue("xyz");
        Assert.assertNotNull(typedPropertyValue);
        Assert.assertEquals("xyz", typedPropertyValue);

        typedPropertyValue = this.mockResource.getTypedPropertyValue("123");
        Assert.assertNotNull(typedPropertyValue);
        Assert.assertEquals("123", typedPropertyValue);
    }

    @Test
    public void getTypedPropertyValueBadFormats() {
        Object typedPropertyValue = this.mockResource.getTypedPropertyValue("i,123)");
        Assert.assertNotNull(typedPropertyValue);
        Assert.assertEquals("i,123)", typedPropertyValue);

        typedPropertyValue = this.mockResource.getTypedPropertyValue("(dfd,123)");
        Assert.assertNotNull(typedPropertyValue);
        // TODO (string, marko) should be a datatype of string (causes problems for this model though)
        // Assert.assertEquals("(dfd,123)", typedPropertyValue);
    }

    @Test
    public void getTypedPropertyValueIntegerTyped() {
        Object typedPropertyValue = this.mockResource.getTypedPropertyValue("(i,123)");
        Assert.assertNotNull(typedPropertyValue);
        Assert.assertEquals(123, typedPropertyValue);

        typedPropertyValue = this.mockResource.getTypedPropertyValue("(integer,123)");
        Assert.assertNotNull(typedPropertyValue);
        Assert.assertEquals(123, typedPropertyValue);

        typedPropertyValue = this.mockResource.getTypedPropertyValue("(integer,123bad)");
        Assert.assertNotNull(typedPropertyValue);
        Assert.assertEquals("123bad", typedPropertyValue);
    }

    @Test
    public void getTypedPropertyValueLongTyped() {
        Object typedPropertyValue = this.mockResource.getTypedPropertyValue("(l,123)");
        Assert.assertNotNull(typedPropertyValue);
        Assert.assertEquals(123l, typedPropertyValue);

        typedPropertyValue = this.mockResource.getTypedPropertyValue("(long,123)");
        Assert.assertNotNull(typedPropertyValue);
        Assert.assertEquals(123l, typedPropertyValue);

        typedPropertyValue = this.mockResource.getTypedPropertyValue("(long,123bad)");
        Assert.assertNotNull(typedPropertyValue);
        Assert.assertEquals("123bad", typedPropertyValue);
    }

    @Test
    public void getTypedPropertyValueDoubleTyped() {
        Object typedPropertyValue = this.mockResource.getTypedPropertyValue("(d,123)");
        Assert.assertNotNull(typedPropertyValue);
        Assert.assertEquals(123d, typedPropertyValue);

        typedPropertyValue = this.mockResource.getTypedPropertyValue("(double,123)");
        Assert.assertNotNull(typedPropertyValue);
        Assert.assertEquals(123d, typedPropertyValue);

        typedPropertyValue = this.mockResource.getTypedPropertyValue("(double,123bad)");
        Assert.assertNotNull(typedPropertyValue);
        Assert.assertEquals("123bad", typedPropertyValue);
    }

    @Test
    public void getTypedPropertyValueFloatTyped() {
        Object typedPropertyValue = this.mockResource.getTypedPropertyValue("(f,123)");
        Assert.assertNotNull(typedPropertyValue);
        Assert.assertEquals(123f, typedPropertyValue);

        typedPropertyValue = this.mockResource.getTypedPropertyValue("(float,123)");
        Assert.assertNotNull(typedPropertyValue);
        Assert.assertEquals(123f, typedPropertyValue);

        typedPropertyValue = this.mockResource.getTypedPropertyValue("(float,123bad)");
        Assert.assertNotNull(typedPropertyValue);
        Assert.assertEquals("123bad", typedPropertyValue);
    }

    @Test
    public void getTypedPropertyValueListNonTyped() {
        Object typedPropertyValue = this.mockResource.getTypedPropertyValue("(list,(123,321,456,678))");
        Assert.assertNotNull(typedPropertyValue);
        Assert.assertTrue(typedPropertyValue instanceof List);

        List list = (List) typedPropertyValue;
        Assert.assertEquals(4, list.size());
        Assert.assertEquals("123", list.get(0));
        Assert.assertEquals("321", list.get(1));
        Assert.assertEquals("456", list.get(2));
        Assert.assertEquals("678", list.get(3));
    }

    @Test
    public void getTypedPropertyValueListTyped() {
        Object typedPropertyValue = this.mockResource.getTypedPropertyValue("(list,((integer,123),(d,321),456,(f,678)))");
        Assert.assertNotNull(typedPropertyValue);
        Assert.assertTrue(typedPropertyValue instanceof List);

        List list = (List) typedPropertyValue;
        Assert.assertEquals(4, list.size());
        Assert.assertEquals(123, list.get(0));
        Assert.assertEquals(321d, list.get(1));
        Assert.assertEquals("456", list.get(2));
        Assert.assertEquals(678f, list.get(3));
    }

    @Test
    public void getTypedPropertyValueListTypedBadFormat() {
        Object typedPropertyValue = this.mockResource.getTypedPropertyValue("(list,((integer,123,(d,321),456,(f,678)))");
        Assert.assertNotNull(typedPropertyValue);
        Assert.assertTrue(typedPropertyValue instanceof List);

        List list = (List) typedPropertyValue;
        Assert.assertEquals(1, list.size());
        Assert.assertEquals("123,(d,321),456,(f,678", list.get(0));
    }

    @Test
    public void getTypedPropertyValueMapNonTyped() {
        Object typedPropertyValue = this.mockResource.getTypedPropertyValue("(map,(a=123,b=321))");
        Assert.assertNotNull(typedPropertyValue);
        Assert.assertTrue(typedPropertyValue instanceof Map);

        Map map = (Map) typedPropertyValue;
        Assert.assertEquals(2, map.size());
        Assert.assertTrue(map.containsKey("a"));
        Assert.assertTrue(map.containsKey("b"));
        Assert.assertEquals("123", map.get("a"));
        Assert.assertEquals("321", map.get("b"));
    }

    @Test
    public void getTypedPropertyValueMapTyped() {
        Object typedPropertyValue = this.mockResource.getTypedPropertyValue("(map,(a=(i,123),b=(d,321)))");
        Assert.assertNotNull(typedPropertyValue);
        Assert.assertTrue(typedPropertyValue instanceof Map);

        Map map = (Map) typedPropertyValue;
        Assert.assertEquals(2, map.size());
        Assert.assertTrue(map.containsKey("a"));
        Assert.assertTrue(map.containsKey("b"));
        Assert.assertEquals(123, map.get("a"));
        Assert.assertEquals(321d, map.get("b"));
    }

    @Test
    public void getTypedPropertyValueMapTypedEmbedded() {
        Object typedPropertyValue = this.mockResource.getTypedPropertyValue("(map,(a=(i,123),b=(d,321),c=(map,(x=y))))");
        Assert.assertNotNull(typedPropertyValue);
        Assert.assertTrue(typedPropertyValue instanceof Map);

        Map map = (Map) typedPropertyValue;
        Assert.assertEquals(3, map.size());
        Assert.assertTrue(map.containsKey("a"));
        Assert.assertTrue(map.containsKey("b"));
        Assert.assertTrue(map.containsKey("c"));
        Assert.assertEquals(123, map.get("a"));
        Assert.assertEquals(321d, map.get("b"));

        Assert.assertTrue(map.get("c") instanceof Map);
        Map inner = (Map) map.get("c");
        Assert.assertEquals("y", inner.get("x"));
    }

    @Test
    public void getTypedPropertyValueListTypedEmbeddedList() {
        Object typedPropertyValue = this.mockResource.getTypedPropertyValue("(list,((integer,123),(d,321),456,(f,678),(list,(123,(i,456),789))))");
        Assert.assertNotNull(typedPropertyValue);
        Assert.assertTrue(typedPropertyValue instanceof List);

        List list = (List) typedPropertyValue;
        Assert.assertEquals(5, list.size());
        Assert.assertEquals(123, list.get(0));
        Assert.assertEquals(321d, list.get(1));
        Assert.assertEquals("456", list.get(2));
        Assert.assertEquals(678f, list.get(3));

        Assert.assertTrue(list.get(4) instanceof List);
        List innerList = (List) list.get(4);
        Assert.assertEquals(3, innerList.size());
        Assert.assertEquals("123", innerList.get(0));
        Assert.assertEquals(456, innerList.get(1));
        Assert.assertEquals("789", innerList.get(2));

    }

    @Test
    public void getTypedPropertyValueMapTypedComplexMap() {
        Object typedPropertyValue = this.mockResource.getTypedPropertyValue("(map,(a=(i,123),b=(d,321),c=(map,(x=y)),d=(list,(321,(f,123)))))");
        Assert.assertNotNull(typedPropertyValue);
        Assert.assertTrue(typedPropertyValue instanceof Map);

        Map map = (Map) typedPropertyValue;
        Assert.assertEquals(4, map.size());
        Assert.assertTrue(map.containsKey("a"));
        Assert.assertTrue(map.containsKey("b"));
        Assert.assertTrue(map.containsKey("c"));
        Assert.assertEquals(123, map.get("a"));
        Assert.assertEquals(321d, map.get("b"));

        Assert.assertTrue(map.get("c") instanceof Map);
        Map inner = (Map) map.get("c");
        Assert.assertEquals("y", inner.get("x"));

        Assert.assertTrue(map.get("d") instanceof List);
        List innerList = (List) map.get("d");
        Assert.assertEquals(2, innerList.size());
        Assert.assertEquals("321", innerList.get(0));
        Assert.assertEquals(123f, innerList.get(1));
    }

    @Test
    public void getTypedPropertyValueListTypedComplexList() {
        Object typedPropertyValue = this.mockResource.getTypedPropertyValue("(list,((integer,123),(d,321),456,(f,678),(list,(123,(i,456),789)),(map,(x=y)),(map,(x=(i,123)))))");
        Assert.assertNotNull(typedPropertyValue);
        Assert.assertTrue(typedPropertyValue instanceof List);

        List list = (List) typedPropertyValue;
        Assert.assertEquals(7, list.size());
        Assert.assertEquals(123, list.get(0));
        Assert.assertEquals(321d, list.get(1));
        Assert.assertEquals("456", list.get(2));
        Assert.assertEquals(678f, list.get(3));

        Assert.assertTrue(list.get(4) instanceof List);
        List innerList = (List) list.get(4);
        Assert.assertEquals(3, innerList.size());
        Assert.assertEquals("123", innerList.get(0));
        Assert.assertEquals(456, innerList.get(1));
        Assert.assertEquals("789", innerList.get(2));

        Assert.assertTrue(list.get(5) instanceof Map);
        Map innerMap1 = (Map) list.get(5);
        Assert.assertTrue(innerMap1.containsKey("x"));
        Assert.assertEquals("y", innerMap1.get("x"));

        Assert.assertTrue(list.get(6) instanceof Map);
        Map innerMap2 = (Map) list.get(6);
        Assert.assertTrue(innerMap2.containsKey("x"));
        Assert.assertEquals(123, innerMap2.get("x"));
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

        public MockAbstractSubResource(UriInfo ui, HttpServletRequest req, RexsterApplicationProvider rap) {
            super(rap);
            this.httpServletRequest = req;
            this.uriInfo = ui;

            extensionCache.clear();
        }

        public Object getTypedPropertyValue(String propertyValue) {
            return super.getTypedPropertyValue(propertyValue);
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
