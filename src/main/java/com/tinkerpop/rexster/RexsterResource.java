package com.tinkerpop.rexster;

import com.tinkerpop.rexster.traversals.Traversal;
import org.json.simple.JSONObject;
import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

import java.util.ServiceLoader;

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
        resultObject.put("up_time", this.getTimeAlive());
        return new StringRepresentation(resultObject.toJSONString(), MediaType.APPLICATION_JSON);
    }

    private String getTimeAlive() {
        long timeMillis = System.currentTimeMillis() - ((RexsterApplication) this.getApplication()).getStartTime();
        long time = timeMillis / 1000;
        String seconds = Integer.toString((int) (time % 60));
        String minutes = Integer.toString((int) ((time % 3600) / 60));
        String hours = Integer.toString((int) (time / 3600));
        String days = Integer.toString((int) (time / 86400));
        for (int i = 0; i < 2; i++) {
            if (seconds.length() < 2) {
                seconds = "0" + seconds;
            }
            if (minutes.length() < 2) {
                minutes = "0" + minutes;
            }
            if (hours.length() < 2) {
                hours = "0" + hours;
            }
        }
        return days + "[d]:" + hours + "[h]:" + minutes + "[m]:" + seconds + "[s]";
    }
}
