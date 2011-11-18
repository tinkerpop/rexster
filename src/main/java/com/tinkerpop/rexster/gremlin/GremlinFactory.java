package com.tinkerpop.rexster.gremlin;

import com.tinkerpop.gremlin.groovy.jsr223.GremlinGroovyScriptEngine;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.SimpleBindings;
import java.util.Map;

/**
 * Builds Gremlin evaluators.
 * <p/>
 * Credit to Neo Technology (http://neotechnology.com/) for most of the code related to the
 * Gremlin in Rexster.  Specifically, this code was borrowed from
 * https://github.com/neo4j/webadmin and re-purposed for Rexster's needs.
 * <p/>
 * Original author Jacob Hansson <jacob@voltvoodoo.com>
 */
@SuppressWarnings("restriction")
public class GremlinFactory {

    protected volatile static boolean initiated = false;

    public static ScriptEngine createGremlinScriptEngine(Map<String, Object> context) {
        try {
            ScriptEngine engine = new GremlinGroovyScriptEngine();

            Bindings bindings = new SimpleBindings();
            bindings.putAll(context);

            engine.setBindings(bindings, ScriptContext.ENGINE_SCOPE);

            return engine;
        } catch (Throwable e) {
            // Pokemon catch b/c fails here get hidden until the server exits.
            e.printStackTrace();
            return null;
        }
    }

    protected synchronized void ensureInitiated() {
        if (initiated == false) {
            new GremlinGarbageCollector();
        }
    }
}
