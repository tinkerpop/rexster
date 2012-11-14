package com.tinkerpop.rexster.client;

import com.tinkerpop.rexster.protocol.filter.RexProMessageFilter;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.filterchain.TransportFilter;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.nio.transport.TCPNIOTransportBuilder;
import org.glassfish.grizzly.strategies.SameThreadIOStrategy;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class RexsterClientFactory {

    private static final RexsterClientFactory factory = new RexsterClientFactory();

    private RexsterClientFactory() {

    }

    public static RexsterClientFactory getInstance() {
        return factory;
    }

    public RexsterClient createClient(final String host, final int port, int connectTimeout) throws Exception {
        Connection connection = null;
        RexsterClientHandler handler = new RexsterClientHandler();
        FilterChainBuilder filterChainBuilder = FilterChainBuilder.stateless();
        filterChainBuilder.add(new TransportFilter());
        filterChainBuilder.add(new RexProMessageFilter());
        filterChainBuilder.add(handler);
        final TCPNIOTransport transport = TCPNIOTransportBuilder.newInstance().build();
        transport.setIOStrategy(SameThreadIOStrategy.getInstance());
        transport.setProcessor(filterChainBuilder.build());
        transport.start();
        Future<Connection> future = transport.connect(host, port);
        connection = future.get(connectTimeout, TimeUnit.SECONDS);

        RexsterClient client = new RexsterClient(host, port, connectTimeout, connection, transport);
        handler.setClient(client);
        return client;
    }
}
