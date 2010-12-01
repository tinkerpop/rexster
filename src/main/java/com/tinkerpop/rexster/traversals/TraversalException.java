package com.tinkerpop.rexster.traversals;

public class TraversalException extends Exception {
	public TraversalException(String msg){
		super(msg);
	}
	
	public TraversalException(Throwable inner){
		super(inner);
	}
	
	public TraversalException(String msg, Throwable inner){
		super(msg, inner);
	}
}
