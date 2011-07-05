package com.tinkerpop.rexster.protocol;

import javax.script.Bindings;
import javax.script.ScriptContext;
import java.util.Iterator;
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

            System.out.println("before");
            dumpBindings(bindings);

            Object retVal = remote.eval("g = rexster.getGraph(\"tinkergraph\");g.V;x=\"testing\";");

            if (retVal instanceof Iterator) {
                Iterator list = (Iterator) retVal;
                while (list.hasNext()) {
                    Object item = list.next();
                    System.out.println(item.toString() + " [" + item.getClass().getName() + "]");
                }
            } else {
                System.out.println(retVal.toString() + " [" + retVal.getClass().getName() + "]");
            }

            System.out.println("after");
            dumpBindings(bindings);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void dumpBindings(Bindings bindings) {
        if (bindings == null)
          System.out.println("  No bindings");
        else
          for (String key : bindings.keySet())
            System.out.println("  " + key + ": " + bindings.get(key));
        System.out.println();
      }
}
