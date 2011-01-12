/**
 * Manages the graph panel user interface.
 */
Rexster.modules.graph = function(api) {
	
	var mediator = new GraphPanelMediator("#accordionGraph", "#panelTraversals", "#panelVertices");
	
	/**
	 * Manages graph panel interactions.
	 */
	function GraphPanelMediator(accordionGraph, panelTraversals, panelVertices) {
		var  containerAccordionGraph = $(accordionGraph),
			 containerPanelTraversals = $(panelTraversals),
			 containerPanelTraversalsList = containerPanelTraversals.find("ul"),
			 containerPanelVertices = $(panelVertices);
		
		if (!(this instanceof GraphPanelMediator)) {
			return new GraphPanelMediator();
		}
		
		this.containerPanelTraversalsList = containerPanelTraversalsList;
		this.containerAccordionGraph = containerAccordionGraph;
		
		this.accordionLoaded = function(t) {
			containerPanelTraversals.show();
			containerPanelVertices.hide();
			containerPanelTraversalsList.empty();
		};
	} 
	
	api.initGraphAccordion = function(){
		
		Rexster("ajax", "template", "info", function(api) {
			api.getGraphs(function(result){
				
				var ix = 0,
					max = 0,
				    graphs = [];
				
				// construct a list of graphs that can be pushed into the graph accordion
				max = result.graphs.length;
				for (ix = 0; ix < max; ix += 1) {
					// synchronous
					api.getGraph(result.graphs[ix], function(graphResult) {
						graphs.push({ "graphName": result.graphs[ix], "graphDescription":graphResult.graph});
					},
					function(err){
						api.showMessageError("Could not get the list of graphs from Rexster.");
					});
				}

				api.applyAccordionGraphTemplate(graphs, mediator.containerAccordionGraph);
				
				mediator.containerAccordionGraph.accordion({
					autoHeight: false,
					active: false,
					change: function(event, ui) {
						
						// load traversals panel if the user changes graphs.
						api.getTraversals(ui.newHeader.text(), function(traversalResult) { 
							mediator.accordionLoaded();
							api.applyListTraversalsTemplate(traversalResult.results, mediator.containerPanelTraversalsList);
						},
						function(err){
							api.showMessageError("Could not get the list of traversals from Rexster.");
						});
					}
				});
				
				// init the first item in the graph
				mediator.containerAccordionGraph.accordion("activate", 0);
				
				$("#accordionGraph a[_type='vertex']").click(function(){
					$("#panelTraversals").hide();
					$("#panelVertices").show();
					
					var graphName = $(this).attr("_graph");
					api.getVertices(graphName, 0, 99, function(verticesResult) {
						$("#panelVertices").show();
						$("#panelVertices ul").empty();
						
						// yuck
						$("#panelVertices").attr("_graph", graphName);
						
						api.applyListVerticesTemplate(verticesResult.results, "#panelVertices ul");
					},
					function (err){
						api.showMessageError("Could not get the list of vertices from Rexster.");
					});
					
				});
				
			}, 
			function(err) {
				api.showMessageError("Could not get the list of graphs from Rexster.");
			});
		});

		
	};
};