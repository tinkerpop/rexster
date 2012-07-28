package com.tinkerpop.rexster.server;

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public interface RexsterServer {
    void stop() throws Exception;

    void start() throws Exception ;
}
