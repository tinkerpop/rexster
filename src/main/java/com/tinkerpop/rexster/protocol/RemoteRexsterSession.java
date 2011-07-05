package com.tinkerpop.rexster.protocol;

import com.tinkerpop.rexster.protocol.message.MessageType;
import com.tinkerpop.rexster.protocol.message.RexProMessage;
import com.tinkerpop.rexster.protocol.message.SessionRequestMessage;

import java.util.UUID;

/**
 * Client-side session with Rexster.
 */
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
            RexProMessage sessionRequestMessageToSend = new SessionRequestMessage(SessionRequestMessage.FLAG_NEW);
            final RexProMessage rcvMessage = RexPro.sendMessage(this.rexProHost, this.rexProPort, sessionRequestMessageToSend);
            this.sessionKey = rcvMessage.getSessionAsUUID();
        }
    }

    public void close() {

        if (sessionKey != RexProMessage.EMPTY_SESSION) {
            RexProMessage sessionKillMessageToSend = new SessionRequestMessage(SessionRequestMessage.FLAG_KILL);

            // need to set the session here so that the server knows which one to delete.
            sessionKillMessageToSend.setSession(BitWorks.convertUUIDToByteArray(this.sessionKey));
            final RexProMessage rcvMessage = RexPro.sendMessage(this.rexProHost, this.rexProPort, sessionKillMessageToSend);

            // response message will have an EMPTY_SESSION
            if (rcvMessage.getType() == MessageType.SESSION_RESPONSE) {
                this.sessionKey = rcvMessage.getSessionAsUUID();
            } else {
                // TODO: an error message
            }

        }
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
