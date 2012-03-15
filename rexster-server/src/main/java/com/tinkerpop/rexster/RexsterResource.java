package com.tinkerpop.rexster;

import com.tinkerpop.rexster.extension.HttpMethod;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.Set;

@Path("/graphs")
@Produces({MediaType.APPLICATION_JSON, RexsterMediaType.APPLICATION_REXSTER_JSON, RexsterMediaType.APPLICATION_REXSTER_TYPED_JSON})
public class RexsterResource extends BaseResource {

    @Context
    public ServletContext servletCtx;

    public RexsterResource() {
        super(null);
    }

    public RexsterResource(RexsterApplication ra) {
        super(ra);
    }

    @OPTIONS
    public Response optionsRexsterRoot() {
        return buildOptionsResponse(HttpMethod.GET.toString());
    }

    @GET
    public Response getRexsterRoot() {
        try {

            Set<String> graphNames = this.getRexsterApplication().getGraphNames();
            JSONArray jsonArrayNames = new JSONArray(graphNames);

            this.resultObject.put("name", "Rexster: A Graph Server");
            this.resultObject.put("graphs", jsonArrayNames);
            this.resultObject.put(Tokens.QUERY_TIME, this.sh.stopWatch());
            this.resultObject.put(Tokens.UP_TIME, this.getTimeAlive());
            return Response.ok(this.resultObject).build();

        } catch (JSONException ex) {
            JSONObject error = generateErrorObject(ex.getMessage());
            throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR).entity(error).build());
        }
    }
}
