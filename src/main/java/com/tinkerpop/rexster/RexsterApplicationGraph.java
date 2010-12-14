package com.tinkerpop.rexster;

import com.tinkerpop.blueprints.pgm.Graph;
import com.tinkerpop.rexster.traversals.Traversal;

import java.util.*;

/**
 * Holds a graph, its assigned traversals and packages.
 */
public class RexsterApplicationGraph {

    private Graph graph;
    private Map<String, Class<? extends Traversal>> loadedTraversals = new HashMap<String, Class<? extends Traversal>>();
    private String graphName;
    private Set<String> packageNames;

    public RexsterApplicationGraph(String graphName, Graph graph) {
        this.graphName = graphName;
        this.graph = graph;
    }

    public RexsterApplicationGraph(String graphName, Graph graph, String packageNames) {
        this.graphName = graphName;
        this.graph = graph;
        this.loadPackageNames(packageNames);
    }

    public boolean hasPackages() {
        return this.packageNames != null && this.packageNames.size() > 0;
    }

    public String getGraphName() {
        return graphName;
    }

    public Map<String, Class<? extends Traversal>> getLoadedTraversals() {
        return loadedTraversals;
    }

    public void setLoadedTraversals(Map<String, Class<? extends Traversal>> loadedTraversals) {
        this.loadedTraversals = loadedTraversals;
    }

    public Graph getGraph() {
        return graph;
    }

    public Set<String> getPackageNames() {
        return packageNames;
    }

    public void loadPackageNames(String packageNameString) {
        if (packageNameString != null && packageNameString.length() > 0) {
            if (packageNameString.trim().equals(";")) {
                // allows configuration of the root package only (ie. gremlin)
                this.packageNames = new HashSet<String>();
                this.packageNames.add("");
            } else {
                // allows configuration of the root package plus anything else
                this.packageNames = new HashSet<String>(Arrays.asList(packageNameString.split(";")));
            }
        } else {
            // no packages when empty
            this.packageNames = new HashSet<String>();
        }
    }
}
