package com.tinkerpop.rexster;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.Status;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.tinkerpop.rexster.traversals.Traversal;

@Path("/{graphname}/traversals")
@Produces(MediaType.APPLICATION_JSON)
public class TraversalResource extends BaseResource {
	
	private static Logger logger = Logger.getLogger(TraversalResource.class);

    public TraversalResource(@PathParam("graphname") String graphName, @Context UriInfo ui, @Context HttpServletRequest req) {
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

    @GET
    @Path("/{path: .+}")
    public Response getTraversal() {

        Class<? extends Traversal> traversalClass = null;
        String pattern = "";

        try {

            List<PathSegment> pathSegments = this.uriInfo.getPathSegments();

            // ignore the first two parts of the path as they are "graphname/traversal".
            // everything after that point represents the name of the traversal specified
            // by the getTraversalName() method on the Traversal interface
            for (int ix = 2; ix < pathSegments.size(); ix++) {
                pattern = pattern + "/" + pathSegments.get(ix).getPath();
            }

            // get the traversal class based on the pattern of the URI. strip the first
            // character of the pattern as the variable is initialized that way in the loop
            traversalClass = this.rag.getLoadedTraversals().get(pattern.substring(1));

            if (traversalClass != null) {
                Traversal traversal = (Traversal) traversalClass.newInstance();

                RexsterResourceContext ctx = new RexsterResourceContext();
                ctx.setRequest(this.request);
                ctx.setResultObject(this.resultObject);
                ctx.setUriInfo(this.uriInfo);
                ctx.setRexsterApplicationGraph(this.rag);
                ctx.setRequestObject(this.requestObject);

                traversal.evaluate(ctx);
            } else {
                logger.info("Request for a non-configured traversal [" + pattern + "]");

                JSONObject error = generateErrorObject("Graph [" + pattern + "] could not be found.");
                throw new WebApplicationException(Response.status(Status.NOT_FOUND).entity(error).build());
            }

            this.resultObject.put(Tokens.QUERY_TIME, sh.stopWatch());
        } catch (JSONException ex) {
            logger.error(ex);

            JSONObject error = generateErrorObjectJsonFail(ex);
            throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR).entity(error).build());
        } catch (IllegalAccessException iae) {
            logger.error(iae);

            JSONObject error = generateErrorObject("Failed to instantiate the Traversal for [" + pattern + "].  No access to [" + traversalClass.getName() + "]", iae);
            throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR).entity(error).build());

        } catch (InstantiationException ie) {
            logger.error(ie);

            JSONObject error = generateErrorObject("Failed to instantiate the Traversal for [" + pattern + "].  Expected a concrete class definition for [" + traversalClass.getName() + "]", ie);
            throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR).entity(error).build());
        }

        return Response.ok(this.resultObject).build();
    }
}
