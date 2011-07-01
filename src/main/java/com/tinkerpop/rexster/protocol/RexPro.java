package com.tinkerpop.rexster.protocol;

import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.GrizzlyFuture;
import org.glassfish.grizzly.filterchain.*;
import org.glassfish.grizzly.impl.FutureImpl;
import org.glassfish.grizzly.impl.SafeFutureImpl;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.nio.transport.TCPNIOTransportBuilder;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

final class RexPro {

    public static RexProMessage sendMessage(String rexProHost, int rexProPort, RexProMessage messageToSend) {
        final FutureImpl<RexProMessage> sessionMessageFuture = SafeFutureImpl.create();

        Connection connection = null;
        TCPNIOTransport transport = getTransport(sessionMessageFuture);

        try {
            // start transport
            transport.start();

            // Connect client to the server
            GrizzlyFuture<Connection> future = transport.connect(rexProHost, rexProPort);

            connection = future.get(10, TimeUnit.SECONDS);

            connection.write(messageToSend);

            return sessionMessageFuture.get(10, TimeUnit.SECONDS);

        } catch (Exception e) {
            throw new RuntimeException("Could not open session with Rexster at " + rexProHost + ":" + rexProPort, e);
        }finally {
            try {
                if (connection != null) {
                    connection.close();
                }

                transport.stop();
            } catch (IOException ioe) {
                // log??
            }
        }
    }

    public static TCPNIOTransport getTransport(FutureImpl<RexProMessage> future) {
        // Create a FilterChain using FilterChainBuilder
        FilterChainBuilder filterChainBuilder = FilterChainBuilder.stateless();
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

        public CustomClientFilter(FutureImpl<RexProMessage> resultFuture) {
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
