package com.tinkerpop.rexster.protocol;

import com.tinkerpop.rexster.protocol.message.MessageType;
import com.tinkerpop.rexster.protocol.message.RexProMessage;
import com.tinkerpop.rexster.protocol.message.SessionRequestMessage;
import com.tinkerpop.rexster.protocol.message.SessionResponseMessage;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Client-side session with Rexster.
 */
public class RemoteRexsterSession {

    private int rexProPort = 8185;
    private int timeout;
    private String rexProHost = "localhost";

    private UUID sessionKey = RexProMessage.EMPTY_SESSION;

    private List<String> availableLanguages;

    public RemoteRexsterSession(String rexProHost, int rexProPort) {
        this(rexProHost, rexProPort, RexPro.DEFAULT_TIMEOUT_SECONDS);
    }

    public RemoteRexsterSession(String rexProHost, int rexProPort, int timeout) {
        this.rexProHost = rexProHost;
        this.rexProPort = rexProPort;
        this.timeout = timeout;
    }

    public void open() {
        if (sessionKey == RexProMessage.EMPTY_SESSION) {
            RexProMessage sessionRequestMessageToSend = new SessionRequestMessage(SessionRequestMessage.FLAG_NEW_CONSOLE_SESSION);
            final RexProMessage rcvMessage = RexPro.sendMessage(this.rexProHost, this.rexProPort, sessionRequestMessageToSend);

            final SessionResponseMessage sessionResponseMessage = new SessionResponseMessage(rcvMessage);

            this.availableLanguages = sessionResponseMessage.getLanguages();

            this.sessionKey = rcvMessage.getSessionAsUUID();
        }
    }

    public Iterator<String> getAvailableLanguages() {
        if (sessionKey == RexProMessage.EMPTY_SESSION) {
            return null;
        }

        return this.availableLanguages.iterator();
    }

    public boolean isAvailableLanguage(String language) {

        if (sessionKey == RexProMessage.EMPTY_SESSION) {
            return false;
        }

        boolean found = false;
        Iterator<String> languageIterator = this.availableLanguages.iterator();
        while (languageIterator.hasNext()) {
            if (languageIterator.next().equals(language)) {
                found = true;
            }
        }

        return found;
    }

    public void reset() {
        this.close();

        this.sessionKey = RexProMessage.EMPTY_SESSION;

        this.open();
    }

    public void close() {

        if (sessionKey != RexProMessage.EMPTY_SESSION) {
            RexProMessage sessionKillMessageToSend = new SessionRequestMessage(SessionRequestMessage.FLAG_KILL_SESSION);

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
