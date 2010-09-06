package com.tinkerpop.rexster;

import com.tinkerpop.blueprints.pgm.Edge;
import com.tinkerpop.blueprints.pgm.Element;
import com.tinkerpop.blueprints.pgm.Graph;
import com.tinkerpop.blueprints.pgm.Vertex;
import com.tinkerpop.rexster.traversals.ElementJSONObject;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.restlet.data.Form;
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

    @Get
    public Representation getResource() {
        Map<String, String> queryParameters = createQueryMap(this.getRequest().getResourceRef().getQueryAsForm());
        this.buildRequestObject(queryParameters);
        String id = (String) getRequest().getAttributes().get(Tokens.ID);
        String direction = (String) getRequest().getAttributes().get(Tokens.DIRECTION);
        
    	String graphName = this.getRequest().getResourceRef().getSegments().get(0);
        
        if (null == direction && null == id)
            getVertices(graphName);
        else if (null == direction)
            getSingleVertex(graphName, id);
        else
            getVertexEdges(graphName, id, direction);

        this.resultObject.put(Tokens.QUERY_TIME, sh.stopWatch());
        return new StringRepresentation(this.resultObject.toJSONString(), MediaType.APPLICATION_JSON);
    }

    @Post
    public Representation postResource() {
        this.buildRequestObject(createQueryMap(this.getRequest().getResourceRef().getQueryAsForm()));
        
        String graphName = this.getRequest().getResourceRef().getSegments().get(0);
        final Graph graph = this.getRexsterApplication().getGraph(graphName);
        final Object id = getRequest().getAttributes().get(Tokens.ID);

        Vertex vertex = graph.getVertex(id);
        if (null == vertex) {
            vertex = graph.addVertex(id);
        }
        for (String key : (Set<String>) this.requestObject.keySet()) {
            if (!key.startsWith(Tokens.UNDERSCORE))
                vertex.setProperty(key, this.requestObject.get(key));
        }
        this.resultObject.put(Tokens.RESULTS, new ElementJSONObject(vertex, this.getReturnKeys()));
        this.resultObject.put(Tokens.QUERY_TIME, sh.stopWatch());
        return new StringRepresentation(this.resultObject.toJSONString(), MediaType.APPLICATION_JSON);
    }

    @Delete
    public Representation deleteResource() {
        // TODO: delete individual properties
        final String id = (String) getRequest().getAttributes().get(Tokens.ID);
        
        String graphName = this.getRequest().getResourceRef().getSegments().get(0);
        final Graph graph = this.getRexsterApplication().getGraph(graphName);
        final Vertex vertex = graph.getVertex(id);
        if (null != vertex)
            graph.removeVertex(vertex);

        this.resultObject.put(Tokens.QUERY_TIME, sh.stopWatch());
        return new StringRepresentation(this.resultObject.toJSONString(), MediaType.APPLICATION_JSON);

    }

    ///////////////////////

    protected void getVertexEdges(String graphName, Object vertexId, String direction) {
        try {
            Long start = this.getStartOffset();
            if (null == start)
                start = 0l;
            Long end = this.getEndOffset();
            if (null == end)
                end = Long.MAX_VALUE;

            long counter = 0l;
            Vertex vertex = this.getRexsterApplication().getGraph(graphName).getVertex(vertexId);
            JSONArray edgeArray = new JSONArray();

            if (null != vertex) {
                JSONObject tempRequest = this.getNonRexsterRequest();
                if (direction.equals(Tokens.OUT_E) || direction.equals(Tokens.BOTH_E)) {
                    for (Edge edge : vertex.getOutEdges()) {
                        if (this.hasPropertyValues(edge, tempRequest)) {
                            if (counter >= start && counter < end) {
                                edgeArray.add(new ElementJSONObject(edge, this.getReturnKeys()));
                            }
                            counter++;
                        }
                    }
                }
                if (direction.equals(Tokens.IN_E) || direction.equals(Tokens.BOTH_E)) {
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

            this.resultObject.put(Tokens.RESULTS, edgeArray);
            this.resultObject.put(Tokens.TOTAL_SIZE, counter);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void getSingleVertex(String graphName, Object id) {
        Vertex vertex = this.getRexsterApplication().getGraph(graphName).getVertex(id);
        if (null != vertex) {
            this.resultObject.put(Tokens.RESULTS, new ElementJSONObject(vertex, this.getReturnKeys()));
        } else {
            this.resultObject.put(Tokens.RESULTS, null);
        }
    }


    protected void getVertices(String graphName) {
        Long start = this.getStartOffset();
        if (null == start)
            start = 0l;
        Long end = this.getEndOffset();
        if (null == end)
            end = Long.MAX_VALUE;

        long counter = 0l;
        JSONArray vertexArray = new JSONArray();
        String key = null;
        for (String tempKey : (Set<String>) this.getNonRexsterRequest().keySet()) {
            key = tempKey;
            break;
        }
        Iterable<? extends Element> itty;
        if (null != key) {
            itty = this.getRexsterApplication().getGraph(graphName).getIndex().get(key, this.requestObject.get(key));
        } else {
            itty = this.getRexsterApplication().getGraph(graphName).getVertices();
        }

        if (null != itty) {
            for (Element element : itty) {
                if (counter >= start && counter < end) {
                    vertexArray.add(new ElementJSONObject(element, this.getReturnKeys()));
                }
                counter++;
            }
        }
        this.resultObject.put(Tokens.RESULTS, vertexArray);
        this.resultObject.put(Tokens.TOTAL_SIZE, counter);
    }

}
