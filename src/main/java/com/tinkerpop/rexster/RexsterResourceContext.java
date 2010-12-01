package com.tinkerpop.rexster;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.UriInfo;

import org.codehaus.jettison.json.JSONObject;

/**
 * Context for a resource request.  Provides request information to a traversal.
 */
public class RexsterResourceContext {
	private RexsterApplicationGraph rag;
	
	private UriInfo uriInfo;
	private HttpServletRequest request;
	private JSONObject resultObject;
	private JSONObject requestObject;
    private ResultObjectCache cache;
	
	public JSONObject getRequestObject() {
		return requestObject;
	}

	public void setRequestObject(JSONObject requestObject) {
		this.requestObject = requestObject;
	}

	public JSONObject getResultObject() {
		return this.resultObject;
	}

	public void setResultObject(JSONObject resultObjectCache) {
		this.resultObject = resultObjectCache;
	}

	public UriInfo getUriInfo() {
		return uriInfo;
	}

	public void setUriInfo(UriInfo uriInfo) {
		this.uriInfo = uriInfo;
	}

	public HttpServletRequest getRequest() {
		return request;
	}

	public void setRequest(HttpServletRequest request) {
		this.request = request;
	}

	public RexsterApplicationGraph getRexsterApplicationGraph() {
		return rag;
	}

	public void setRexsterApplicationGraph(RexsterApplicationGraph rag) {
		this.rag = rag;
	}

	public ResultObjectCache getCache() {
		return cache;
	}

	public void setCache(ResultObjectCache cache) {
		this.cache = cache;
	}

}
