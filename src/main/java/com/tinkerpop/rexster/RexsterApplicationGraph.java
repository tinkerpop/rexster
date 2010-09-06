package com.tinkerpop.rexster;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.tinkerpop.blueprints.pgm.Graph;

/**
 * Holds a graph, its assigned traversals and packages.
 */
public class RexsterApplicationGraph {
	/**
	 * The graph that is loaded to Rexster. 
	 */
	private Graph graph;
	
	/**
	 * The list of traversals loaded for this graph.
	 */
	private Map<String, Class> loadedTraversals = new HashMap<String, Class>();
	
	/**
	 * The name of the graph, which should be unique for the Rexster instance.
	 */
	private String graphName;
	
	private Set<String> packageNames;

	/**
	 * Create a new object.
	 * @param graphName The name of the graph.
	 */
	public RexsterApplicationGraph(String graphName) {
		this.graphName = graphName;
	}
	
	/**
	 * Create a new object.
	 * @param graphName The name of the graph.
	 * @param packageNames The package names in comma delimited format.
	 */
	public RexsterApplicationGraph(String graphName, String packageNames) {
		this.graphName = graphName;
		this.loadPackageNames(packageNames);
	}
	
	/**
	 * Determines if there are any package names configured for this graph.
	 * @return True if there are packages and false otherwise.
	 */
	public boolean hasPackages(){
		return this.packageNames != null && this.packageNames.size() > 0;
	}

	public String getGraphName() {
		return graphName;
	}

	public void setGraphName(String graphName) {
		this.graphName = graphName;
	}

	public Map<String, Class> getLoadedTraversals() {
		return loadedTraversals;
	}

	public void setLoadedTraversals(Map<String, Class> loadedTraversals) {
		this.loadedTraversals = loadedTraversals;
	}

	public Graph getGraph() {
		return graph;
	}

	public void setGraph(Graph graph) {
		this.graph = graph;
	}

	public Set<String> getPackageNames() {
		return packageNames;
	}

	public void setPackageNames(Set<String> packageNames) {
		this.packageNames = packageNames;
	}
	
	public void loadPackageNames(String packageNameString) {
		this.packageNames = new HashSet<String>(Arrays.asList(packageNameString.split(";")));
	}
}
