/**
 * Manages the graph panel user interface.
 */
Rexster.modules.graph = function(api) {
	
	var mediator = new GraphPanelMediator("#menuGraph", "#panelTraversals", "#panelVertices", "#panelGraphMenu"),
	    currentGraph;
	
	/**
	 * Manages graph panel interactions.
	 */
	function GraphPanelMediator(menuGraph, panelTraversals, panelVertices, panelGraphMenu) {
		var  containerMenuGraph = $(menuGraph),
			 containerPanelTraversals = $(panelTraversals),
			 containerPanelTraversalsList = containerPanelTraversals.find("ul"),
			 containerPanelVertices = $(panelVertices),
			 containerPanelGraphMenu = $(panelGraphMenu),
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
		
		this.getContainerPanelGraphMenu = function() {
			return containerPanelGraphMenu;
		}
		
		this.getContainerMenuGraph = function() {
			return containerMenuGraph;
		}
		
		this.graphSelectionChanged = function(api, currentSelectedGraphName, onComplete) {
			
			// keep track of the currently selected graph.
			currentGraph = currentSelectedGraphName;
			
			containerMenuGraph.find(".graph-item").removeClass("ui-state-active");
			containerMenuGraph.find("#graphItem" + currentSelectedGraphName).addClass("ui-state-active");
			
			// modify the links on the browse menus to match current state
			containerPanelGraphMenu.find("a[_type='vertices']").attr("href", "/main/" + currentSelectedGraphName + "/vertices");
			containerPanelGraphMenu.find("a[_type='edges']").attr("href", "/main/" + currentSelectedGraphName + "/edges");
			
			// load the graph profile
			api.getGraph(currentSelectedGraphName, function(graphResult) {
				$("#panelGraphTitle").text(currentSelectedGraphName);
				$("#panelGraphDetail").text(graphResult.graph);
			},
			function(err){
				api.showMessageError("Could not get the graph profile from Rexster.");
			});
			
			// load traversals panel for the current graph
			api.getTraversals(currentSelectedGraphName, function(traversalResult) { 
				
				containerPanelTraversals.show();
				containerPanelVertices.hide();
				containerPanelTraversalsList.empty();
				
				currentGraphName = currentSelectedGraphName;
				
				api.applyListTraversalsTemplate(traversalResult.results, containerPanelTraversalsList);
				
				// execute the callback now that the traversals are done.
				onComplete();
			},
			function(err){
				api.showMessageError("Could not get the list of traversals from Rexster.");
			});
		}
		
		this.panelGraphNavigationSelected = function(navigation) {
			return;
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
				
				mediator.getContainerPanelGraphMenu().find("a").button({ icons: {primary:"ui-icon-search"}});
				mediator.getContainerPanelGraphMenu().find("a").click(function(evt) {
					evt.preventDefault();
	                var uri = $(this).attr('href');
	                window.history.pushState({"uri":uri}, '', uri);
					mediator.panelGraphNavigationSelected($(this).attr("_type"));
				});
				
				mediator.getContainerMenuGraph().find("div").hover(function() {
					$(this).toggleClass("ui-state-hover");
				});
				
				mediator.getContainerMenuGraph().find("div").click(function(evt) {
					evt.preventDefault();
					var selectedLink = $(this).find("a"); 
	                var uri = selectedLink.attr('href');
	                window.history.pushState({"uri":uri}, '', uri);
	                
	                mediator.graphSelectionChanged(api, selectedLink.text(), onInitComplete);
				});
				
				// check the state, if it is at least two items deep then the state 
				// of the graph is also selected and an attempt to make the graph active
				// should be made.
				if (state.hasOwnProperty("graph")) {
	                mediator.graphSelectionChanged(api, state.graph, onInitComplete);
				}
				
				// if the state does not specify a graph then select the first one. 
				if (!state.hasOwnProperty("graph")) {
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