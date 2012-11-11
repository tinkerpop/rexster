package com.tinkerpop.rexster.protocol;

import com.tinkerpop.rexster.protocol.filter.RexProMessageFilter;
import com.tinkerpop.rexster.protocol.msg.RexProMessage;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.GrizzlyFuture;
import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;
import org.glassfish.grizzly.filterchain.TransportFilter;
import org.glassfish.grizzly.impl.FutureImpl;
import org.glassfish.grizzly.impl.SafeFutureImpl;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.nio.transport.TCPNIOTransportBuilder;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Basic class for sending and receiving messages via RexPro.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public final class RexPro {

    public static final int DEFAULT_TIMEOUT_SECONDS = 100;

    public static RexProMessage sendMessage(final String rexProHost, final int rexProPort, final RexProMessage messageToSend) {
        return sendMessage(rexProHost, rexProPort, messageToSend, DEFAULT_TIMEOUT_SECONDS);
    }

    public static RexProMessage sendMessage(final String rexProHost, final int rexProPort,
                                            final RexProMessage messageToSend, final int timeoutSeconds) {
        final FutureImpl<RexProMessage> sessionMessageFuture = SafeFutureImpl.create();

        Connection connection = null;
        final TCPNIOTransport transport = getTransport(sessionMessageFuture);

        try {
            // start transport
            transport.start();

            // Connect client to the server
            final GrizzlyFuture<Connection> future = transport.connect(rexProHost, rexProPort);
            connection = future.get(timeoutSeconds, TimeUnit.SECONDS);
            connection.write(messageToSend);
            return sessionMessageFuture.get(timeoutSeconds, TimeUnit.SECONDS);

        } catch (Exception e) {
            throw new RuntimeException("Request to Rexster failed [" + rexProHost + ":" + rexProPort + "] - " + e.getMessage(), e);
        } finally {
            try {
                if (connection != null) {
                    connection.close();
                }

                transport.stop();
            } catch (IOException ioe) {
                //
            }
        }
    }

    public static TCPNIOTransport getTransport(final FutureImpl<RexProMessage> future) {
        // Create a FilterChain using FilterChainBuilder
        final FilterChainBuilder filterChainBuilder = FilterChainBuilder.stateless();

        // Add TransportFilter, which is responsible
        // for reading and writing data to the connection
        filterChainBuilder.add(new TransportFilter());
        filterChainBuilder.add(new RexProMessageFilter());
        filterChainBuilder.add(new CustomClientFilter(future));

        // Create TCP NIO transport
        final TCPNIOTransport transport =
                TCPNIOTransportBuilder.newInstance().build();
        transport.setProcessor(filterChainBuilder.build());

        return transport;
    }

    private static final class CustomClientFilter extends BaseFilter {
        private final FutureImpl<RexProMessage> resultFuture;

        public CustomClientFilter(final FutureImpl<RexProMessage> resultFuture) {
            this.resultFuture = resultFuture;
        }

        @Override
        public NextAction handleRead(FilterChainContext ctx) throws IOException {
            final RexProMessage message = ctx.getMessage();
            resultFuture.result(message);

            return ctx.getStopAction();
        }
    }
}
