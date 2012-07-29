package com.tinkerpop.rexster.server;

/**
 * Interface for the various "servers" that Rexster exposes.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public interface RexsterServer {
    void stop() throws Exception;

    void start(final RexsterApplication application) throws Exception ;
}
