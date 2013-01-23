package com.tinkerpop.rexster.kibbles.batch;

import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Index;
import com.tinkerpop.blueprints.IndexableGraph;
import com.tinkerpop.blueprints.KeyIndexableGraph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.tg.TinkerGraphFactory;
import com.tinkerpop.rexster.RexsterApplicationGraph;
import com.tinkerpop.rexster.RexsterResourceContext;
import com.tinkerpop.rexster.Tokens;
import com.tinkerpop.rexster.extension.ExtensionResponse;
import junit.framework.Assert;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

public class BatchExtensionTest {

    private Graph graph;
    private IndexableGraph indexGraph;
    private KeyIndexableGraph keyIndexGraph;
    private RexsterResourceContext ctx;

    @Before
    public void beforeTest() {
        // tests for batch extension use tinkergraph which is non-transactional.  batch extension uses
        // autocommit option which relies on rexster to handle commits so even tests that used a
        // transactional graph will need to take that into account.
        this.graph = TinkerGraphFactory.createTinkerGraph();

        this.indexGraph = TinkerGraphFactory.createTinkerGraph();

        Index ageIndex = this.indexGraph.createIndex("age", Vertex.class);
        Vertex v1 = this.indexGraph.getVertex(1);
        Vertex v2 = this.indexGraph.getVertex(2);
        ageIndex.put("age", v1.getProperty("age"), v1);
        ageIndex.put("age", v2.getProperty("age"), v2);

        Index weightIndex = this.indexGraph.createIndex("weight", Edge.class);
        Edge e7 = this.indexGraph.getEdge(7);
        Edge e12 = this.indexGraph.getEdge(12);
        weightIndex.put("weight", e7.getProperty("weight"), e7);
        weightIndex.put("weight", e12.getProperty("weight"), e12);

        this.keyIndexGraph = TinkerGraphFactory.createTinkerGraph();
        this.keyIndexGraph.createKeyIndex("name", Vertex.class);
        this.keyIndexGraph.createKeyIndex("weight", Edge.class);
    }

    @Test
    public void getVerticesNoValuesInvalid() throws Exception {
        BatchExtension batchExtension = new BatchExtension();

        JSONObject requestObject = new JSONObject();

        this.ctx = new RexsterResourceContext(null, null, null, requestObject, null, null, null);

        ExtensionResponse response = batchExtension.getVertices(this.ctx, graph);

        Assert.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getJerseyResponse().getStatus());

    }

    @Test
    public void getVerticesDefaultTypeValid() throws Exception {
        BatchExtension batchExtension = new BatchExtension();

        JSONObject requestObject = new JSONObject();
        JSONArray values = new JSONArray();
        values.put(1);
        values.put(2);
        values.put(100000);
        requestObject.put("values", values);

        this.ctx = new RexsterResourceContext(null, null, null, requestObject, null, null, null);

        ExtensionResponse response = batchExtension.getVertices(this.ctx, graph);

        Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getJerseyResponse().getStatus());
        JSONObject entity = (JSONObject) response.getJerseyResponse().getEntity();

        Assert.assertNotNull(entity);
        Assert.assertTrue(entity.has(Tokens.RESULTS));

        JSONArray results = entity.optJSONArray(Tokens.RESULTS);

        Assert.assertNotNull(results);
        Assert.assertEquals(2, results.length());

    }

    @Test
    public void getVerticesTypeIdValid() throws Exception {
        BatchExtension batchExtension = new BatchExtension();

        JSONObject requestObject = new JSONObject();
        JSONArray values = new JSONArray();
        values.put(1);
        values.put(2);
        values.put(100000);
        requestObject.put("values", values);
        requestObject.put("type", "id");

        this.ctx = new RexsterResourceContext(null, null, null, requestObject, null, null, null);

        ExtensionResponse response = batchExtension.getVertices(this.ctx, graph);

        Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getJerseyResponse().getStatus());
        JSONObject entity = (JSONObject) response.getJerseyResponse().getEntity();

        Assert.assertNotNull(entity);
        Assert.assertTrue(entity.has(Tokens.RESULTS));

        JSONArray results = entity.optJSONArray(Tokens.RESULTS);

        Assert.assertNotNull(results);
        Assert.assertEquals(2, results.length());

    }

    @Test
    public void getVerticesTypeIndexNoKeyInvalid() throws Exception {
        BatchExtension batchExtension = new BatchExtension();

        JSONObject requestObject = new JSONObject();
        JSONArray values = new JSONArray();
        values.put("(i,27)");
        values.put("(i,29)");
        values.put("(i,50)");
        requestObject.put("values", values);
        requestObject.put("type", "index");

        this.ctx = new RexsterResourceContext(null, null, null, requestObject, null, null, null);

        ExtensionResponse response = batchExtension.getVertices(this.ctx, indexGraph);

        Assert.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getJerseyResponse().getStatus());

    }

    @Test
    public void getVerticesTypeIndexValid() throws Exception {
        BatchExtension batchExtension = new BatchExtension();

        JSONObject requestObject = new JSONObject();
        JSONArray values = new JSONArray();
        values.put("(i,27)");
        values.put("(i,29)");
        values.put("(i,50)");
        requestObject.put("values", values);
        requestObject.put("type", "index");
        requestObject.put("key", "age");

        this.ctx = new RexsterResourceContext(null, null, null, requestObject, null, null, null);

        ExtensionResponse response = batchExtension.getVertices(this.ctx, indexGraph);

        Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getJerseyResponse().getStatus());
        JSONObject entity = (JSONObject) response.getJerseyResponse().getEntity();

        Assert.assertNotNull(entity);
        Assert.assertTrue(entity.has(Tokens.RESULTS));

        JSONArray results = entity.optJSONArray(Tokens.RESULTS);

        Assert.assertNotNull(results);
        Assert.assertEquals(2, results.length());

    }

    @Test
    public void getVerticesTypeKeyIndexNoKeyInvalid() throws Exception {
        BatchExtension batchExtension = new BatchExtension();

        JSONObject requestObject = new JSONObject();
        JSONArray values = new JSONArray();
        values.put("marko");
        values.put("peter");
        values.put("nobody");
        requestObject.put("values", values);
        requestObject.put("type", "keyindex");

        this.ctx = new RexsterResourceContext(null, null, null, requestObject, null, null, null);

        ExtensionResponse response = batchExtension.getVertices(this.ctx, keyIndexGraph);

        Assert.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getJerseyResponse().getStatus());

    }

    @Test
    public void getVerticesTypeKeyIndexValid() throws Exception {
        BatchExtension batchExtension = new BatchExtension();

        JSONObject requestObject = new JSONObject();
        JSONArray values = new JSONArray();
        values.put("marko");
        values.put("peter");
        values.put("nobody");
        requestObject.put("values", values);
        requestObject.put("type", "keyindex");
        requestObject.put("key", "name");

        this.ctx = new RexsterResourceContext(null, null, null, requestObject, null, null, null);

        ExtensionResponse response = batchExtension.getVertices(this.ctx, keyIndexGraph);

        Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getJerseyResponse().getStatus());
        JSONObject entity = (JSONObject) response.getJerseyResponse().getEntity();

        Assert.assertNotNull(entity);
        Assert.assertTrue(entity.has(Tokens.RESULTS));

        JSONArray results = entity.optJSONArray(Tokens.RESULTS);

        Assert.assertNotNull(results);
        Assert.assertEquals(2, results.length());

    }

    @Test
    public void getEdgesNoValuesInvalid() throws Exception {
        BatchExtension batchExtension = new BatchExtension();

        JSONObject requestObject = new JSONObject();

        this.ctx = new RexsterResourceContext(null, null, null, requestObject, null, null, null);

        ExtensionResponse response = batchExtension.getEdges(this.ctx, graph);

        Assert.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getJerseyResponse().getStatus());

    }

    @Test
    public void getEdgesDefaultTypeValid() throws Exception {
        BatchExtension batchExtension = new BatchExtension();

        JSONObject requestObject = new JSONObject();
        JSONArray values = new JSONArray();
        values.put(7);
        values.put(8);
        values.put(100000);
        requestObject.put("values", values);

        this.ctx = new RexsterResourceContext(null, null, null, requestObject, null, null, null);

        ExtensionResponse response = batchExtension.getEdges(this.ctx, graph);

        Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getJerseyResponse().getStatus());
        JSONObject entity = (JSONObject) response.getJerseyResponse().getEntity();

        Assert.assertNotNull(entity);
        Assert.assertTrue(entity.has(Tokens.RESULTS));

        JSONArray results = entity.optJSONArray(Tokens.RESULTS);

        Assert.assertNotNull(results);
        Assert.assertEquals(2, results.length());

    }

    @Test
    public void getEdgesTypeIdValid() throws Exception {
        BatchExtension batchExtension = new BatchExtension();

        JSONObject requestObject = new JSONObject();
        JSONArray values = new JSONArray();
        values.put(7);
        values.put(8);
        values.put(100000);
        requestObject.put("values", values);
        requestObject.put("type", "id");

        this.ctx = new RexsterResourceContext(null, null, null, requestObject, null, null, null);

        ExtensionResponse response = batchExtension.getEdges(this.ctx, graph);

        Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getJerseyResponse().getStatus());
        JSONObject entity = (JSONObject) response.getJerseyResponse().getEntity();

        Assert.assertNotNull(entity);
        Assert.assertTrue(entity.has(Tokens.RESULTS));

        JSONArray results = entity.optJSONArray(Tokens.RESULTS);

        Assert.assertNotNull(results);
        Assert.assertEquals(2, results.length());

    }

    @Test
    public void getEdgesTypeIndexNoKeyInvalid() throws Exception {
        BatchExtension batchExtension = new BatchExtension();

        JSONObject requestObject = new JSONObject();
        JSONArray values = new JSONArray();
        values.put("(f,0.2)");
        values.put("(f,0.5)");
        values.put("(f,0.7)");
        requestObject.put("values", values);
        requestObject.put("type", "index");

        this.ctx = new RexsterResourceContext(null, null, null, requestObject, null, null, null);

        ExtensionResponse response = batchExtension.getEdges(this.ctx, indexGraph);

        Assert.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getJerseyResponse().getStatus());

    }

    @Test
    public void getEdgesTypeIndexValid() throws Exception {
        BatchExtension batchExtension = new BatchExtension();

        JSONObject requestObject = new JSONObject();
        JSONArray values = new JSONArray();
        values.put("(f,0.2)");
        values.put("(f,0.5)");
        values.put("(f,0.7)");
        requestObject.put("values", values);
        requestObject.put("type", "index");
        requestObject.put("key", "weight");

        this.ctx = new RexsterResourceContext(null, null, null, requestObject, null, null, null);

        ExtensionResponse response = batchExtension.getEdges(this.ctx, indexGraph);

        Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getJerseyResponse().getStatus());
        JSONObject entity = (JSONObject) response.getJerseyResponse().getEntity();

        Assert.assertNotNull(entity);
        Assert.assertTrue(entity.has(Tokens.RESULTS));

        JSONArray results = entity.optJSONArray(Tokens.RESULTS);

        Assert.assertNotNull(results);
        Assert.assertEquals(2, results.length());

    }

    @Test
    public void getEdgesTypeKeyIndexNoKeyInvalid() throws Exception {
        BatchExtension batchExtension = new BatchExtension();

        JSONObject requestObject = new JSONObject();
        JSONArray values = new JSONArray();
        values.put("(f,0.2)");
        values.put("(f,0.5)");
        values.put("(f,0.7)");
        requestObject.put("values", values);
        requestObject.put("type", "keyindex");

        this.ctx = new RexsterResourceContext(null, null, null, requestObject, null, null, null);

        ExtensionResponse response = batchExtension.getEdges(this.ctx, keyIndexGraph);

        Assert.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getJerseyResponse().getStatus());

    }

    @Test
    public void getEdgesTypeKeyIndexValid() throws Exception {
        BatchExtension batchExtension = new BatchExtension();

        JSONObject requestObject = new JSONObject();
        JSONArray values = new JSONArray();
        values.put("(f,0.2)");
        values.put("(f,0.5)");
        values.put("(f,0.7)");
        requestObject.put("values", values);
        requestObject.put("type", "keyindex");
        requestObject.put("key", "weight");

        this.ctx = new RexsterResourceContext(null, null, null, requestObject, null, null, null);

        ExtensionResponse response = batchExtension.getEdges(this.ctx, keyIndexGraph);

        Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getJerseyResponse().getStatus());
        JSONObject entity = (JSONObject) response.getJerseyResponse().getEntity();

        Assert.assertNotNull(entity);
        Assert.assertTrue(entity.has(Tokens.RESULTS));

        JSONArray results = entity.optJSONArray(Tokens.RESULTS);

        Assert.assertNotNull(results);
        Assert.assertEquals(2, results.length());

    }

    @Test
    public void postTxValid() throws Exception {
        BatchExtension batchExtension = new BatchExtension();

        JSONObject requestObject = new JSONObject();
        JSONArray txList = new JSONArray();

        Map<String, Object> newVertexA = new HashMap<String, Object>();
        newVertexA.put("_id", 100);
        newVertexA.put("_type", "vertex");
        newVertexA.put("_action", "create");
        newVertexA.put("k1", "v1");
        txList.put(new JSONObject(newVertexA));

        Map<String, Object> newVertexB = new HashMap<String, Object>();
        newVertexB.put("_id", 101);
        newVertexB.put("_type", "vertex");
        newVertexB.put("_action", "create");
        newVertexB.put("k1", "v2");
        txList.put(new JSONObject(newVertexB));

        Map<String, Object> newEdgeC = new HashMap<String, Object>();
        newEdgeC.put("_id", 1000);
        newEdgeC.put("_type", "edge");
        newEdgeC.put("_action", "create");
        newEdgeC.put("_outV", 100);
        newEdgeC.put("_inV", 101);
        newEdgeC.put("_label", "buddy");
        newEdgeC.put("k2", "v3");
        txList.put(new JSONObject(newEdgeC));

        Map<String, Object> existingVertex1 = new HashMap<String, Object>();
        existingVertex1.put("_id", 1);
        existingVertex1.put("_type", "vertex");
        existingVertex1.put("_action", "update");
        existingVertex1.put("k1", "v4");
        existingVertex1.put("name", "okram");
        txList.put(new JSONObject(existingVertex1));

        Map<String, Object> deleteVertexProperty4 = new HashMap<String, Object>();
        JSONArray keysToRemove = new JSONArray();
        keysToRemove.put("age");
        deleteVertexProperty4.put("_id", 4);
        deleteVertexProperty4.put("_type", "vertex");
        deleteVertexProperty4.put("_action", "delete");
        deleteVertexProperty4.put("_keys", keysToRemove);
        txList.put(new JSONObject(deleteVertexProperty4));

        Map<String, Object> deleteVertex3 = new HashMap<String, Object>();
        deleteVertex3.put("_id", 3);
        deleteVertex3.put("_type", "vertex");
        deleteVertex3.put("_action", "delete");
        txList.put(new JSONObject(deleteVertex3));

        requestObject.put("tx", txList);

        RexsterApplicationGraph rag = new RexsterApplicationGraph("graph", this.graph);

        this.ctx = new RexsterResourceContext(rag, null, null, requestObject, null, null, null);

        ExtensionResponse response = batchExtension.postTx(this.ctx, this.graph, rag);

        Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getJerseyResponse().getStatus());
        JSONObject entity = (JSONObject) response.getJerseyResponse().getEntity();

        Assert.assertNotNull(entity);
        Assert.assertTrue(entity.has("txProcessed"));

        Assert.assertEquals(6, entity.optInt("txProcessed"));

        Vertex v100 = graph.getVertex(100);
        Assert.assertNotNull(v100);
        Assert.assertEquals("v1", v100.getProperty("k1"));

        Vertex v101 = graph.getVertex(101);
        Assert.assertNotNull(v101);
        Assert.assertEquals("v2", v101.getProperty("k1"));

        Edge e1000 = graph.getEdge(1000);
        Assert.assertNotNull(e1000);
        Assert.assertEquals("v3", e1000.getProperty("k2"));
        Assert.assertEquals("buddy", e1000.getLabel());

        Vertex v1 = graph.getVertex(1);
        Assert.assertNotNull(v1);
        Assert.assertEquals("v4", v1.getProperty("k1"));
        Assert.assertEquals("okram", v1.getProperty("name"));

        Vertex v4 = graph.getVertex(4);
        Assert.assertNotNull(v4);
        Assert.assertFalse(v4.getPropertyKeys().contains("age"));

        Vertex v3 = graph.getVertex(3);
        Assert.assertNull(v3);

    }
}