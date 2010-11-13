package com.tinkerpop.rexster;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class RexsterResource extends BaseResource {

	@GET
    public JSONObject evaluate() throws JSONException {
    	this.resultObject.put("name", "Rexster: A RESTful Graph Shell");
        this.resultObject.put("graph_count", WebServer.GetRexsterApplication().getGraphCount());
        this.resultObject.put("query_time", this.sh.stopWatch());
        this.resultObject.put("up_time", this.getTimeAlive());
        return this.resultObject;
    }
}
