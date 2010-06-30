package com.tinkerpop.rexster;

import com.tinkerpop.blueprints.pgm.Edge;
import com.tinkerpop.rexster.traversals.ElementJSONObject;
import org.json.simple.JSONArray;
import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;

import java.util.Map;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class EdgeResource extends BaseResource {

    @Get
    public Representation getResource() {
        Map<String, String> queryParameters = createQueryMap(this.getRequest().getResourceRef().getQueryAsForm());
        this.buildRequestObject(queryParameters);
        getAllEdges();
        return new StringRepresentation(this.resultObject.toJSONString(), MediaType.APPLICATION_JSON);
    }

    public void getAllEdges() {
        Long start = this.getStartOffset();
        if (null == start)
            start = 0l;
        Long end = this.getEndOffset();
        if (null == end)
            end = Long.MAX_VALUE;

        long counter = 0l;
        JSONArray edgeArray = new JSONArray();
        for (Edge edge : this.getRexsterApplication().getGraph().getEdges()) {
            if (counter >= start && counter < end) {
                edgeArray.add(new ElementJSONObject(edge, this.getReturnKeys()));
            }
            counter++;
        }
        this.resultObject.put(RESULT, edgeArray);
        this.resultObject.put(TOTAL_SIZE, counter);
        this.resultObject.put(QUERY_TIME, sh.stopWatch());

    }
}