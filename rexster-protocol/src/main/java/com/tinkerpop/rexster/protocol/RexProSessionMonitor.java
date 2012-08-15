package com.tinkerpop.rexster.protocol;

import java.util.Collection;

/**
 * Monitors RexPro sessions and cleans up ones that have been idle.
 */
public class RexProSessionMonitor extends Thread {
    private static final long MIN_UPDATE_INTERVAL = 1000;
    private static final long MIN_IDLE_TIME = 60000;

    private final long updateInterval;
    private final long maxIdleTime;

    /**
     * Create a new monitor.
     *
     * @param updateInterval time in milliseconds between checking the idle time of sessions.
     * @param maxIdleTime time in milliseconds that a session can stay idle before being closed.
     */
    public RexProSessionMonitor(final long updateInterval, final long maxIdleTime) {
        if (updateInterval < MIN_UPDATE_INTERVAL) {
            this.updateInterval = MIN_UPDATE_INTERVAL;
        } else {
            this.updateInterval = updateInterval;
        }

        if (maxIdleTime < MIN_IDLE_TIME) {
            this.maxIdleTime = MIN_IDLE_TIME;
        } else {
            this.maxIdleTime = maxIdleTime;
        }

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

            final Collection<String> sessionKeys = RexProSessions.getSessionKeys();

            for (String sessionKey : sessionKeys) {
                if (RexProSessions.hasSessionKey(sessionKey)) {
                    // check if the idle time of the session is past the threshold
                    if (RexProSessions.getSession(sessionKey).getIdleTime() > maxIdleTime) {
                        // Throw the GremlinSession instance to the wolves
                        RexProSessions.destroySession(sessionKey);
                    }
                }
            }
        }
    }
}
