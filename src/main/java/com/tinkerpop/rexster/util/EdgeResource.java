package com.tinkerpop.rexster.util;

import com.tinkerpop.blueprints.pgm.Edge;
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
public class EdgeResource extends BaseResource {

    @Get
    public Representation getResource() {
        Map<String, String> queryParameters = createQueryMap(this.getRequest().getResourceRef().getQueryAsForm());
        this.buildRequestObject(queryParameters);
        getAllEdges();
        return new StringRepresentation(this.resultObject.toJSONString(), MediaType.APPLICATION_JSON);
    }

    public void getAllEdges() {
        Integer start = this.getStartOffset();
        if (null == start)
            start = 0;
        Integer end = this.getEndOffset();
        if (null == end)
            end = Integer.MAX_VALUE;

        int counter = 0;
        JSONArray edgeArray = new JSONArray();
        for (Edge edge : this.getRexsterApplication().getGraph().getEdges()) {
            if (counter >= start && counter < end) {
                edgeArray.add(new ElementJSONObject(edge, (List) this.requestObject.get(RETURN_KEYS)));
            }
            counter++;
        }
        this.resultObject.put(RESULT, edgeArray);
        this.resultObject.put(TOTAL_SIZE, counter);
        this.resultObject.put(QUERY_TIME, sh.stopWatch());

    }
}