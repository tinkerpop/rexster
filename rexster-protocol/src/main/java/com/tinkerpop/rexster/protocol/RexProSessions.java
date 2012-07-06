package com.tinkerpop.rexster.protocol;

import com.tinkerpop.rexster.RexsterApplication;
import org.apache.log4j.Logger;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

public class RexProSessions {
    private static final Logger logger = Logger.getLogger(RexProSessions.class);

    protected static ConcurrentHashMap<String, RexProSession> sessions = new ConcurrentHashMap<String, RexProSession>();

    public static RexProSession getSession(String sessionKey) {
        return sessions.get(sessionKey);
    }

    public static void destroySession(String sessionKey) {
        sessions.remove(sessionKey);
        logger.info("RexPro Session destroyed: " + sessionKey);
    }

    public static void destroyAllSessions() {
        Iterator<String> keys = sessions.keySet().iterator();
        while (keys.hasNext()) {
            String keyToRemove = keys.next();
            destroySession(keyToRemove);
        }
    }

    public static boolean hasSessionKey(String sessionKey) {
        return sessions.containsKey(sessionKey);
    }

    public static Collection<String> getSessionKeys() {
        return sessions.keySet();
    }

    public static void ensureSessionExists(String sessionKey, RexsterApplication rexsterApplication, byte sessionChannel, int chunkSize) {
        if (!sessions.containsKey(sessionKey)) {

            RexProSession session = new RexProSession(sessionKey, rexsterApplication, sessionChannel, chunkSize);
            if (session == null) {
                logger.warn("A RexPro Session could not be created because the requested channel is not valid.");
                throw new RuntimeException("Requested channel is not valid.");
            }

            sessions.put(sessionKey, session);
            logger.info("RexPro Session created: " + sessionKey.toString());
        }
    }

}
