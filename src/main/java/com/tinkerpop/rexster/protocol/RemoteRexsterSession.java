package com.tinkerpop.rexster.protocol;

import java.util.UUID;

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
            RexProMessage sessionRequestMessageToSend = new com.tinkerpop.rexster.protocol.message.SessionRequestMessage();
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
