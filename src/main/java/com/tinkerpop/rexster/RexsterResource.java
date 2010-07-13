package com.tinkerpop.rexster;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

import java.util.Map;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class RexsterResource extends ServerResource {

    @Get
    public Representation evaluate() {
        StatisticsHelper sh = new StatisticsHelper();
        sh.stopWatch();
        JSONObject resultObject = new JSONObject();
        resultObject.put("name", "Rexster: A RESTful Graph Shell");
        resultObject.put("graph", this.getRexsterApplication().getGraph().toString());

        JSONArray queriesArray = new JSONArray();
        for (Map.Entry<String, Class> traversal : this.getRexsterApplication().getLoadedTraversalServices().entrySet()) {
            queriesArray.add(traversal.getKey());
        }
        resultObject.put("traversals", queriesArray);

        resultObject.put("query_time", sh.stopWatch());
        resultObject.put("up_time", this.getTimeAlive());
        resultObject.put("version", RexsterApplication.getVersion());
        return new StringRepresentation(resultObject.toJSONString(), MediaType.APPLICATION_JSON);
    }

    private String getTimeAlive() {
        long timeMillis = System.currentTimeMillis() - this.getRexsterApplication().getStartTime();
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

    protected RexsterApplication getRexsterApplication() {
        return (RexsterApplication) this.getApplication();
    }
}
