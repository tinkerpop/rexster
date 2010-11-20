package com.tinkerpop.rexster.config;

public class GraphConfigurationException extends Exception {
	public GraphConfigurationException(Throwable inner){
		super(inner);
	}
	
	public GraphConfigurationException(String msg, Throwable inner){
		super(msg, inner);
	}
}
