package com.tinkerpop.rexster.protocol;

import java.util.List;

public class RexProClient {
    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {

        try {

            RemoteRexster remote = new RemoteRexster("localhost", 8185);
            Object retVal = remote.eval("g = rexster.getGraph(\"tinkergraph\");g.V;", "gremlin");

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
