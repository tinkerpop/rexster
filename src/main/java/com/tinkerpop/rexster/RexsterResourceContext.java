package com.tinkerpop.rexster;

import com.tinkerpop.rexster.extension.ExtensionMethod;
import org.codehaus.jettison.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

/**
 * Context for a resource request.  Provides request information to an extension.
 */
public class RexsterResourceContext {
    private RexsterApplicationGraph rag;

    private UriInfo uriInfo;
    private HttpServletRequest request;
    private JSONObject requestObject;
    private JSONObject requestObjectFlat;
    private ExtensionMethod extensionMethod;
    private SecurityContext securityContext;

    public RexsterResourceContext(RexsterApplicationGraph rag, UriInfo uriInfo, HttpServletRequest request,
                                  JSONObject requestObject, JSONObject requestObjectFlat, ExtensionMethod extensionMethod,
                                  SecurityContext securityContext) {
        this.rag = rag;
        this.uriInfo = uriInfo;
        this.request = request;
        this.requestObject = requestObject;
        this.extensionMethod = extensionMethod;
        this.requestObjectFlat = requestObjectFlat;
        this.securityContext = securityContext;
    }

    public SecurityContext getSecurityContext() {
        return this.securityContext;
    }

    public JSONObject getRequestObject() {
        return this.requestObject;
    }

    public JSONObject getRequestObjectFlat() {
        return this.requestObjectFlat;
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

    public ExtensionMethod getExtensionMethod() {
        return this.extensionMethod;
    }

}
