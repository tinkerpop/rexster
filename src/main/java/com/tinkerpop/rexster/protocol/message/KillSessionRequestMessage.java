package com.tinkerpop.rexster.protocol.message;

public class KillSessionRequestMessage extends SessionRequestMessage {
    public KillSessionRequestMessage() {
        super(SessionRequestMessage.FLAG_KILL_SESSION);
    }
}
