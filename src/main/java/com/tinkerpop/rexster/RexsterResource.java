package com.tinkerpop.rexster;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.sun.jersey.api.view.Viewable;
import com.sun.jersey.core.util.ReaderWriter;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.Status;

import java.net.URL;
import java.util.Set;

@Path("/")
@Produces({MediaType.APPLICATION_JSON})
public class RexsterResource extends BaseResource {
    
	@Context
    public ServletContext servletCtx;
	
	public RexsterResource(){
	    super(null);	
	}
	
	public RexsterResource(RexsterApplicationProvider rap){
	    super(rap);	
	}
	
    @GET
    public Response evaluate() {
        try {

            Set<String> graphNames = this.rexsterApplicationProvider.getGraphsNames();
            JSONArray jsonArrayNames = new JSONArray(graphNames);

            this.resultObject.put("name", "Rexster: A RESTful Graph Shell");
            this.resultObject.put("graphs", jsonArrayNames);
            this.resultObject.put("query_time", this.sh.stopWatch());
            this.resultObject.put("up_time", this.getTimeAlive());
            return this.addHeaders(Response.ok(this.resultObject)).build();

        } catch (JSONException ex) {
            JSONObject error = generateErrorObject(ex.getMessage());
            throw new WebApplicationException(
            		this.addHeaders(Response.status(Status.INTERNAL_SERVER_ERROR).entity(error)).build());
        }
    }
}
