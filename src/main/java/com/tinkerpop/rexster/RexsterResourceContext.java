package com.tinkerpop.rexster;

import com.tinkerpop.rexster.extension.ExtensionMethod;
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
    private ExtensionMethod extensionMethod;

    public RexsterResourceContext(RexsterApplicationGraph rag, UriInfo uriInfo, HttpServletRequest request,
                                  JSONObject requestObject, ResultObjectCache cache, ExtensionMethod extensionMethod) {
        this.rag = rag;
        this.uriInfo = uriInfo;
        this.request = request;
        this.requestObject = requestObject;
        this.cache = cache;
        this.extensionMethod = extensionMethod;
    }

    public JSONObject getRequestObject() {
        return this.requestObject;
    }

    public UriInfo getUriInfo() {
        return this.uriInfo;
    }

    public HttpServletRequest getRequest() {
        return this.request;
    }

    public RexsterApplicationGraph getRexsterApplicationGraph() {
        return this.rag;
    }

    public ResultObjectCache getCache() {
        return this.cache;
    }

    public ExtensionMethod getExtensionMethod() {
        return this.extensionMethod;
    }

}
