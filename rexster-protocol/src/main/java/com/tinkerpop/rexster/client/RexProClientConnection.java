package com.tinkerpop.rexster.client;

import com.tinkerpop.rexster.protocol.msg.RexProMessage;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;
import org.glassfish.grizzly.filterchain.TransportFilter;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.nio.transport.TCPNIOTransportBuilder;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

/**
 * Basic class for sending and receiving messages via RexPro.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public final class RexProClientConnection {
    public static final int DEFAULT_TIMEOUT_SECONDS = 100;

    private final Connection connection;
    private final BlockingQueue<RexProMessage> responseQueue = new SynchronousQueue<RexProMessage>(true);

    public RexProClientConnection(String rexProHost, int rexProPort) {
        TCPNIOTransport transport = getTransport(responseQueue);

        try {
            transport.start();

            connection = transport.connect(rexProHost, rexProPort).get(10, TimeUnit.SECONDS);
            connection.configureBlocking(true);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    public RexProMessage sendMessage(RexProMessage messageToSend) throws IOException {
        return sendMessage(messageToSend, DEFAULT_TIMEOUT_SECONDS);
    }

    public RexProMessage sendMessage(RexProMessage messageToSend, int timeoutSeconds) throws IOException {
        try {
            connection.write(new RexsterClient.MessageContainer((byte)0, messageToSend)).get(timeoutSeconds, TimeUnit.SECONDS);
            return responseQueue.take();
        } catch (Exception e) {
            throw new RuntimeException("Request [" + messageToSend.getClass().getName() + "] to Rexster failed [" + connection + "] - " + e.getMessage(), e);
        }
    }

    public void close() {
        connection.close();
    }

    public static TCPNIOTransport getTransport(final BlockingQueue<RexProMessage> responseQueue) {
        // Create a FilterChain using FilterChainBuilder
        final FilterChainBuilder filterChainBuilder = FilterChainBuilder.stateless();

        // Add TransportFilter, which is responsible
        // for reading and writing data to the connection
        filterChainBuilder.add(new TransportFilter());
        filterChainBuilder.add(new RexProClientFilter());
        filterChainBuilder.add(new BaseFilter() {
            @Override
            public NextAction handleRead(FilterChainContext ctx) throws IOException {
                responseQueue.add((RexProMessage) ctx.getMessage());
                return ctx.getStopAction();
            }
        });

        // Create TCP NIO transport
        TCPNIOTransport transport = TCPNIOTransportBuilder.newInstance().build();
        transport.setProcessor(filterChainBuilder.build());

        return transport;
    }
}
