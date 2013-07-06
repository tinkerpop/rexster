package com.tinkerpop.rexster.client;

import com.tinkerpop.rexster.protocol.msg.RexProMessage;
import com.tinkerpop.rexster.protocol.msg.SessionRequestMessage;
import com.tinkerpop.rexster.protocol.msg.SessionResponseMessage;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Client-side session with Rexster.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class RemoteRexsterSession {
    private static final Logger logger = Logger.getLogger(RemoteRexsterSession.class);

    private int rexProPort = 8184;
    private int timeout;
    private String rexProHost = "localhost";
    private String username = "";
    private String password = "";

    private RexProClientConnection rexProConnection;

    private UUID sessionKey = RexProMessage.EMPTY_SESSION;

    private List<String> availableLanguages;

    public RemoteRexsterSession(String rexProHost, int rexProPort, String username, String password) {
        this(rexProHost, rexProPort, RexProClientConnection.DEFAULT_TIMEOUT_SECONDS, username, password);
    }

    public RemoteRexsterSession(String rexProHost, int rexProPort, int timeout, String username, String password) {
        this.rexProHost = rexProHost;
        this.rexProPort = rexProPort;
        this.timeout = timeout;
        this.username = username;
        this.password = password;
        this.rexProConnection = new RexProClientConnection(rexProHost, rexProPort);
    }

    public void open() {
        if (sessionKey == RexProMessage.EMPTY_SESSION) {
            SessionRequestMessage sessionRequestMessageToSend = new SessionRequestMessage();
            sessionRequestMessageToSend.Username = this.username;
            sessionRequestMessageToSend.Password = this.password;
            sessionRequestMessageToSend.setSessionAsUUID(SessionRequestMessage.EMPTY_SESSION);
            sessionRequestMessageToSend.setRequestAsUUID(UUID.randomUUID());

            try {
                sessionRequestMessageToSend.validateMetaData();
            } catch (RexProException e) {
                e.printStackTrace();
            }

            // if close() gets called then have to recreate the the connection here.  need to factor out this
            // RexPro class.
            if (this.rexProConnection == null) {
                this.rexProConnection = new RexProClientConnection(rexProHost, rexProPort);
            }

            final RexProMessage rcvMessage = sendRequest(sessionRequestMessageToSend, 3);

            if (rcvMessage != null && rcvMessage instanceof SessionResponseMessage) {
                final SessionResponseMessage sessionResponseMessage = (SessionResponseMessage) rcvMessage;
                this.availableLanguages = new ArrayList<String>();
                for (String lang : sessionResponseMessage.Languages) {
                    this.availableLanguages.add(lang);
                }

                this.sessionKey = rcvMessage.sessionAsUUID();
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

        // set the session for all incoming messages.
        request.setSessionAsUUID(this.getSessionKey());

        while (rcvMessage == null && tries < maxRetries) {
            tries++;

            try {
                rcvMessage = rexProConnection.sendMessage(request);
            } catch (Exception ex) {
                String logMessage = "Failure sending message via RexPro. Attempt [" + tries + "] of [" + maxRetries + "].";

                if (tries < maxRetries) {
                    logMessage = logMessage + " Trying again in " + waitMsBetweenTries + " (ms)";
                }

                logger.error(logMessage);

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
                SessionRequestMessage sessionKillMessageToSend = new SessionRequestMessage();
                sessionKillMessageToSend.metaSetKillSession(true);
                sessionKillMessageToSend.setRequestAsUUID(UUID.randomUUID());

                // need to set the session here so that the server knows which one to delete.
                sessionKillMessageToSend.setSessionAsUUID(this.sessionKey);
                final RexProMessage rcvMessage = sendRequest(sessionKillMessageToSend, 3);

                // response message will have an EMPTY_SESSION
                if (rcvMessage instanceof SessionResponseMessage) {
                    this.sessionKey = rcvMessage.sessionAsUUID();
                }

                rexProConnection.close();
                rexProConnection = null;
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
