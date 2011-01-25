/**
 * Manages the graph panel user interface.
 */
Rexster.modules.graph = function(api) {
	
	var mediator = new GraphPanelMediator("#menuGraph", "#panelTraversals", "#panelGraphMenu", "#panelBrowser", "#panelBrowserMain"),
	    currentGraph;
	
	/**
	 * Manages graph panel interactions.
	 */
	function GraphPanelMediator(menuGraph, panelTraversals, panelGraphMenu, panelBrowser, panelBrowserMain) {
		var  containerMenuGraph = $(menuGraph),
	         containerPanelBrowser = $(panelBrowser),
		     containerPanelBrowserMain = $(panelBrowserMain),
			 containerPanelTraversals = $(panelTraversals),
			 containerPanelTraversalsList = containerPanelTraversals.find("ul"),
			 containerPanelGraphMenu = $(panelGraphMenu),
			 currentGraphName = "",
			 currentPageStart = 0,
			 currentTotal = 0,
			 currentFeatureBrowsed = "",
			 pageSize = 10;
		
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
		
		this.getContainerPanelBrowser = function() {
			return containerPanelBrowser;
		}
		
		this.graphSelectionChanged = function(api, currentSelectedGraphName, onComplete) {
			
			// keep track of the currently selected graph.
			currentGraph = currentSelectedGraphName;
			
			containerMenuGraph.find(".graph-item").removeClass("ui-state-active");
			containerMenuGraph.find("#graphItem" + currentSelectedGraphName).addClass("ui-state-active");
			
			// modify the links on the browse menus to match current state
			containerPanelGraphMenu.find("a[_type='vertices']").attr("href", "/main/graph/" + currentSelectedGraphName + "/vertices?start=0&end=10");
			containerPanelGraphMenu.find("a[_type='edges']").attr("href", "/main/graph/" + currentSelectedGraphName + "/edges?start=0&end=10");
			
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
				containerPanelBrowser.hide();
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
		
		this.panelGraphNavigationPagedPrevious = function(api, sender) {
			var start = currentPageStart - pageSize,
			    end = currentPageStart;
			
			if (start < 0) {
				this.panelGraphNavigationPagedFirst(api, sender);
			} else {
				this.panelGraphNavigationPaged(api, start, end, sender);
			}
		}
		
		this.panelGraphNavigationPagedNext = function(api, sender) {
			var start = currentPageStart + pageSize,
			     end = start + pageSize;
			
			if (start > currentTotal) {
				this.panelGraphNavigationPagedLast(api, sender);
			} else {
				this.panelGraphNavigationPaged(api, start, end, sender);
			}
		}
		
		this.panelGraphNavigationPagedFirst = function(api, sender) {
			this.panelGraphNavigationPaged(api, 0, pageSize, sender);
		}
		
		this.panelGraphNavigationPagedLast = function(api, sender) {
			var remainder = currentTotal % pageSize,
			start = currentTotal - pageSize;
			
			if (remainder > 0) {
				start = currentTotal - remainder;
			}
			
			this.panelGraphNavigationPaged(api, start, currentTotal, sender);
		}
		
		this.panelGraphNavigationPaged = function(api, start, end, sender) {
			var pageStart = 0,
			    pageEnd = 10;
			
			if (start != undefined) {
				pageStart = start;
			}
			
			if (end != undefined) {
				pageEnd = end;
			}

			$(sender).children().first().attr("href", "/main/graph/" + currentGraphName + "/" + currentFeatureBrowsed + "?start=" + pageStart + "&end=" + pageEnd);
			
			containerPanelBrowserMain.empty();
			
			api.getVertices(currentGraphName, pageStart, pageEnd, function(data) {
				
				if (data.results.length > 0) {
					for (var ix = 0; ix < data.results.length; ix++) {
						containerPanelBrowserMain.append("<div class='make-space'>");
						containerPanelBrowserMain.children().last().jsonviewer({ "json_name": "Result #" + (pageStart + ix + 1), "json_data": data.results[ix], "outer-padding":"0px" });
						
						if(ix % 2 > 0) {
							containerPanelBrowserMain.children().last().find(".json-widget-header").addClass("json-widget-alt");
							containerPanelBrowserMain.children().last().find(".json-widget-content").addClass("json-widget-alt");
						}
					}
					
					// display the paging information plus total record count
					containerPanelBrowser.find(".pager-label").text("Results " + (pageStart + 1) + " - " + (pageStart + data.results.length) + " of " + data.total_size);
					
					currentPageStart = pageStart;
					currentTotal = data.total_size;
					
				} else {
					// no results - hide pagers and show message
					containerPanelBrowser.find(".pager").hide();
					
					currentPageStart = 0;
					currentTotal = 0;
					
					// TODO: there are no records
					
				}
				
				Elastic.refresh();
			},
			function(err) {
				
			});
		}
		
		this.panelGraphNavigationSelected = function(api, featureToBrowse) {
			
			currentFeatureBrowsed = featureToBrowse;
			
			containerPanelTraversals.hide();
			containerPanelBrowser.show();
			
			containerPanelBrowser.find(".pager").show();;
			
			this.panelGraphNavigationPaged(api, 0, pageSize);
			
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
				mediator.getContainerPanelGraphMenu().find("a").unbind("click");
				mediator.getContainerPanelGraphMenu().find("a").click(function(evt) {
					evt.preventDefault();
	                var uri = $(this).attr('href');
	                window.history.pushState({"uri":uri}, '', uri);
					mediator.panelGraphNavigationSelected(api, $(this).attr("_type"));
				});
				
				mediator.getContainerMenuGraph().find("div").unbind("hover");
				mediator.getContainerMenuGraph().find("div").hover(function() {
					$(this).toggleClass("ui-state-hover");
				});
				
				mediator.getContainerMenuGraph().find("div").unbind("click");
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
				
				// initialize the browser panel
				mediator.getContainerPanelBrowser().find("li.pager-button").unbind("hover");
				mediator.getContainerPanelBrowser().find("li.pager-button").hover(function(){
					$(this).addClass("ui-state-hover");
					$(this).removeClass("ui-state-default");
				}, 
				function(){
					$(this).addClass("ui-state-default");
					$(this).removeClass("ui-state-hover");
				});
				
				// get the browser panel pager hooked up 
				mediator.getContainerPanelBrowser().find("li.pager-button").unbind("click");
				mediator.getContainerPanelBrowser().find("li.pager-button").click(function(evt){
					var uri, selectedLink;
					
					if ($(this).children().first().hasClass("ui-icon-seek-first")) {
						// go to first batch of records on the list
						mediator.panelGraphNavigationPagedFirst(api, this);
					} else if ($(this).children().first().hasClass("ui-icon-seek-end")) {
						// go to the last batch of records on the list
						mediator.panelGraphNavigationPagedLast(api, this);
					} else if ($(this).children().first().hasClass("ui-icon-seek-prev")) {
						// go to the previous batch of records on the list
						mediator.panelGraphNavigationPagedPrevious(api, this);
					} else if ($(this).children().first().hasClass("ui-icon-seek-next")) {
						// go to the next batch of records on the list
						mediator.panelGraphNavigationPagedNext(api, this);
					}
					
					evt.preventDefault();
					
					selectedLink = $(this).find("a"); 
	                uri = selectedLink.attr("href");
	                window.history.pushState({"uri":uri}, "", uri);

				})
			}, 
			function(err) {
				api.showMessageError("Could not get the list of graphs from Rexster.");
			});
		});

		
	};
};