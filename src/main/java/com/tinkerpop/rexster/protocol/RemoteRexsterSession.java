package com.tinkerpop.rexster.protocol;

import com.tinkerpop.rexster.protocol.message.KillSessionRequestMessage;
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

    private int rexProPort = 8184;
    private int timeout;
    private String rexProHost = "localhost";
    private String username = "";
    private String password = "";

    private UUID sessionKey = RexProMessage.EMPTY_SESSION;

    private List<String> availableLanguages;

    public RemoteRexsterSession(String rexProHost, int rexProPort, String username, String password) {
        this(rexProHost, rexProPort, RexPro.DEFAULT_TIMEOUT_SECONDS, username, password);
    }

    public RemoteRexsterSession(String rexProHost, int rexProPort, int timeout, String username, String password) {
        this.rexProHost = rexProHost;
        this.rexProPort = rexProPort;
        this.timeout = timeout;
        this.username = username;
        this.password = password;
    }

    public void open() {
        if (sessionKey == RexProMessage.EMPTY_SESSION) {
            RexProMessage sessionRequestMessageToSend = new SessionRequestMessage(
                    SessionRequestMessage.FLAG_NEW_SESSION, SessionRequestMessage.CHANNEL_CONSOLE,
                    this.username, this.password);
            final RexProMessage rcvMessage = sendRequest(sessionRequestMessageToSend, 3);

            if (rcvMessage != null && rcvMessage.getType() == MessageType.SESSION_RESPONSE) {
                final SessionResponseMessage sessionResponseMessage = new SessionResponseMessage(rcvMessage);
                this.availableLanguages = sessionResponseMessage.getLanguages();
                this.sessionKey = rcvMessage.getSessionAsUUID();
            }
        }
    }

    public boolean isOpen() {
        return this.sessionKey != RexProMessage.EMPTY_SESSION;
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

    public RexProMessage sendRequest(RexProMessage request, int maxRetries) {
        return sendRequest(request, maxRetries, 3000);
    }

    public RexProMessage sendRequest(RexProMessage request, int maxRetries, int waitMsBetweenTries) {
        int tries = 0;
        RexProMessage rcvMessage = null;

        while (rcvMessage == null && tries < maxRetries) {
            tries++;

            try {
                rcvMessage = RexPro.sendMessage(this.rexProHost, this.rexProPort, request);
            } catch (Exception ex) {

                String logMessage = "Failure sending message via RexPro. Attempt [" + tries + "] of [" + maxRetries + "].";

                if (tries < maxRetries) {
                    logMessage = logMessage + " Trying again in " + waitMsBetweenTries + " (ms)";
                }

                //logger.warn(logMessage, ex);

                rcvMessage = null;

                // wait
                try {
                    Thread.sleep(waitMsBetweenTries);
                } catch (InterruptedException ie) {
                    // carry on
                }
            }
        }

        return rcvMessage;
    }

    public void reset() {
        this.close();
        this.open();
    }

    public void close() {

        try {
            if (sessionKey != RexProMessage.EMPTY_SESSION) {
                RexProMessage sessionKillMessageToSend = new KillSessionRequestMessage();

                // need to set the session here so that the server knows which one to delete.
                sessionKillMessageToSend.setSession(BitWorks.convertUUIDToByteArray(this.sessionKey));
                final RexProMessage rcvMessage = sendRequest(sessionKillMessageToSend, 3);

                // response message will have an EMPTY_SESSION
                if (rcvMessage.getType() == MessageType.SESSION_RESPONSE) {
                    this.sessionKey = rcvMessage.getSessionAsUUID();
                } else {
                    // TODO: an error message
                }
            }
        } catch (Exception ex) {
            // likely fail is a null pointer on the session
        } finally {
            this.sessionKey = RexProMessage.EMPTY_SESSION;
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
