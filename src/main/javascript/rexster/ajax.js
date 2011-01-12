/**
 * Provides methods to access REST services within Rexster.
 */
Rexster.modules.ajax = function(api) {
	
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
			  type: "GET",
			  dataType:"json",
			  success: onSuccess,
			  async:false,
			  error: onFail
			});
	};
	
	/**
	 * Get a list of traversals for a specific graph.
	 * 
	 * @param graphName	{String} The name of the graph.
	 * @param onSuccess	{Function} The action that occurs on a successful REST call.
	 * @param onFail 	{Function} The action that occurs on a failed REST call.
	 */
	
	api.getTraversals = function(graphName, onSuccess, onFail){
		$.ajax({
			  url: baseUri + graphName + "/traversals",
			  type: "GET",
			  dataType:"json",
			  success: onSuccess,
			  async:false,
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
			  type: "GET",
			  dataType:"json",
			  success: onSuccess,
			  async:false,
			  error: onFail
			});
	};
};