package com.tinkerpop.rexster.gremlin;

import java.util.Collection;

/**
 * Remove Gremlin sessions that have been idle for too long.
 * <p/>
 * Credit to Neo Technology (http://neotechnology.com/) for most of the code related to the
 * Gremlin in Rexster.  Specifically, this code was borrowed from
 * https://github.com/neo4j/webadmin and re-purposed for Rexster's needs.
 * <p/>
 * Based on Webling garbage collector by Pavel A. Yaskevich then re-purposed by
 * Jacob Hansson <jacob@voltvoodoo.com>
 */
public class GremlinGarbageCollector extends Thread {

    long updateInterval = 3000000; // 50 minutes
    long maxIdleInterval = 1790000; // 29 minutes

    public GremlinGarbageCollector() {
        setDaemon(true);
        start();
    }

    @Override
    public void run() {

        while (true) {
            try {
                Thread.sleep(updateInterval);
            } catch (InterruptedException e) {
            }

            Collection<String> sessionKeys = GremlinSessions.getSessionKeys();

            for (String sessionKey : sessionKeys) {
                // Make sure session exists (otherwise
                // GremlinSessions.getSession() will create it)
                if (GremlinSessions.hasSessionKey(sessionKey)) {
                    // If idle time is above our threshold
                    if (GremlinSessions.findSessionByKey(sessionKey).getIdleTime() > maxIdleInterval) {
                        // Throw the GremlinSession instance to the wolves
                        GremlinSessions.destroySession(sessionKey);
                    }
                }
            }
        }
    }

}
