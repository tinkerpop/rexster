package com.tinkerpop.rexster;

import com.tinkerpop.blueprints.pgm.Graph;
import com.tinkerpop.rexster.extension.*;
import junit.framework.Assert;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
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
    public void findExtensionGraphExtensionNotPresent(){

        this.mockery = new JUnit4Mockery();
        UriInfo uri = this.mockTheUri("not", "here", "");

        RexsterExtension extension = this.mockResource.findExtensionExposed(new ExtensionSegmentSet(uri, ExtensionPoint.GRAPH));
        Assert.assertNull(extension);
    }

    @Test
    public void findExtensionGraphExtensionFound(){

        this.mockery = new JUnit4Mockery();
        UriInfo uri = this.mockTheUri("tp", "gremlin", "");

        RexsterExtension extension = this.mockResource.findExtensionExposed(new ExtensionSegmentSet(uri, ExtensionPoint.GRAPH));
        Assert.assertNotNull(extension);
        Assert.assertTrue(extension instanceof GremlinExtension);
    }

    @Test
    public void findExtensionMethodNotPresent() {
        MockRexsterExtension ext = new MockRexsterExtension();
        ExtensionMethod m = this.mockResource.findExtensionMethodExposed(ext, ExtensionPoint.VERTEX, "action");
        Assert.assertNull(m);

        // this does a check that really checks an bad allowable configuration where a match
        // of a method will occur if the action doesn't match but there is an extension point
        // with no path supplied.  yet to see if this is a big deal or not.
        m = this.mockResource.findExtensionMethodExposed(ext, ExtensionPoint.GRAPH, "something-that-does-not-exist");
        Assert.assertNotNull(m);
    }

    @Test
    public void findExtensionMethodFoundRoot() {
        MockRexsterExtension ext = new MockRexsterExtension();
        ExtensionMethod m = this.mockResource.findExtensionMethodExposed(ext, ExtensionPoint.GRAPH, "");
        Assert.assertNotNull(m);
    }

    @Test
    public void findExtensionMethodFoundSpecificAction() {
        MockRexsterExtension ext = new MockRexsterExtension();
        ExtensionMethod m = this.mockResource.findExtensionMethodExposed(ext, ExtensionPoint.GRAPH, "action");
        Assert.assertNotNull(m);
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
        }

        public Object getTypedPropertyValue(String propertyValue) {
            return super.getTypedPropertyValue(propertyValue);
        }

        public RexsterExtension findExtensionExposed(ExtensionSegmentSet extensionSegmentSet) {
            return findExtension(extensionSegmentSet);
        }

        public ExtensionMethod findExtensionMethodExposed(
                RexsterExtension rexsterExtension, ExtensionPoint extensionPoint, String extensionAction) {
            return findExtensionMethod(rexsterExtension, extensionPoint, extensionAction);
        }

        public ExtensionSegmentSet parseUriForExtensionSegment(String graphName, ExtensionPoint extensionPoint) {
            return parseUriForExtensionSegment(graphName, extensionPoint);
        }
    }

    private class MockRexsterExtension implements RexsterExtension {

        @ExtensionDefinition(extensionPoint = ExtensionPoint.GRAPH)
        public ExtensionResponse doRoot(){
            return null;
        }

        @ExtensionDefinition(extensionPoint = ExtensionPoint.GRAPH, path = "action")
        public ExtensionResponse doAction(){
            return null;
        }

        public void justIgnoreMe() {
            // ensuring no fails when no ExtensionDefinition annotation is supplied
        }

        public boolean isConfigurationValid(ExtensionConfiguration extensionConfiguration) {
            return true;
        }
    }
}
