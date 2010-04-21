package com.tinkerpop.rexster.traversals;

import com.tinkerpop.blueprints.pgm.Vertex;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.Collection;
import java.util.Map;

/**
 * @author: Marko A. Rodriguez (http://markorodriguez.com)
 */
public class GremlinTraversal extends AbstractTraversal {

    private ScriptEngine engine = new ScriptEngineManager().getEngineByName(GREMLIN);

    private static final String GREMLIN = "gremlin";
    private static final String ROOT_VARIABLE = "$_";
    private static final String GRAPH_VARIABLE = "$_g";
    private static final String VERTICES = "vertices";
    private static final String SCRIPT = "script";
    private static final String RETURN = "return";

    public String getResourceName() {
        return GREMLIN;
    }

    public void traverse() {
        try {
            String script = this.requestObject.get(SCRIPT).toString();
            if (this.resultObject.containsKey(VERTICES)) {
                Collection<Vertex> roots = getVertices(graph, (Map) this.resultObject.get(VERTICES));
                if (null != roots)
                    engine.getBindings(ScriptContext.ENGINE_SCOPE).put(ROOT_VARIABLE, roots);
            }

            engine.getBindings(ScriptContext.ENGINE_SCOPE).put(GRAPH_VARIABLE, graph);

            this.resultObject.put(RETURN, engine.eval(script));
            this.success = true;
        } catch (ScriptException e) {
            this.success = false;
            this.message = e.getMessage();
        }
    }
}

