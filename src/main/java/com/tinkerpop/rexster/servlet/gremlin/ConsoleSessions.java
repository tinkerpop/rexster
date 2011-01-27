package com.tinkerpop.rexster.servlet.gremlin;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps track of currently running gremlin sessions. Each one is associated
 * with a web client.
 * 
 * Credit to Neo Technology (http://neotechnology.com/) for most of the code related to the 
 * Gremlin Terminal in Rexster.  Specifically, this code was borrowed from 
 * https://github.com/neo4j/webadmin and re-purposed for Rexster's needs.
 * 
 * Original author Jacob Hansson <jacob@voltvoodoo.com>
 * 
 */
public class ConsoleSessions {

	protected static ConcurrentHashMap<String, ConsoleSession> sessions = new ConcurrentHashMap<String, ConsoleSession>();

	/**
	 * Gets a GremlinSesssion for a given sessionId, creating a GremlinSession
	 * if one does not exist.
	 */
	public static ConsoleSession getSession(String sessionId, String graphName) {
		ensureSessionExists(sessionId, graphName);
		return sessions.get(sessionId);
	}
	
	public static ConsoleSession getSession(String sessionId) {
		return sessions.get(sessionId);
	}

	public static void destroySession(String sessionId) {
		sessions.get(sessionId).die();
		sessions.remove(sessionId);
	}

	public static void destroyAllSessions() {
		Iterator<String> keys = sessions.keySet().iterator();
		while (keys.hasNext()) {
			destroySession(keys.next());
		}
	}

	public static boolean hasSession(String sessionId) {
		return sessions.containsKey(sessionId);
	}

	public static Collection<String> getSessionIds() {
		return sessions.keySet();
	}

	protected static void ensureSessionExists(String sessionId, String graphName) {
		if (!sessions.containsKey(sessionId)) {
			sessions.put(sessionId, new ConsoleSession(graphName));
		}
	}

}
