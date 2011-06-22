package com.tinkerpop.rexster.protocol;

import java.io.IOException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.filterchain.*;
import org.glassfish.grizzly.impl.FutureImpl;
import org.glassfish.grizzly.impl.SafeFutureImpl;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.nio.transport.TCPNIOTransportBuilder;

public class RexProClient {
    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        Connection connection = null;

        final FutureImpl<RexProMessage> resultMessageFuture = SafeFutureImpl.create();

        // Create a FilterChain using FilterChainBuilder
        FilterChainBuilder filterChainBuilder = FilterChainBuilder.stateless();
        // Add TransportFilter, which is responsible
        // for reading and writing data to the connection
        filterChainBuilder.add(new TransportFilter());
        filterChainBuilder.add(new RexProMessageFilter());
        filterChainBuilder.add(new CustomClientFilter(resultMessageFuture));

        // Create TCP NIO transport
        final TCPNIOTransport transport =
                TCPNIOTransportBuilder.newInstance().build();
        transport.setProcessor(filterChainBuilder.build());

        try {
            // start transport
            transport.start();

            // Connect client to the server
            Future<Connection> future = transport.connect("localhost", 8185);

            connection = future.get(10, TimeUnit.SECONDS);

            // Initialize sample message
            byte[] testMessage = "RexPro test".getBytes();
            RexProMessage sentMessage = new RexProMessage((byte) 1, (byte) 1, (byte) 1, 123435, 67889, testMessage);

            connection.write(sentMessage);

            final RexProMessage rcvMessage = resultMessageFuture.get(10, TimeUnit.SECONDS);

            // Check if echo returned message equal to original one
            if (sentMessage.equals(rcvMessage)) {
                System.out.println("DONE!");
            } else {
                System.out.println("Messages are not equal!");
            }

        } finally {
            if (connection != null) {
                connection.close();
            }

            transport.stop();
        }
    }

    public static final class CustomClientFilter extends BaseFilter {
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
