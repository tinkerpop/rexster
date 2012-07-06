package com.tinkerpop.rexster.config;

/**
 * Exception thrown when an error occurs while configuring a Graph in a GraphConfiguration implementation.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class GraphConfigurationException extends Exception {

    public GraphConfigurationException(String msg) {
        super(msg);
    }

    public GraphConfigurationException(Throwable inner) {
        super(inner);
    }

    public GraphConfigurationException(String msg, Throwable inner) {
        super(msg, inner);
    }
}
