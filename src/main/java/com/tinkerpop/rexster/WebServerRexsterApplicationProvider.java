package com.tinkerpop.rexster;

import java.util.Set;

/**
 * Providers the RexsterApplication through the instantiated WebServer and 
 * its static methods.
 */
public class WebServerRexsterApplicationProvider implements RexsterApplicationProvider {

	@Override
	public RexsterApplication getRexsterApplication() {
		return WebServer.getRexsterApplication();
	}

	@Override
	public RexsterApplicationGraph getApplicationGraph(String graphName) {
		return this.getRexsterApplication().getApplicationGraph(graphName);
	}
	
	@Override
	public ResultObjectCache getResultObjectCache(){
		return this.getRexsterApplication().getResultObjectCache();
	}
	
	@Override
	public Set<String> getGraphsNames(){
		return this.getRexsterApplication().getGraphsNames();
	}
	
	@Override
	public long getStartTime() {
		return this.getRexsterApplication().getStartTime();
	}
}
