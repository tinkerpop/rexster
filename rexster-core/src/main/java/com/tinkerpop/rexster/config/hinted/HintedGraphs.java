package com.tinkerpop.rexster.config.hinted;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class HintedGraphs implements Serializable {
    public final Map<String,HintedGraph> graphs = new HashMap<String, HintedGraph>();

    /*
     * Guava's helpers could make this simpler
     */
    @Override
    public String toString() {
        final String g;

        if (null == graphs) {
            g = "null";
        } else {
            StringBuilder sb = new StringBuilder();
            boolean tail = false;
            for (Map.Entry<String, HintedGraph> ent : graphs.entrySet()) {
                if (tail)
                    sb.append(", ");

                sb.append(ent.getKey());
                sb.append("=");
                sb.append(ent.getValue());
                tail = true;
            }
            g = sb.toString();
        }

        return "HintedGraphs[" + g + "]";
    }
}
