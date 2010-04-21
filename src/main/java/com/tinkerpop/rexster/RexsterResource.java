package com.tinkerpop.rexster;

import com.tinkerpop.rexster.traversals.Traversal;
import org.json.simple.JSONObject;
import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

import java.util.*;

/**
 * @author: Marko A. Rodriguez (http://markorodriguez.com)
 */
public class RexsterResource extends ServerResource {

    @Get
    public Representation evaluate() {
        StatisticsHelper sh = new StatisticsHelper();
        sh.stopWatch();
        JSONObject resultObject = new JSONObject();
        resultObject.put("name", "Rexster: A Graph-Based Ranking Engine");
        JSONObject queriesObject = new JSONObject();
        ServiceLoader<Traversal> queryResources = ServiceLoader.load(Traversal.class);
        for (Traversal traversalResource : queryResources) {
            queriesObject.put(traversalResource.getResourceName(), traversalResource.getClass().getName());
        }
        resultObject.put("traversals", queriesObject);
        resultObject.put("query_time", sh.stopWatch());

        try {
        } catch(Exception e) {
            e.printStackTrace();
        }
        return new StringRepresentation(resultObject.toJSONString(), MediaType.APPLICATION_JSON);
    }
}
