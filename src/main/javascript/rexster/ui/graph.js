/**
 * Manages the graph panel user interface.
 */
Rexster.modules.graph = function(api) {
	
	var mediator = new GraphPanelMediator("#menuGraph", "#panelTraversals", "#panelVertices");
	
	/**
	 * Manages graph panel interactions.
	 */
	function GraphPanelMediator(menuGraph, panelTraversals, panelVertices) {
		var  containerMenuGraph = $(menuGraph),
			 containerPanelTraversals = $(panelTraversals),
			 containerPanelTraversalsList = containerPanelTraversals.find("ul"),
			 containerPanelVertices = $(panelVertices),
			 currentGraphName = "";
		
		if (!(this instanceof GraphPanelMediator)) {
			return new GraphPanelMediator();
		}
		
		this.getCurrentGraphName = function() {
			return currentGraphName;
		}
		
		this.getContainerPanelTraversalsList = function() {
			return containerPanelTraversalsList;
		}
		
		this.getContainerMenuGraph = function() {
			return containerMenuGraph;
		}
		
		this.graphSelectionChanged = function(currentSelectedGraphName) {
			containerPanelTraversals.show();
			containerPanelVertices.hide();
			containerPanelTraversalsList.empty();
			
			currentGraphName = currentSelectedGraphName;
		}
		
		this.resetMenuGraph = function() {
			containerMenuGraph.empty();
		}
	} 
	
	/**
	 * Initializes the graph list.
	 * 
	 * @param initialGraphName {String} The name of the graph that should be marked as selected.
	 * @param onInitComplete   {Function} The callback made when graph initialization is completed. 
	 */
	api.initGraphList = function(state, onInitComplete){
		
		mediator.resetMenuGraph();
		Rexster("ajax", "template", "info", function(api) {
			api.getGraphs(function(result){
				
				var ix = 0,
					max = 0,
				    graphs = [];
				
				// construct a list of graphs that can be pushed into the graph menu
				max = result.graphs.length;
				for (ix = 0; ix < max; ix += 1) {
					graphs.push({ "menuName": result.graphs[ix] });
				}

				api.applyMenuGraphTemplate(graphs, mediator.getContainerMenuGraph());
				
				mediator.getContainerMenuGraph().find("div").hover(function() {
					$(this).toggleClass("ui-state-hover");
				});
				
				mediator.getContainerMenuGraph().find("div").click(function() {
	                var uri = $(this).find("a").attr('href');
	                uri = uri.replace(/^.*#/, '');
					$.history.load(uri);
					return false;
				});
				
				// check the state, if it is at least two items deep then the state 
				// of the graph is also selected and an attempt to make the graph active
				// should be made.
				if (state.length >= 2) {
					mediator.getContainerMenuGraph().find("#graphItem" + state[1]).addClass("ui-state-active");
					
					// load traversals panel for the current graph
					api.getTraversals(state[1], function(traversalResult) { 
						mediator.graphSelectionChanged(state[1]);
						api.applyListTraversalsTemplate(traversalResult.results, mediator.getContainerPanelTraversalsList());
						
						// execute the callback now that the traversals are done.
						onInitComplete();
					},
					function(err){
						api.showMessageError("Could not get the list of traversals from Rexster.");
					});
				}
				
				// if the state does not specify a graph then select the first one. 
				if (state.length < 2) {
					mediator.getContainerMenuGraph().find("#graphItem" + graphs[0].menuName).click();
				}	
				
				/*
				// init the selected item in the graph
				if (state != undefined && $("#graph-" + state).length) {
					mediator.getContainerMenuGraph().accordion("click", "#graph-" + state);
				} else {
					mediator.getContainerMenuGraph().accordion("click", 0);
				}
				
				
				$("#accordionGraph a[_type='vertex']").click(function(){
					$("#panelTraversals").hide();
					$("#panelVertices").show();
					
					var graphName = mediator.getCurrentGraphName();
					api.getVertices(graphName, 0, 49, function(verticesResult) {
						$("#panelVertices").show();
						$("#panelVertices #panelVerticesList").empty();
						
						api.applyListVerticesTemplate(verticesResult.results, "#panelVertices #panelVerticesList");
					},
					function (err){
						api.showMessageError("Could not get the list of vertices from Rexster.");
					});
					
				});
				*/
				
			}, 
			function(err) {
				api.showMessageError("Could not get the list of graphs from Rexster.");
			});
		});

		
	};
};