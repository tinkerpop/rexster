package com.tinkerpop.rexster;

import com.codahale.metrics.MetricRegistry;
import com.tinkerpop.rexster.extension.ExtensionMethod;
import org.codehaus.jettison.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

/**
 * Context for a resource request.  Provides request information to an extension.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class RexsterResourceContext {
    private final RexsterApplicationGraph rag;

    private final UriInfo uriInfo;
    private final HttpServletRequest request;
    private final JSONObject requestObject;
    private final JSONObject requestObjectFlat;
    private final ExtensionMethod extensionMethod;
    private final SecurityContext securityContext;
    private final MetricRegistry metricRegistry;

    public RexsterResourceContext(final RexsterApplicationGraph rag, final UriInfo uriInfo, final HttpServletRequest request,
                                  final JSONObject requestObject, final JSONObject requestObjectFlat, final ExtensionMethod extensionMethod,
                                  final SecurityContext securityContext, final MetricRegistry metricRegistry) {
        this.rag = rag;
        this.uriInfo = uriInfo;
        this.request = request;
        this.requestObject = requestObject;
        this.extensionMethod = extensionMethod;
        this.requestObjectFlat = requestObjectFlat;
        this.securityContext = securityContext;
        this.metricRegistry = metricRegistry;
    }

    public MetricRegistry getMetricRegistry() {
        return this.metricRegistry;
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
