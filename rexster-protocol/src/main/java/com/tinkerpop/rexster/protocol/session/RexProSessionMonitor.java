package com.tinkerpop.rexster.protocol.session;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Monitors RexPro sessions and cleans up ones that have been idle.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 * @author Blake Eggleston (bdeggleston.github.com)
 */
public class RexProSessionMonitor extends Thread {
    private static final long MIN_UPDATE_INTERVAL = 1000;
    private static final long MIN_IDLE_TIME = 60000;

    private final AtomicLong updateInterval = new AtomicLong();
    private final AtomicLong maxIdleTime = new AtomicLong();
    private final AtomicLong lastCheck = new AtomicLong(System.currentTimeMillis());

    /**
     * Create a new monitor.
     */
    public RexProSessionMonitor() {
        setDaemon(true);
        reconfigure(MIN_UPDATE_INTERVAL, MIN_IDLE_TIME);
        start();
    }

    /**
     * Reconfigure the session monitor with new settings.
     */
    public void reconfigure(final long updateInterval, final long maxIdleTime) {
        if (updateInterval < MIN_UPDATE_INTERVAL) {
            this.updateInterval.set(MIN_UPDATE_INTERVAL);
        } else {
            this.updateInterval.set(updateInterval);
        }

        if (maxIdleTime < MIN_IDLE_TIME) {
            this.maxIdleTime.set(MIN_IDLE_TIME);
        } else {
            this.maxIdleTime.set(maxIdleTime);
        }
    }

    @Override
    public void run() {

        while (true) {
            try {
                Thread.sleep(MIN_UPDATE_INTERVAL);
            } catch (InterruptedException e) {
            }

            if (updateInterval.get() > (System.currentTimeMillis() - lastCheck.get())) {
                final Collection<String> sessionKeys = RexProSessions.getSessionKeys();
                for (String sessionKey : sessionKeys) {
                    if (RexProSessions.hasSessionKey(sessionKey)) {
                        // check if the idle time of the session is past the threshold
                        if (RexProSessions.getSession(sessionKey).getIdleTime() > maxIdleTime.get()) {
                            // Throw the GremlinSession instance to the wolves
                            RexProSessions.destroySession(sessionKey);
                        }
                    }
                }

                lastCheck.set(System.currentTimeMillis());
            }
        }
    }
}
