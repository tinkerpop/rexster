package com.tinkerpop.rexster.protocol;

import javax.script.Bindings;
import javax.script.ScriptContext;
import java.util.List;

public class RexProClient {
    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {

        try {

            RexsterScriptEngine remote = new RexsterScriptEngine();
            remote.put(RexsterScriptEngine.CONFIG_SCOPE_HOST, "localhost");
            remote.put(RexsterScriptEngine.CONFIG_SCOPE_PORT, 8185);
            remote.put(RexsterScriptEngine.CONFIG_SCOPE_LANGUAGE, "gremlin");

            Bindings bindings = remote.createBindings();
            bindings.put("x", "test");

            remote.setBindings(bindings, ScriptContext.ENGINE_SCOPE);
            remote.setBindings(bindings, ScriptContext.GLOBAL_SCOPE);

            Object retVal = remote.eval("g = rexster.getGraph(\"tinkergraph\");g.V;x;");

            if (retVal instanceof List) {
                List list = (List) retVal;
                for (Object item : list) {
                    System.out.println(item.toString() + " [" + item.getClass().getName() + "]");
                }
            } else {
                System.out.println(retVal.toString() + " [" + retVal.getClass().getName() + "]");
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
