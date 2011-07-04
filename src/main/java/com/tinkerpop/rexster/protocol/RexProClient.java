package com.tinkerpop.rexster.protocol;

import java.util.List;

public class RexProClient {
    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {

        try {

            RemoteRexster remote = new RemoteRexster();
            remote.put(RemoteRexster.RESERVED_HOST, "localhost");
            remote.put(RemoteRexster.RESERVED_PORT, 8185);
            remote.put(RemoteRexster.RESERVED_LANGUAGE, "gremlin");
            Object retVal = remote.eval("g = rexster.getGraph(\"tinkergraph\");g.V;");

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
