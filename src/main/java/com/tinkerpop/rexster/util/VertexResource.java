package com.tinkerpop.rexster.util;

import com.tinkerpop.blueprints.pgm.Edge;
import com.tinkerpop.blueprints.pgm.Vertex;
import com.tinkerpop.rexster.traversals.ElementJSONObject;
import org.json.simple.JSONArray;
import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;

import java.util.List;
import java.util.Map;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class VertexResource extends BaseResource {

    @Get
    public Representation process() {
        Map<String, String> queryParameters = createQueryMap(this.getRequest().getResourceRef().getQueryAsForm());
        this.buildRequestObject(queryParameters);
        String id = (String) getRequest().getAttributes().get("id");
        String direction = (String) getRequest().getAttributes().get("direction");
        if (null == id) {
            getAllVertices();
        } else {
            if (null == direction)
                getSingleVertex(id);
            else
                getVertexEdges(id, direction);
        }
        return new StringRepresentation(this.resultObject.toJSONString(), MediaType.APPLICATION_JSON);
    }

    public void getVertexEdges(Object id, String direction) {
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
        this.resultObject.put(QUERY_TIME, sh.stopWatch());
    }

    public void getSingleVertex(Object id) {
        Vertex vertex = this.getRexsterApplication().getGraph().getVertex(id);
        this.resultObject.put(RESULT, new ElementJSONObject(vertex, (List) this.requestObject.get(RETURN_KEYS)));
        this.resultObject.put(QUERY_TIME, sh.stopWatch());
    }

    public void getAllVertices() {
        Integer start = this.getStartOffset();
        if (null == start)
            start = 0;
        Integer end = this.getEndOffset();
        if (null == end)
            end = Integer.MAX_VALUE;

        int counter = 0;
        JSONArray vertexArray = new JSONArray();
        for (Vertex vertex : this.getRexsterApplication().getGraph().getVertices()) {
            if (counter >= start && counter < end) {
                vertexArray.add(new ElementJSONObject(vertex, (List) this.requestObject.get(RETURN_KEYS)));
            }
            counter++;
        }
        this.resultObject.put(RESULT, vertexArray);
        this.resultObject.put(TOTAL_SIZE, counter);
        this.resultObject.put(QUERY_TIME, sh.stopWatch());

    }
}
