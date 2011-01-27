package com.tinkerpop.rexster.servlet.gremlin;

import java.util.Collection;

/**
 * Remove Gremlin sessions that have been idle for too long.
 * 
 * Credit to Neo Technology (http://neotechnology.com/) for most of the code related to the 
 * Gremlin Terminal in Rexster.  Specifically, this code was borrowed from 
 * https://github.com/neo4j/webadmin and re-purposed for Rexster's needs.
 * 
 * Based on Webling garbage collector by Pavel A. Yaskevich then re-purposed by 
 * Jacob Hansson <jacob@voltvoodoo.com>
 * 
 */
public class ConsoleGarbageCollector extends Thread {

	long updateInterval = 3000000; // 50 minutes
	long maxIdleInterval = 1790000; // 29 minutes

	ConsoleGarbageCollector() {
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

			Collection<String> sessionIds = ConsoleSessions.getSessionIds();

			for (String sessionId : sessionIds) {
				// Make sure session exists (otherwise
				// GremlinSessions.getSession() will create it)
				if (ConsoleSessions.hasSession(sessionId)) {
					// If idle time is above our threshold
					if (ConsoleSessions.getSession(sessionId).getIdleTime() > maxIdleInterval) {
						// Throw the GremlinSession instance to the wolves
						ConsoleSessions.destroySession(sessionId);
					}
				}
			}
		}
	}

}
