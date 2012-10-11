package com.tinkerpop.rexster;

import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Index;
import com.tinkerpop.blueprints.IndexableGraph;
import com.tinkerpop.blueprints.KeyIndexableGraph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.sail.SailGraph;
import com.tinkerpop.blueprints.impls.sail.SailGraphFactory;
import com.tinkerpop.blueprints.impls.sail.impls.MemoryStoreSailGraph;
import com.tinkerpop.blueprints.impls.tg.TinkerGraph;
import com.tinkerpop.blueprints.impls.tg.TinkerGraphFactory;
import com.tinkerpop.rexster.server.DefaultRexsterApplication;
import com.tinkerpop.rexster.server.RexsterApplication;
import com.tinkerpop.rexster.util.StatisticsHelper;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Variant;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public abstract class BaseTest {

    private static Logger logger = Logger.getLogger(BaseTest.class.getName());
    protected static final String graphName = "graph";

    protected Mockery mockery = new JUnit4Mockery();
    protected StatisticsHelper sh = new StatisticsHelper();

    protected Graph toyGraph;
    protected Graph emptyGraph;
    protected RexsterApplication raToyGraph;
    protected RexsterApplication raEmptyGraph;

    @Before
    public void init() {
        this.mockery = new JUnit4Mockery();
        this.createDefaultGraphs(TinkerGraphFactory.createTinkerGraph(), new TinkerGraph());
    }

    @After
    public void tearDown() {
        this.toyGraph.shutdown();
        this.emptyGraph.shutdown();
    }

    public static void printPerformance(String name, Integer events, String eventName, double timeInMilliseconds) {
        if (null != events)
            logger.info(name + ": " + events + " " + eventName + " in " + timeInMilliseconds + "ms");
        else
            logger.info(name + ": " + eventName + " in " + timeInMilliseconds + "ms");
    }

    protected ResourceHolder<PrefixResource> constructPrefixResource() {
        final SailGraph sg = new MemoryStoreSailGraph();
        SailGraphFactory.createTinkerGraph(sg);

        // have to reset with a sail graph for prefixes to work. empty graph is not used
        // in these tests so no need to reset.
        this.createDefaultGraphs(sg, this.emptyGraph);

        final UriInfo uri = this.mockery.mock(UriInfo.class);
        final URI requestUriPath = URI.create("http://localhost/graphs/graph/prefixes");
        final HttpServletRequest httpServletRequest = this.mockery.mock(HttpServletRequest.class);

        this.mockery.checking(new Expectations() {{
            allowing(httpServletRequest).getParameterMap();
            will(returnValue(new HashMap<String, String>()));
            allowing(httpServletRequest).getParameterNames();
            will(returnValue(new Hashtable().keys()));
            allowing(uri).getAbsolutePath();
            will(returnValue(requestUriPath));
        }});

        final PrefixResource resource = new PrefixResource(uri, httpServletRequest, this.raToyGraph);
        return new ResourceHolder<PrefixResource>(resource, null);
    }

    protected ResourceHolder<VertexResource> constructVertexResourceWithToyGraph() {
        return this.constructVertexResource(true, new HashMap<String, Object>(), MediaType.APPLICATION_JSON_TYPE);
    }

    protected ResourceHolder<VertexResource> constructVertexResourceWithEmptyGraph() {
        return this.constructVertexResource(false, new HashMap<String, Object>(), MediaType.APPLICATION_JSON_TYPE);
    }

    protected ResourceHolder<VertexResource> constructVertexResource(final boolean useToyGraph,
                                                                     final HashMap<String, Object> parameters){
        return this.constructVertexResource(useToyGraph, parameters, MediaType.APPLICATION_JSON_TYPE);
    }

    protected ResourceHolder<VertexResource> constructVertexResource(final boolean useToyGraph,
                                                                     final HashMap<String, Object> parameters,
                                                                     final MediaType mediaType) {
        final UriInfo uri = this.mockery.mock(UriInfo.class);
        final HttpServletRequest httpServletRequest = this.mockery.mock(HttpServletRequest.class);

        final Request request = this.mockery.mock(Request.class);
        final Variant variantJson = new Variant(mediaType, null, null);
        final URI requestUriPath = URI.create("http://localhost/graphs/graph/vertices");

        this.mockery.checking(new Expectations() {{
            allowing(httpServletRequest).getParameterMap();
            will(returnValue(parameters));
            allowing(httpServletRequest).getParameterNames();
            will(returnValue(new Hashtable(parameters).keys()));
            allowing(request).selectVariant(with(any(List.class)));
            will(returnValue(variantJson));
            allowing(uri).getAbsolutePath();
            will(returnValue(requestUriPath));
        }});

        final VertexResource resource = useToyGraph ? new VertexResource(uri, httpServletRequest, this.raToyGraph)
                : new VertexResource(uri, httpServletRequest, this.raEmptyGraph);
        return new ResourceHolder<VertexResource>(resource, request);
    }

    protected ResourceHolder<EdgeResource> constructEdgeResourceWithToyGraph() {
        return this.constructEdgeResource(true, new HashMap<String, Object>(), MediaType.APPLICATION_JSON_TYPE);
    }

    protected ResourceHolder<EdgeResource> constructEdgeResourceWithEmptyGraph() {
        return this.constructEdgeResource(false, new HashMap<String, Object>(), MediaType.APPLICATION_JSON_TYPE);
    }

    protected ResourceHolder<EdgeResource> constructEdgeResource(final boolean useToyGraph,
                                                                     final HashMap<String, Object> parameters){
        return this.constructEdgeResource(useToyGraph, parameters, MediaType.APPLICATION_JSON_TYPE);
    }

    protected ResourceHolder<EdgeResource> constructEdgeResource(final boolean useToyGraph,
                                                                     final HashMap<String, Object> parameters,
                                                                     final MediaType mediaType) {
        final UriInfo uri = this.mockery.mock(UriInfo.class);
        final HttpServletRequest httpServletRequest = this.mockery.mock(HttpServletRequest.class);

        final Request request = this.mockery.mock(Request.class);
        final Variant variantJson = new Variant(mediaType, null, null);
        final URI requestUriPath = URI.create("http://localhost/graphs/graph/edges");

        this.mockery.checking(new Expectations() {{
            allowing(httpServletRequest).getParameterMap();
            will(returnValue(parameters));
            allowing(httpServletRequest).getParameterNames();
            will(returnValue(new Hashtable(parameters).keys()));
            allowing(request).selectVariant(with(any(List.class)));
            will(returnValue(variantJson));
            allowing(uri).getAbsolutePath();
            will(returnValue(requestUriPath));
        }});

        final EdgeResource resource = useToyGraph ? new EdgeResource(uri, httpServletRequest, this.raToyGraph)
                : new EdgeResource(uri, httpServletRequest, this.raEmptyGraph);
        return new ResourceHolder<EdgeResource>(resource, request);
    }


    protected ResourceHolder<KeyIndexResource> constructKeyIndexResourceWithToyGraph() {
        return this.constructKeyIndexResource(true, new HashMap<String, Object>(), MediaType.APPLICATION_JSON_TYPE);
    }

    protected ResourceHolder<KeyIndexResource> constructKeyIndexResourceWithEmptyGraph() {
        return this.constructKeyIndexResource(false, new HashMap<String, Object>(), MediaType.APPLICATION_JSON_TYPE);
    }

    protected ResourceHolder<KeyIndexResource> constructKeyIndexResource(final boolean useToyGraph,
                                                                       final HashMap<String, Object> parameters){
        return this.constructKeyIndexResource(useToyGraph, parameters, MediaType.APPLICATION_JSON_TYPE);
    }

    protected ResourceHolder<KeyIndexResource> constructKeyIndexResource(final boolean useToyGraph,
                                                                       final HashMap<String, Object> parameters,
                                                                       final MediaType mediaType) {
        // add key indices to the toy graph
        final KeyIndexableGraph keyIndexableGraph = (KeyIndexableGraph) this.toyGraph;
        keyIndexableGraph.createKeyIndex("name", Vertex.class);
        keyIndexableGraph.createKeyIndex("test", Vertex.class);
        keyIndexableGraph.createKeyIndex("weight", Edge.class);

        final UriInfo uri = this.mockery.mock(UriInfo.class);
        final HttpServletRequest httpServletRequest = this.mockery.mock(HttpServletRequest.class);

        final Request request = this.mockery.mock(Request.class);
        final Variant variantJson = new Variant(mediaType, null, null);
        final URI requestUriPath = URI.create("http://localhost/graphs/graph/vertices");

        this.mockery.checking(new Expectations() {{
            allowing(httpServletRequest).getParameterMap();
            will(returnValue(parameters));
            allowing(httpServletRequest).getParameterNames();
            will(returnValue(new Hashtable(parameters).keys()));
            allowing(request).selectVariant(with(any(List.class)));
            will(returnValue(variantJson));
            allowing(uri).getAbsolutePath();
            will(returnValue(requestUriPath));
        }});

        final KeyIndexResource resource = useToyGraph ? new KeyIndexResource(uri, httpServletRequest, this.raToyGraph)
                : new KeyIndexResource(uri, httpServletRequest, this.raEmptyGraph);
        return new ResourceHolder<KeyIndexResource>(resource, request);
    }

    protected ResourceHolder<IndexResource> constructIndexResourceWithToyGraph() {
        return this.constructIndexResource(true, new HashMap<String, Object>(), MediaType.APPLICATION_JSON_TYPE);
    }

    protected ResourceHolder<IndexResource> constructIndexResourceWithEmptyGraph() {
        return this.constructIndexResource(false, new HashMap<String, Object>(), MediaType.APPLICATION_JSON_TYPE);
    }

    protected ResourceHolder<IndexResource> constructIndexResource(final boolean useToyGraph,
                                                                     final HashMap<String, Object> parameters){
        return this.constructIndexResource(useToyGraph, parameters, MediaType.APPLICATION_JSON_TYPE);
    }

    protected ResourceHolder<IndexResource> constructIndexResource(final boolean useToyGraph,
                                                                     final HashMap<String, Object> parameters,
                                                                     final MediaType mediaType) {
        final IndexableGraph indexableGraph = (IndexableGraph) this.toyGraph;
        final Index<Vertex> basicNameIndex = indexableGraph.createIndex("index-name-0", Vertex.class);
        indexableGraph.createIndex("index-name-1", Edge.class);
        indexableGraph.createIndex("index-name-2", Vertex.class);
        indexableGraph.createIndex("index-name-3", Vertex.class);
        indexableGraph.createIndex("index-name-4", Vertex.class);
        final Index<Edge> edgeIndex = indexableGraph.createIndex("index-name-5", Edge.class);
        indexableGraph.createIndex("index-name-6", Vertex.class);
        indexableGraph.createIndex("index-name-7", Vertex.class);
        indexableGraph.createIndex("index-name-8", Vertex.class);
        final Index<Vertex> madeUpIndex = indexableGraph.createIndex("index-name-9", Vertex.class);

        basicNameIndex.put("name", "marko", this.toyGraph.getVertex(1));
        basicNameIndex.put("name", "vadas", this.toyGraph.getVertex(2));
        basicNameIndex.put("name", "lop", this.toyGraph.getVertex(3));
        basicNameIndex.put("name", "josh", this.toyGraph.getVertex(4));
        basicNameIndex.put("name", "ripple", this.toyGraph.getVertex(5));
        basicNameIndex.put("name", "peter", this.toyGraph.getVertex(6));

        madeUpIndex.put("field", "X", this.toyGraph.getVertex(1));
        madeUpIndex.put("field", "X", this.toyGraph.getVertex(2));
        madeUpIndex.put("field", "Y", this.toyGraph.getVertex(3));
        madeUpIndex.put("field", "X", this.toyGraph.getVertex(4));
        madeUpIndex.put("field", "Y", this.toyGraph.getVertex(5));
        madeUpIndex.put("field", "X", this.toyGraph.getVertex(6));

        edgeIndex.put("weight", 0.4, this.toyGraph.getEdge(9));
        edgeIndex.put("weight", 0.2, this.toyGraph.getEdge(12));
        edgeIndex.put("weight", 0.4, this.toyGraph.getEdge(11));
        edgeIndex.put("weight", 1.0, this.toyGraph.getEdge(8));
        edgeIndex.put("weight", 0.5, this.toyGraph.getEdge(7));
        edgeIndex.put("weight", 1.0, this.toyGraph.getEdge(10));

        final UriInfo uri = this.mockery.mock(UriInfo.class);
        final HttpServletRequest httpServletRequest = this.mockery.mock(HttpServletRequest.class);

        final Request request = this.mockery.mock(Request.class);
        final Variant variantJson = new Variant(mediaType, null, null);
        final URI requestUriPath = URI.create("http://localhost/graphs/graph/indices");

        this.mockery.checking(new Expectations() {{
            allowing(httpServletRequest).getParameterMap();
            will(returnValue(parameters));
            allowing(httpServletRequest).getParameterNames();
            will(returnValue(new Hashtable(parameters).keys()));
            allowing(request).selectVariant(with(any(List.class)));
            will(returnValue(variantJson));
            allowing(uri).getAbsolutePath();
            will(returnValue(requestUriPath));
        }});

        final IndexResource resource = useToyGraph ? new IndexResource(uri, httpServletRequest, this.raToyGraph)
                : new IndexResource(uri, httpServletRequest, this.raEmptyGraph);
        return new ResourceHolder<IndexResource>(resource, request);
    }

    protected ResourceHolder<GraphResource> constructGraphResourceWithToyGraph() {
        return constructGraphResource(true, new HashMap<String, Object>(), MediaType.APPLICATION_JSON_TYPE);
    }

    protected ResourceHolder<GraphResource> constructGraphResource(final boolean useToyGraph,
                                                                   final HashMap<String, Object> parameters,
                                                                   final MediaType mediaType) {
        final UriInfo uri = this.mockery.mock(UriInfo.class);
        final HttpServletRequest httpServletRequest = this.mockery.mock(HttpServletRequest.class);

        final Request request = this.mockery.mock(Request.class);
        final Variant variantJson = new Variant(mediaType, null, null);
        final URI requestUriPath = URI.create("http://localhost/graphs/graph");

        this.mockery.checking(new Expectations() {{
            allowing(httpServletRequest).getParameterMap();
            will(returnValue(parameters));
            allowing(httpServletRequest).getParameterNames();
            will(returnValue(new Hashtable(parameters).keys()));
            allowing(request).selectVariant(with(any(List.class)));
            will(returnValue(variantJson));
            allowing(uri).getAbsolutePath();
            will(returnValue(requestUriPath));
        }});

        final GraphResource resource = useToyGraph ? new GraphResource(uri, httpServletRequest, this.raToyGraph)
                : new GraphResource(uri, httpServletRequest, this.raEmptyGraph);
        return new ResourceHolder<GraphResource>(resource, request);
    }

    protected void assertFoundElementsInResults(final JSONArray jsonResultArray, final String elementType,
                                                final String... expectedIds) {
        Assert.assertNotNull(jsonResultArray);
        Assert.assertEquals(expectedIds.length, jsonResultArray.length());

        final List<String> foundIds = new ArrayList<String>();
        for (int ix = 0; ix < jsonResultArray.length(); ix++) {
            final JSONObject jsonResult = jsonResultArray.optJSONObject(ix);
            Assert.assertNotNull(jsonResult);
            Assert.assertEquals(elementType, jsonResult.optString(Tokens._TYPE));
            Assert.assertTrue(jsonResult.has(Tokens._ID));
            foundIds.add(jsonResult.optString(Tokens._ID));
        }

        for (String expectedId : expectedIds) {
            Assert.assertTrue(foundIds.contains(expectedId));
        }
    }

    private void createDefaultGraphs(final Graph toy, final Graph empty) {
        this.toyGraph = toy;
        this.emptyGraph = empty;

        this.raToyGraph = new DefaultRexsterApplication(graphName, toy);
        this.raEmptyGraph = new DefaultRexsterApplication(graphName, empty);

        final List<String> namespaces = new ArrayList<String>();
        namespaces.add("*:*");
        this.raToyGraph.getApplicationGraph(graphName).loadAllowableExtensions(namespaces);
        this.raEmptyGraph.getApplicationGraph(graphName).loadAllowableExtensions(namespaces);
    }
}
