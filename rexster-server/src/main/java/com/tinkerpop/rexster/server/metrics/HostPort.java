package com.tinkerpop.rexster.server.metrics;

/**
 * Represents a host and port combination.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
class HostPort {

    private final String host;

    private final int port;

    public HostPort(final String host, final int port)
    {
        this.host = host;
        this.port = port;
    }

    public String getHost()
    {
        return host;
    }

    public int getPort()
    {
        return port;
    }
}
