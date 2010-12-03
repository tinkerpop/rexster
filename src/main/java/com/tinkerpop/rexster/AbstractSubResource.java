package com.tinkerpop.rexster;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

public abstract class AbstractSubResource extends BaseResource {
	
	private static final Logger logger = Logger.getLogger(AbstractSubResource.class);
	
	protected AbstractSubResource(String graphName, UriInfo ui, HttpServletRequest req) {
    	super();

        this.rag = WebServer.GetRexsterApplication().getApplicationGraph(graphName);
        if (this.rag == null) {

            logger.info("Request for a non-configured graph [" + graphName + "]");

            JSONObject error = generateErrorObject("Graph [" + graphName + "] could not be found.");
            throw new WebApplicationException(Response.status(Status.NOT_FOUND).entity(error).build());
        }

        try {
            this.resultObject.put(Tokens.VERSION, RexsterApplication.getVersion());
            Map<String, String> queryParameters = req.getParameterMap();
            this.buildRequestObject(queryParameters);

            this.request = req;
            this.uriInfo = ui;
        } catch (JSONException ex) {

            logger.error(ex);

            JSONObject error = generateErrorObjectJsonFail(ex);
            throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR).entity(error).build());
        }
    }
}
