package com.tinkerpop.rexster.protocol;

import com.tinkerpop.rexster.RexsterApplication;
import org.apache.log4j.Logger;

import java.util.Collection;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RexProSessions {
    private static final Logger logger = Logger.getLogger(RexProSession.class);

    protected static ConcurrentHashMap<UUID, RexProSession> sessions = new ConcurrentHashMap<UUID, RexProSession>();

    public static RexProSession getSession(UUID sessionKey) {
        return sessions.get(sessionKey);
    }

    public static void destroySession(UUID sessionKey) {
        sessions.remove(sessionKey);
        logger.info("RexPro Session destroyed: " + sessionKey.toString());
    }

    public static void destroyAllSessions() {
        Iterator<UUID> keys = sessions.keySet().iterator();
        while (keys.hasNext()) {
            UUID keyToRemove = keys.next();
            destroySession(keyToRemove);
            logger.info("RexPro Session destroyed: " + keyToRemove.toString());
        }
    }

    public static boolean hasSessionKey(UUID sessionKey) {
        return sessions.containsKey(sessionKey);
    }

    public static Collection<UUID> getSessionKeys() {
        return sessions.keySet();
    }

    public static void ensureSessionExists(UUID sessionKey, RexsterApplication rexsterApplication) {
        if (!sessions.containsKey(sessionKey)) {
            sessions.put(sessionKey, new RexProSession(sessionKey, rexsterApplication));
            logger.info("RexPro Session created: " + sessionKey.toString());
        }
    }

}
