package com.tinkerpop.rexster.protocol;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.GrizzlyFuture;
import org.glassfish.grizzly.filterchain.*;
import org.glassfish.grizzly.impl.FutureImpl;
import org.glassfish.grizzly.impl.SafeFutureImpl;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.nio.transport.TCPNIOTransportBuilder;

public class RexProClient {
    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {

        final FutureImpl<RexProMessage> resultMessageFuture = SafeFutureImpl.create();
        final FutureImpl<RexProMessage> sessionMessageFuture = SafeFutureImpl.create();

        Connection connection = null;
        TCPNIOTransport transport = getTransport(sessionMessageFuture);
        UUID sessionKey = SessionRequestMessage.EMPTY_SESSION;

        try {
            // start transport
            transport.start();

            // Connect client to the server
            GrizzlyFuture<Connection> future = transport.connect("localhost", 8185);

            connection = future.get(10, TimeUnit.SECONDS);

            // Initialize session message
            RexProMessage sentMessage = new SessionRequestMessage();

            connection.write(sentMessage);

            final RexProMessage rcvMessage = sessionMessageFuture.get(10, TimeUnit.SECONDS);
            sessionKey = rcvMessage.getSessionAsUUID();


        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            if (connection != null) {
                connection.close();
            }

            transport.stop();
        }

        transport = getTransport(resultMessageFuture);

        try {
            // start transport
            transport.start();

            // Connect client to the server
            Future<Connection> future = transport.connect("localhost", 8185);

            connection = future.get(10, TimeUnit.SECONDS);

            RexProMessage scriptMessage = new ScriptRequestMessage(sessionKey, "gremlin", "g = rexster.getGraph(\"tinkergraph\");g.V;");
            connection.write(scriptMessage);

            final RexProMessage resultMessage = resultMessageFuture.get(10, TimeUnit.SECONDS);

            ByteBuffer bb = ByteBuffer.wrap(resultMessage.getBody());
            while (bb.hasRemaining()) {
                int segmentLength = bb.getInt();
                byte[] resultObjectBytes = new byte[segmentLength];
                bb.get(resultObjectBytes);
                ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(resultObjectBytes);
                ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);

                Object o = objectInputStream.readObject();
                System.out.println(o.getClass().getName());
                System.out.println(o.toString());
            }
        } finally {
            if (connection != null) {
                connection.close();
            }

            transport.stop();
        }
    }


    private static TCPNIOTransport getTransport(FutureImpl<RexProMessage> future) {
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
