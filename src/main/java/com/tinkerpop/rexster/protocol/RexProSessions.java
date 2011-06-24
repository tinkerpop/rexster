package com.tinkerpop.rexster.protocol;

import com.tinkerpop.rexster.RexsterApplicationProvider;
import com.tinkerpop.rexster.gremlin.GremlinSession;

import java.util.Collection;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RexProSessions {

    protected static ConcurrentHashMap<UUID, RexProSession> sessions = new ConcurrentHashMap<UUID, RexProSession>();

    public static RexProSession getSession(UUID sessionKey) {
        ensureSessionExists(sessionKey);
        return sessions.get(sessionKey);
    }

    public static void destroySession(UUID sessionKey) {
        sessions.remove(sessionKey);
    }

    public static void destroyAllSessions() {
        Iterator<UUID> keys = sessions.keySet().iterator();
        while (keys.hasNext()) {
            destroySession(keys.next());
        }
    }

    public static boolean hasSessionKey(UUID sessionKey) {
        return sessions.containsKey(sessionKey);
    }

    public static Collection<UUID> getSessionKeys() {
        return sessions.keySet();
    }

    private static void ensureSessionExists(UUID sessionKey) {
        if (!sessions.containsKey(sessionKey)) {
            sessions.put(sessionKey, new RexProSession());
        }
    }

}
