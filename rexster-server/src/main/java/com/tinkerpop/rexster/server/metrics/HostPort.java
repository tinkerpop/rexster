package com.tinkerpop.rexster.server.metrics;

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class HostPort {

    private String host;

    private int port;

    public HostPort() {}

    public HostPort(String host, int port)
    {
        this.host = host;
        this.port = port;
    }

    public String getHost()
    {
        return host;
    }

    public void setHost(String host)
    {
        this.host = host;
    }

    public int getPort()
    {
        return port;
    }

    public void setPort(int port)
    {
        this.port = port;
    }
}
