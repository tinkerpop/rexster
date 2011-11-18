/**
 * Provides methods to access REST services within Rexster.
 */
Rexster.modules.ajax = function(api) {

	var rexsterMimeType = "application/vnd.rexster-v1+json";
	var baseUri = "";
	
	Rexster("server", function(api) {
		// currently only one of these
		baseUri = api.getBaseUri(0);
	});
	
	/**
	 * Get a list of graphs.
	 * 
	 * @param onSuccess	{Function} The action that occurs on a successful REST call.
	 * @param onFail 	{Function} The action that occurs on a failed REST call.
	 */
	api.getGraphs = function(onSuccess, onFail){
		$.ajax({
			  url: baseUri,
			  accepts:{
			    json: rexsterMimeType
			  },
			  type: "GET",
			  dataType:"json",
			  success: onSuccess,
			  error: onFail
			});
	};
	
	/**
	 * Get a specific graph.
	 * 
	 * @param graphName {String} The name of the graph.
	 * @param onSuccess	{Function} The action that occurs on a successful REST call.
	 * @param onFail 	{Function} The action that occurs on a failed REST call.
	 */
	api.getGraph = function(graphName, onSuccess, onFail){
		$.ajax({
			  url: baseUri + graphName,
			  accepts:{
			    json: rexsterMimeType
			  },
			  type: "GET",
			  dataType:"json",
			  success: onSuccess,
			  error: onFail
			});
	};
	
	/**
	 * Get a list of vertices for a specific graph.
	 * 
	 * @param graphName {String} the name of the graph.
	 * @param start		{int} The first vertex to return in the set.
	 * @param end		{int} The last vertex to return in the set.
	 * @param onSuccess	{Function} The action that occurs on a successful REST call.
	 * @param onFail 	{Function} The action that occurs on a failed REST call.
	 */
	api.getVertices = function(graphName, start, end, onSuccess, onFail){
		$.ajax({
			  url: baseUri + graphName + "/vertices?rexster.offset.start=" + start + "&rexster.offset.end=" + end,
			  accepts:{
			    json: rexsterMimeType
			  },
			  type: "GET",
			  dataType:"json",
			  success: onSuccess,
			  async:false,
			  error: onFail
			});
	};
	
	/**
	 * Get a list of edges for a specific graph.
	 * 
	 * @param graphName {String} the name of the graph.
	 * @param start		{int} The first edge to return in the set.
	 * @param end		{int} The last edge to return in the set.
	 * @param onSuccess	{Function} The action that occurs on a successful REST call.
	 * @param onFail 	{Function} The action that occurs on a failed REST call.
	 */
	api.getEdges = function(graphName, start, end, onSuccess, onFail){
		$.ajax({
			  url: baseUri + graphName + "/edges?rexster.offset.start=" + start + "&rexster.offset.end=" + end,
			  accepts:{
			    json: rexsterMimeType
			  },
			  type: "GET",
			  dataType:"json",
			  success: onSuccess,
			  async:false,
			  error: onFail
			});
	};
	
	api.getVertexEdges = function(graphName, vertex, onSuccess, onFail, asynchronous) {
		$.ajax({
			  url: baseUri + graphName + "/vertices/" + vertex + "/bothE",
			  accepts:{
			    json: rexsterMimeType
			  },
			  type: "GET",
			  dataType:"json",
			  success: onSuccess,
			  async:asynchronous,
			  error: onFail
			});
	};
	
	api.getVertexInEdges = function(graphName, vertex, onSuccess, onFail, asynchronous) {
		$.ajax({
			  url: baseUri + graphName + "/vertices/" + vertex + "/inE",
			  accepts:{
			    json: rexsterMimeType
			  },
			  type: "GET",
			  dataType:"json",
			  success: onSuccess,
			  async:asynchronous,
			  error: onFail
			});
	};
	
	api.getVertexOutEdges = function(graphName, vertex, onSuccess, onFail, asynchronous) {
		$.ajax({
			  url: baseUri + graphName + "/vertices/" + vertex + "/outE",
			  accepts:{
			    json: rexsterMimeType
			  },
			  type: "GET",
			  dataType:"json",
			  success: onSuccess,
			  async:asynchronous,
			  error: onFail
			});
	};
	
	api.getVertexElement = function(graphName, vertex, onSuccess, onFail, asynchronous) {
		$.ajax({
			  url: baseUri + graphName + "/vertices/" + vertex,
			  accepts:{
			    json: rexsterMimeType
			  },
			  type: "GET",
			  dataType:"json",
			  success: onSuccess,
			  async:asynchronous,
			  error: onFail
			});
	};
	
	api.getEdgeElement = function(graphName, edge, onSuccess, onFail, asynchronous) {
		$.ajax({
			  url: baseUri + graphName + "/edges/" + edge,
			  accepts:{
			    json: rexsterMimeType
			  },
			  type: "GET",
			  dataType:"json",
			  success: onSuccess,
			  async:asynchronous,
			  error: onFail
			});
	};
	
	api.getVertexCenteredGraph = function(graphName, vertex, degrees, onSuccess, onFail) {
		var graph = getVertexCenteredGraphByDegree(api, graphName, vertex, degrees, null);
		onSuccess(graph);
	};
	
	function getVertexCenteredGraphByDegree(api, graphName, sourceVertex, degrees, graph) {
		if (graph == undefined || graph == null) {
			graph = { vertices: new Array(), edges: new Array() };
		}
		
		var tempVertices = new Array();
		
		api.getVertexEdges(graphName, sourceVertex, function(results) {
			var edges = results.results;
			for (var ix = 0; ix < edges.length; ix++) {
				
				if (!hasId(graph.vertices, edges[ix]._outV)) {
					graph.vertices.push({id:edges[ix]._outV, nodeName:edges[ix]._outV, group:1});
					tempVertices.push({id:edges[ix]._outV, nodeName:edges[ix]._outV, group:1})
				}
				
				if (!hasId(graph.vertices, edges[ix]._inV)) {
					graph.vertices.push({id:edges[ix]._inV, nodeName:edges[ix]._inV, group:1});
					tempVertices.push({id:edges[ix]._inV, nodeName:edges[ix]._inV, group:1})
				}
			}
			
			for (var iz = 0; iz < edges.length; iz++) {
				if (!hasEdge(graph.edges, edges[iz])) {
					graph.edges.push({id:edges[iz]._id, source: findIndex(graph.vertices, edges[iz]._outV), target:findIndex(graph.vertices, edges[iz]._inV)});
				}
			}
		}, 
		function(err) {
			
		},
		false);
		
		if ((degrees - 1) > 0) {
			for (var iy = 0; iy < tempVertices.length; iy++) {
				getVertexCenteredGraphByDegree(api, graphName, tempVertices[iy].id, degrees - 1, graph);
			}
		}
		
		return graph;
	}
	
	function hasId(list, value) {
		for (var ix = 0; ix < list.length; ix++) {
			if (list[ix].id === value) {
				return true;
			}
		}
		
		return false;
	}
	
	function hasEdge(list, edge) {
		for (var ix = 0; ix < list.length; ix++) {
			if (list[ix].source === edge._outV && list[ix].target === edge._inV) {
				return true;
			}
		}
		
		return false;
	}
	
	function findIndex(list, value) {
		for (var ix = 0; ix < list.length; ix++) {
			if (list[ix].id === value) {
				return ix;
			}
		}
		
		return -1;
	}
};