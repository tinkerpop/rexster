package com.tinkerpop.rexster;

import org.codehaus.jettison.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.UriInfo;

/**
 * Context for a resource request.  Provides request information to an extension.
 */
public class RexsterResourceContext {
    private RexsterApplicationGraph rag;

    private UriInfo uriInfo;
    private HttpServletRequest request;
    private JSONObject requestObject;
    private ResultObjectCache cache;

    public RexsterResourceContext(RexsterApplicationGraph rag, UriInfo uriInfo, HttpServletRequest request,
                                  JSONObject requestObject, ResultObjectCache cache) {
        this.rag = rag;
        this.uriInfo = uriInfo;
        this.request = request;
        this.requestObject = requestObject;
        this.cache = cache;
    }

    public JSONObject getRequestObject() {
        return requestObject;
    }

    public UriInfo getUriInfo() {
        return uriInfo;
    }

    public HttpServletRequest getRequest() {
        return request;
    }

    public RexsterApplicationGraph getRexsterApplicationGraph() {
        return rag;
    }

    public ResultObjectCache getCache() {
        return cache;
    }

}
