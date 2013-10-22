package com.tinkerpop.rexster.config.hinted;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class HintedGraphs implements Serializable {
    public final Map<String,HintedGraph> graphs = new HashMap<String, HintedGraph>();
}
