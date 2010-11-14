package com.tinkerpop.rexster;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.sun.jersey.api.Responses;

@Path("/")
@Produces({MediaType.APPLICATION_JSON })
public class RexsterResource extends BaseResource {

	@GET
    public Response evaluate() {
		try {
	    	this.resultObject.put("name", "Rexster: A RESTful Graph Shell");
	        this.resultObject.put("graph_count", WebServer.GetRexsterApplication().getGraphCount());
	        this.resultObject.put("query_time", this.sh.stopWatch());
	        this.resultObject.put("up_time", this.getTimeAlive());
	        return Response.ok(this.resultObject).build();
		} catch (JSONException ex) {
			JSONObject error = generateErrorObject(ex.getMessage());
			throw new WebApplicationException(
					Response.status(Status.INTERNAL_SERVER_ERROR).entity(error).build());
		}
    }
}
