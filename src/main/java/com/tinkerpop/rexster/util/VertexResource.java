package com.tinkerpop.rexster.util;

import com.tinkerpop.blueprints.pgm.Edge;
import com.tinkerpop.blueprints.pgm.Element;
import com.tinkerpop.blueprints.pgm.Graph;
import com.tinkerpop.blueprints.pgm.Vertex;
import com.tinkerpop.rexster.traversals.ElementJSONObject;
import org.json.simple.JSONArray;
import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Post;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class VertexResource extends BaseResource {

    @Get
    public Representation getResource() {
        Map<String, String> queryParameters = createQueryMap(this.getRequest().getResourceRef().getQueryAsForm());
        this.buildRequestObject(queryParameters);
        String id = (String) getRequest().getAttributes().get("id");
        String direction = (String) getRequest().getAttributes().get("direction");

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
        Object id = getRequest().getAttributes().get("id");
        String direction = (String) getRequest().getAttributes().get("direction");
        String id2 = (String) getRequest().getAttributes().get("id2");

        if (null == direction) {
            Vertex vertex = graph.getVertex(id);
            if (null == vertex) {
                vertex = graph.addVertex(id);
            }
            for (String key : (Set<String>) this.requestObject.keySet()) {
                if (!key.startsWith(UNDERSCORE))
                    vertex.setProperty(key, this.requestObject.get(key));
            }
            this.resultObject.put(RESULT, new ElementJSONObject(vertex, (List) this.requestObject.get(RETURN_KEYS)));
        } else if (null != id && null == id2) {
            Vertex vertexA = graph.getVertex(id);
            Vertex vertexB = graph.getVertex(this.requestObject.get("_target"));
            String label = (String) this.requestObject.get("_label");
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
            this.resultObject.put(RESULT, new ElementJSONObject(edge, (List) this.requestObject.get(RETURN_KEYS)));

        } else {
            Edge edge = this.getVertexEdge(id, direction, id2);
            if (null != edge) {
                for (String key : (Set<String>) this.requestObject.keySet()) {
                    if (!key.startsWith(UNDERSCORE))
                        edge.setProperty(key, this.requestObject.get(key));
                }
                this.resultObject.put(RESULT, new ElementJSONObject(edge, (List) this.requestObject.get(RETURN_KEYS)));
            }
        }
        this.resultObject.put(QUERY_TIME, sh.stopWatch());
        return new StringRepresentation(this.resultObject.toJSONString(), MediaType.APPLICATION_JSON);
    }

    @Delete
    public Representation deleteResource() {

        String id = (String) getRequest().getAttributes().get("id");
        String direction = (String) getRequest().getAttributes().get("direction");
        String id2 = (String) getRequest().getAttributes().get("id2");

        Graph graph = this.getRexsterApplication().getGraph();
        if (null == direction) {
            Vertex vertex = graph.getVertex(id);
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
        if (direction.equals(OUT_E)) {
            for (Edge temp : vertex.getOutEdges()) {
                System.out.println(temp.getId());
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
        return null;
    }

    protected void getVertexEdges(Object id, String direction) {
        Integer start = this.getStartOffset();
        if (null == start)
            start = 0;
        Integer end = this.getEndOffset();
        if (null == end)
            end = Integer.MAX_VALUE;

        int counter = 0;
        Vertex vertex = this.getRexsterApplication().getGraph().getVertex(id);
        JSONArray edgeArray = new JSONArray();
        if (direction.equals(OUT_E)) {
            for (Edge edge : vertex.getOutEdges()) {
                if (counter >= start && counter < end) {
                    edgeArray.add(new ElementJSONObject(edge, (List) this.requestObject.get(RETURN_KEYS)));
                }
                counter++;
            }
        } else if (direction.equals(IN_E)) {
            for (Edge edge : vertex.getInEdges()) {
                if (counter >= start && counter < end) {
                    edgeArray.add(new ElementJSONObject(edge, (List) this.requestObject.get(RETURN_KEYS)));
                }
                counter++;
            }
        } else {
            for (Edge edge : vertex.getInEdges()) {
                if (counter >= start && counter < end) {
                    edgeArray.add(new ElementJSONObject(edge, (List) this.requestObject.get(RETURN_KEYS)));
                }
                counter++;
            }
            for (Edge edge : vertex.getOutEdges()) {
                if (counter >= start && counter < end) {
                    edgeArray.add(new ElementJSONObject(edge, (List) this.requestObject.get(RETURN_KEYS)));
                }
                counter++;
            }
        }

        this.resultObject.put(RESULT, edgeArray);
        this.resultObject.put(TOTAL_SIZE, counter);
    }

    protected void getSingleVertex(Object id) {
        Vertex vertex = this.getRexsterApplication().getGraph().getVertex(id);
        this.resultObject.put(RESULT, new ElementJSONObject(vertex, (List) this.requestObject.get(RETURN_KEYS)));
    }


    protected void getVertices() {
        Integer start = this.getStartOffset();
        if (null == start)
            start = 0;
        Integer end = this.getEndOffset();
        if (null == end)
            end = Integer.MAX_VALUE;

        int counter = 0;
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
                    vertexArray.add(new ElementJSONObject(element, (List) this.requestObject.get(RETURN_KEYS)));
                }
                counter++;
            }
        }
        this.resultObject.put(RESULT, vertexArray);
        this.resultObject.put(TOTAL_SIZE, counter);
    }
}
