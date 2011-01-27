package com.tinkerpop.rexster.servlet.gremlin;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;

import com.tinkerpop.blueprints.pgm.Graph;
import com.tinkerpop.gremlin.jsr223.GremlinScriptEngine;
import com.tinkerpop.rexster.WebServer;

/**
 * Builds Gremlin evaluators.
 * 
 * Credit to Neo Technology (http://neotechnology.com/) for most of the code related to the 
 * Gremlin Terminal in Rexster.  Specifically, this code was borrowed from 
 * https://github.com/neo4j/webadmin and re-purposed for Rexster's needs.
 * 
 * Original author Jacob Hansson <jacob@voltvoodoo.com>
 * 
 */
@SuppressWarnings("restriction")
public class GremlinFactory {

	protected volatile static boolean initiated = false;

	public static ScriptEngine createGremlinScriptEngine(String graphName) {
		try {
			ScriptEngine engine = new GremlinScriptEngine();
			Graph graph = WebServer.getRexsterApplication().getGraph(graphName);
			engine.getBindings(ScriptContext.ENGINE_SCOPE).put("g", graph);

			/*
			 * try { engine.getBindings( ScriptContext.ENGINE_SCOPE ).put( "$_",
			 * graph.getVertex(0l)); } catch ( Exception e ) { // Om-nom-nom }
			 */

			return engine;
		} catch (Throwable e) {
			// Pokemon catch b/c fails here get hidden until the server exits.
			e.printStackTrace();
			return null;
		}
	}

	protected synchronized void ensureInitiated() {
		if (initiated == false) {
			new ConsoleGarbageCollector();
		}
	}
}
