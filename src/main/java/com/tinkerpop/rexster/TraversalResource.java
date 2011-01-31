package com.tinkerpop.rexster;

import com.tinkerpop.rexster.traversals.Traversal;
import com.tinkerpop.rexster.traversals.TraversalException;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.Status;
import java.util.List;
import java.util.Map;

@Path("/{graphname}/traversals")
@Produces(MediaType.APPLICATION_JSON)
public class TraversalResource extends AbstractSubResource {

    private static Logger logger = Logger.getLogger(TraversalResource.class);

    public TraversalResource() {
        super(null);
    }

    public TraversalResource(UriInfo ui, HttpServletRequest req, RexsterApplicationProvider rap) {
        super(rap);
        this.httpServletRequest = req;
        this.uriInfo = ui;
    }

    @GET
    public Response getTraversals(@PathParam("graphname") String graphName) {

        try {
            int counter = 0;
            JSONArray queriesArray = new JSONArray();
            for (Map.Entry<String, Class<? extends Traversal>> traversal : this.getRexsterApplicationGraph(graphName).getLoadedTraversals().entrySet()) {
                JSONObject traversalItem = new JSONObject();
                traversalItem.put("path", traversal.getKey());
                traversalItem.put("traversal", traversal.getValue().getName());
                queriesArray.put(traversalItem);
                counter++;
            }

            this.resultObject.put(Tokens.RESULTS, queriesArray);
            this.resultObject.put(Tokens.TOTAL_SIZE, counter);
            this.resultObject.put(Tokens.QUERY_TIME, this.sh.stopWatch());
        } catch (JSONException ex) {
            logger.error(ex);

            JSONObject error = generateErrorObjectJsonFail(ex);
            throw new WebApplicationException(this.addHeaders(Response.status(Status.INTERNAL_SERVER_ERROR).entity(error)).build());
        }

        return this.addHeaders(Response.ok(this.resultObject)).build();
    }

    @GET
    @Path("/{path: .+}")
    public Response getTraversal(@PathParam("graphname") String graphName) {
        return this.processTraversal(graphName);
    }

    @POST
    @Path("/{path: .+}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getTraversal(@PathParam("graphname") String graphName, JSONObject json) {
        this.setRequestObject(json);
        return this.processTraversal(graphName);
    }

    private Response processTraversal(String graphName) {
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
            traversalClass = this.getRexsterApplicationGraph(graphName).getLoadedTraversals().get(pattern.substring(1));

            if (traversalClass != null) {
                Traversal traversal = (Traversal) traversalClass.newInstance();

                RexsterResourceContext ctx = new RexsterResourceContext();
                ctx.setRequest(this.httpServletRequest);
                ctx.setResultObject(this.resultObject);
                ctx.setUriInfo(this.uriInfo);
                ctx.setRexsterApplicationGraph(this.getRexsterApplicationGraph(graphName));
                ctx.setRequestObject(this.getRequestObject());
                ctx.setCache(this.getRexsterApplicationProvider().getResultObjectCache());

                traversal.evaluate(ctx);
            } else {
                logger.info("Request for a non-configured traversal [" + pattern + "]");

                JSONObject error = generateErrorObject("Graph [" + pattern + "] could not be found.");
                throw new WebApplicationException(Response.status(Status.NOT_FOUND).entity(error).build());
            }

            this.resultObject.put(Tokens.QUERY_TIME, sh.stopWatch());
        } catch (JSONException jsonEx) {
            logger.error(jsonEx);

            JSONObject error = generateErrorObjectJsonFail(jsonEx);
            throw new WebApplicationException(this.addHeaders(Response.status(Status.INTERNAL_SERVER_ERROR).entity(error)).build());
        } catch (TraversalException ex) {
            logger.error(ex);

            JSONObject error = generateErrorObjectJsonFail(ex);
            throw new WebApplicationException(this.addHeaders(Response.status(Status.INTERNAL_SERVER_ERROR).entity(error)).build());
        } catch (IllegalAccessException iae) {
            logger.error(iae);

            JSONObject error = generateErrorObject("Failed to instantiate the Traversal for [" + pattern + "].  No access to [" + traversalClass.getName() + "]", iae);
            throw new WebApplicationException(this.addHeaders(Response.status(Status.INTERNAL_SERVER_ERROR).entity(error)).build());

        } catch (InstantiationException ie) {
            logger.error(ie);

            JSONObject error = generateErrorObject("Failed to instantiate the Traversal for [" + pattern + "].  Expected a concrete class definition for [" + traversalClass.getName() + "]", ie);
            throw new WebApplicationException(this.addHeaders(Response.status(Status.INTERNAL_SERVER_ERROR).entity(error)).build());
        }

        return this.addHeaders(Response.ok(this.resultObject)).build();
    }
}
