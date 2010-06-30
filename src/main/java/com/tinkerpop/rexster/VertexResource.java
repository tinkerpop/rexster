package com.tinkerpop.rexster;

import com.tinkerpop.blueprints.pgm.Edge;
import com.tinkerpop.blueprints.pgm.Element;
import com.tinkerpop.blueprints.pgm.Graph;
import com.tinkerpop.blueprints.pgm.Vertex;
import com.tinkerpop.rexster.traversals.ElementJSONObject;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Post;

import java.util.Map;
import java.util.Set;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class VertexResource extends BaseResource {

    protected static final String ID = "id";
    protected static final String ID2 = "id2";
    protected static final String DIRECTION = "direction";
    protected static final String TARGET = "_target";

    @Get
    public Representation getResource() {
        Map<String, String> queryParameters = createQueryMap(this.getRequest().getResourceRef().getQueryAsForm());
        this.buildRequestObject(queryParameters);
        String id = (String) getRequest().getAttributes().get(ID);
        String direction = (String) getRequest().getAttributes().get(DIRECTION);

        if (null == direction && null == id)
            getVertices();
        else if (null == direction)
            getSingleVertex(id);
        else
            getVertexEdges(id, direction);

        this.resultObject.put(QUERY_TIME, sh.stopWatch());
        return new StringRepresentation(this.resultObject.toJSONString(), MediaType.APPLICATION_JSON);
    }

    @Post
    public Representation postResource() {
        Map<String, String> queryParameters = createQueryMap(this.getRequest().getResourceRef().getQueryAsForm());
        this.buildRequestObject(queryParameters);

        Graph graph = this.getRexsterApplication().getGraph();
        Object id = getRequest().getAttributes().get(ID);
        String direction = (String) getRequest().getAttributes().get(DIRECTION);
        String id2 = (String) getRequest().getAttributes().get(ID2);

        if (null == direction) {
            // UPDATE VERTEX PROPERTIES OR CREATE NEW VERTEX
            Vertex vertex = graph.getVertex(id);
            if (null == vertex) {
                vertex = graph.addVertex(id);
            }
            for (String key : (Set<String>) this.requestObject.keySet()) {
                if (!key.startsWith(UNDERSCORE))
                    vertex.setProperty(key, this.requestObject.get(key));
            }
            this.resultObject.put(RESULT, new ElementJSONObject(vertex, this.getReturnKeys()));
        } else if (null != id && null == id2) {
            // CREATE A NEW EDGE BETWEEN TWO VERTICES
            Vertex vertexA = graph.getVertex(id);
            Vertex vertexB = graph.getVertex(this.requestObject.get(TARGET));
            String label = (String) this.requestObject.get(ElementJSONObject.LABEL);
            Edge edge;
            if (direction.equals(OUT_E)) {
                edge = graph.addEdge(null, vertexA, vertexB, label);
            } else {
                edge = graph.addEdge(null, vertexB, vertexA, label);
            }
            for (String key : (Set<String>) this.requestObject.keySet()) {
                if (!key.startsWith(UNDERSCORE))
                    edge.setProperty(key, this.requestObject.get(key));
            }
            this.resultObject.put(RESULT, new ElementJSONObject(edge, this.getReturnKeys()));

        } else {
            // UPDATE THE PROPERTIES OF AN EDGE
            Edge edge = this.getVertexEdge(id, direction, id2);
            if (null != edge) {
                for (String key : (Set<String>) this.requestObject.keySet()) {
                    if (!key.startsWith(UNDERSCORE))
                        edge.setProperty(key, this.requestObject.get(key));
                }
                this.resultObject.put(RESULT, new ElementJSONObject(edge, this.getReturnKeys()));
            }
        }
        this.resultObject.put(QUERY_TIME, sh.stopWatch());
        return new StringRepresentation(this.resultObject.toJSONString(), MediaType.APPLICATION_JSON);
    }

    @Delete
    public Representation deleteResource() {
        // TODO: delete individual properties
        String id = (String) getRequest().getAttributes().get(ID);
        String direction = (String) getRequest().getAttributes().get(DIRECTION);
        String id2 = (String) getRequest().getAttributes().get(ID2);

        Graph graph = this.getRexsterApplication().getGraph();
        if (null == direction) {
            Vertex vertex = graph.getVertex(id);
            if (null != vertex)
                graph.removeVertex(vertex);
        } else {
            Edge edge = getVertexEdge(id, direction, id2);
            if (null != edge)
                graph.removeEdge(edge);
        }
        this.resultObject.put(QUERY_TIME, sh.stopWatch());
        return new StringRepresentation(this.resultObject.toJSONString(), MediaType.APPLICATION_JSON);

    }

    ///////////////////////

    protected Edge getVertexEdge(Object vertexId, String direction, Object edgeId) {
        Graph graph = this.getRexsterApplication().getGraph();
        Vertex vertex = graph.getVertex(vertexId);
        if (null != vertex) {
            if (direction.equals(OUT_E)) {
                for (Edge temp : vertex.getOutEdges()) {
                    if (temp.getId().toString().equals(edgeId)) {
                        return temp;
                    }
                }
            } else {
                for (Edge temp : vertex.getInEdges()) {
                    if (temp.getId().toString().equals(edgeId)) {
                        return temp;
                    }
                }
            }
        }
        return null;
    }

    protected void getVertexEdges(Object vertexId, String direction) {
        try {
            Long start = this.getStartOffset();
            if (null == start)
                start = 0l;
            Long end = this.getEndOffset();
            if (null == end)
                end = Long.MAX_VALUE;

            long counter = 0l;
            Vertex vertex = this.getRexsterApplication().getGraph().getVertex(vertexId);
            JSONArray edgeArray = new JSONArray();

            if (null != vertex) {
                JSONObject tempRequest = this.getNonRexsterRequestObject();
                if (direction.equals(OUT_E) || direction.equals(BOTH_E)) {
                    for (Edge edge : vertex.getOutEdges()) {
                        if (this.hasPropertyValues(edge, tempRequest)) {
                            if (counter >= start && counter < end) {
                                edgeArray.add(new ElementJSONObject(edge, this.getReturnKeys()));
                            }
                            counter++;
                        }
                    }
                }
                if (direction.equals(IN_E) || direction.equals(BOTH_E)) {
                    for (Edge edge : vertex.getInEdges()) {
                        if (this.hasPropertyValues(edge, tempRequest)) {
                            if (counter >= start && counter < end) {
                                edgeArray.add(new ElementJSONObject(edge, this.getReturnKeys()));
                            }
                            counter++;
                        }
                    }
                }
            }

            this.resultObject.put(RESULT, edgeArray);
            this.resultObject.put(TOTAL_SIZE, counter);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void getSingleVertex(Object id) {
        Vertex vertex = this.getRexsterApplication().getGraph().getVertex(id);
        if (null != vertex) {
            this.resultObject.put(RESULT, new ElementJSONObject(vertex, this.getReturnKeys()));
        } else {
            this.resultObject.put(RESULT, null);
        }
    }


    protected void getVertices() {
        Long start = this.getStartOffset();
        if (null == start)
            start = 0l;
        Long end = this.getEndOffset();
        if (null == end)
            end = Long.MAX_VALUE;

        long counter = 0l;
        JSONArray vertexArray = new JSONArray();
        String key = null;
        for (String tempKey : (Set<String>) this.requestObject.keySet()) {
            if (!tempKey.equals(OFFSET) && !tempKey.equals(RETURN_KEYS)) {
                key = tempKey;
                break;
            }
        }
        Iterable<? extends Element> itty;
        if (null != key) {
            itty = this.getRexsterApplication().getGraph().getIndex().get(key, this.requestObject.get(key));
        } else {
            itty = this.getRexsterApplication().getGraph().getVertices();
        }

        if (null != itty) {
            for (Element element : itty) {
                if (counter >= start && counter < end) {
                    vertexArray.add(new ElementJSONObject(element, this.getReturnKeys()));
                }
                counter++;
            }
        }
        this.resultObject.put(RESULT, vertexArray);
        this.resultObject.put(TOTAL_SIZE, counter);
    }

}
