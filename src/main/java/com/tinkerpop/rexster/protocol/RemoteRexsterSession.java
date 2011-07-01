package com.tinkerpop.rexster.protocol;

import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.GrizzlyFuture;
import org.glassfish.grizzly.filterchain.*;
import org.glassfish.grizzly.impl.FutureImpl;
import org.glassfish.grizzly.impl.SafeFutureImpl;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.nio.transport.TCPNIOTransportBuilder;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class RemoteRexsterSession {

    private int rexProPort = 8185;
    private String rexProHost = "localhost";

    private UUID sessionKey = RexProMessage.EMPTY_SESSION;

    public RemoteRexsterSession(String rexProHost, int rexProPort) {
        this.rexProHost = rexProHost;
        this.rexProPort = rexProPort;
    }

    public void open() {
        if (sessionKey == RexProMessage.EMPTY_SESSION) {
            RexProMessage sessionRequestMessageToSend = new SessionRequestMessage();
            final RexProMessage rcvMessage = RexPro.sendMessage(this.rexProHost, this.rexProPort, sessionRequestMessageToSend);
            this.sessionKey = rcvMessage.getSessionAsUUID();
        }
    }

    public void close() {
        this.sessionKey = RexProMessage.EMPTY_SESSION;

        // TODO: kill session on server
    }

    UUID getSessionKey() {
        return this.sessionKey;
    }

    public String getRexProHost() {
        return this.rexProHost;
    }

    public int getRexProPort() {
        return this.rexProPort;
    }
}
