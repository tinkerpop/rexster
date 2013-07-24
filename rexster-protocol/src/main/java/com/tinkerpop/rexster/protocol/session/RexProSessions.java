package com.tinkerpop.rexster.protocol.session;

import com.tinkerpop.rexster.server.RexsterApplication;
import org.apache.log4j.Logger;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manager for sessions of RexPro.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 * @author Blake Eggleston (bdeggleston.github.com)
 */
public class RexProSessions {
    private static final Logger logger = Logger.getLogger(RexProSessions.class);

    protected final static ConcurrentHashMap<String, RexProSession> sessions = new ConcurrentHashMap<String, RexProSession>();

    public static RexProSession getSession(final String sessionKey) {
        return sessions.get(sessionKey);
    }

    public static void destroySession(final String sessionKey) {
        logger.info(String.format("Try to destroy RexPro Session: %s", sessionKey));

        final RexProSession session = getSession(sessionKey);
        if (session != null) {
            session.kill();
            sessions.remove(sessionKey);
        }

        logger.info(String.format("RexPro Session destroyed or doesn't otherwise exist: %s", sessionKey));
    }

    public static void destroyAllSessions() {
        final Iterator<String> keys = sessions.keySet().iterator();
        while (keys.hasNext()) {
            final String keyToRemove = keys.next();
            destroySession(keyToRemove);
        }
    }

    public static boolean hasSessionKey(final String sessionKey) {
        return sessions.containsKey(sessionKey);
    }

    public static Collection<String> getSessionKeys() {
        return sessions.keySet();
    }

    public static void ensureSessionExists(final String sessionKey, final RexsterApplication rexsterApplication) {
        if (!sessions.containsKey(sessionKey)) {
            final RexProSession session = new RexProSession(sessionKey, rexsterApplication);
            sessions.put(sessionKey, session);

            logger.info(String.format("RexPro Session created: %s", sessionKey));
        }
    }

    /**
     * Creates and returns a new RexProSession, and adds it to the sessions table
     *
     * @param sessionKey
     * @param rexsterApplication
     * @return
     */
    public static RexProSession createSession(final String sessionKey, final RexsterApplication rexsterApplication) {
        final RexProSession session = new RexProSession(sessionKey, rexsterApplication);
        sessions.put(sessionKey, session);

        logger.info(String.format("RexPro Session created: %s", sessionKey));
        return session;
    }
}
