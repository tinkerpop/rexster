package com.tinkerpop.rexster.protocol;

import java.util.Collection;

/**
 * Monitors RexPro sessions and cleans up ones that have been idle.
 */
public class RexProSessionMonitor extends Thread {
    private long updateInterval;
    private long maxIdleInterval;

    public RexProSessionMonitor(long updateInterval, long maxIdleInterval) {
        if (updateInterval < 1000) {
            updateInterval = 1000;
        }

        if (maxIdleInterval < 1000) {
            maxIdleInterval = 1000;
        }

        this.updateInterval = updateInterval;
        this.maxIdleInterval = maxIdleInterval;

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

            Collection<String> sessionKeys = RexProSessions.getSessionKeys();

            for (String sessionKey : sessionKeys) {
                if (RexProSessions.hasSessionKey(sessionKey)) {
                    // check if the idle time of the session is past the threshold
                    if (RexProSessions.getSession(sessionKey).getIdleTime() > maxIdleInterval) {
                        // Throw the GremlinSession instance to the wolves
                        RexProSessions.destroySession(sessionKey);
                    }
                }
            }
        }
    }
}
