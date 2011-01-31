/**
 * Manages the graph panel user interface.
 */
Rexster.modules.graph = function(api) {
	
	var mediator = new GraphPanelMediator("#panelGraphMenuGraph", "#panelTraversals", "#panelGraphMenu", "#panelBrowser", "#panelBrowserMain"),
	    currentGraph;
	
	/**
	 * Manages graph panel interactions.
	 */
	function GraphPanelMediator(menuGraph, panelTraversals, panelGraphMenu, panelBrowser, panelBrowserMain) {
		var  containerMenuGraph = $(menuGraph), // graph menu in the left panel
	         containerPanelBrowser = $(panelBrowser),
		     containerPanelBrowserMain = $(panelBrowserMain),
			 containerPanelTraversals = $(panelTraversals),
			 containerPanelTraversalsList = containerPanelTraversals.find("ul"),
			 containerPanelGraphMenu = $(panelGraphMenu), // browse options
			 currentGraphName = "",
			 currentFeatureBrowsed = "",
			 currentPageStart = 0,
			 currentTotal = 0,
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
		
		/**
		 * A graph was selected from the graph menu.
		 * 
		 * @param api        {Object} A Rexster API instance.
		 * @param onComplete {Object}   Call back function made when the event is complete.
		 */
		this.graphSelectionChanged = function(api, onComplete) {
			
			var state = api.getApplicationState();
			    
			currentGraphName = state.graph;
			
			containerMenuGraph.find(".graph-item").removeClass("ui-state-active");
			containerMenuGraph.find("#graphItemgraph" + currentGraphName).addClass("ui-state-active");
			
			// modify the links on the browse menus to match current state
			containerPanelGraphMenu.find("a[_type='vertices']").attr("href", "/main/graph/" + currentGraphName + "/vertices?start=0&end=10");
			containerPanelGraphMenu.find("a[_type='edges']").attr("href", "/main/graph/" + currentGraphName + "/edges?start=0&end=10");
			
			// load the graph profile
			api.getGraph(currentGraphName, function(graphResult) {
				$("#panelGraphTitle").text(currentGraphName);
				$("#panelGraphDetail").text(graphResult.graph);
			},
			function(err){
				api.showMessageError("Could not get the graph profile from Rexster.");
			});
			
			// check the state.  if the browse panel is on, then don't worry about loading
			// traversals.
			if (state.browse === undefined) {
				// load traversals panel for the current graph
				api.getTraversals(currentGraphName, function(traversalResult) { 
					
					containerPanelTraversals.show();
					containerPanelBrowser.hide();
					containerPanelTraversalsList.empty();
					
					api.applyListTraversalsTemplate(traversalResult.results, containerPanelTraversalsList);
					
					// execute the callback now that the traversals are done.
					if (onComplete != undefined) {
						onComplete();
					}
				},
				function(err){
					api.showMessageError("Could not get the list of traversals from Rexster.");
				});
			} else {
				// restore state to a page on the browser
				this.panelGraphNavigationSelected(api, state.browse.element, state.browse.start, state.browse.end, onComplete);
			}
		}
		
		/**
		 * Move the data view to the previous page given the current position.
		 * 
		 * @param api {Object} A Rexster API instance.
		 */
		this.panelGraphNavigationPagedPrevious = function(api) {
			var range = this.calculatePreviousPageRange();
			this.panelGraphNavigationPaged(api, range.start, range.end);
		}
		
		/**
		 * Move the data view to the next page given the current position.
		 * 
		 * @param api {Object} A Rexster API instance.
		 */
		this.panelGraphNavigationPagedNext = function(api) {
			var range = this.calculateNextPageRange();
			this.panelGraphNavigationPaged(api, range.start, range.end);
		}
		
		/**
		 * Move the data view to the first page in the data set.
		 * 
		 * @param api {Object} A Rexster API instance.
		 */
		this.panelGraphNavigationPagedFirst = function(api) {
			this.panelGraphNavigationPaged(api, 0, pageSize);
		}
		
		/**
		 * Move the data view to the last page in the data set.
		 * 
		 * @param api {Object} A Rexster API instance.
		 */
		this.panelGraphNavigationPagedLast = function(api) {
			this.panelGraphNavigationPaged(api, this.calculateStartForLastPage(), currentTotal);
		}
		
		/**
		 * Calculates the index of the start for the last page.
		 * 
		 * Since the last page may have less than the page size number of records, the
		 * last page will not start a page size from the length of the records.
		 */
		this.calculateStartForLastPage = function() {
			var remainder = currentTotal % pageSize,
			start = currentTotal - pageSize;
			
			if (remainder > 0) {
				start = currentTotal - remainder;
			}
			
			return start;
		}
		
		/**
		 * Calculates the next page range given the current position.
		 * 
		 * @returns {Object} A range object that describes a start and end point for the 
		 *                   paging mechanism.
		 */
		this.calculateNextPageRange = function() {
			var start = currentPageStart + pageSize,
		        end = start + pageSize,
		        range = {
					start : start,
					end : end
				};
		
			if (start > currentTotal) {
				range.start = this.calculateStartForLastPage();
				range.end = currentTotal;
			}
			
			return range;
		}
		
		/**
		 * Calculates the previous page range given the current position.
		 * 
		 * @returns {Object} A range object that describes a start and end point for the 
		 *                   paging mechanism.
		 */
		this.calculatePreviousPageRange = function() {
			var start = currentPageStart - pageSize,
		    	end = currentPageStart,
		    	range = {
					start : start,
					end : end
				};
		
			if (start < 0) {
				range.start = 0;
				range.end = pageSize;
			} 
			
			return range;
		}
		
		/**
		 * Renders a set of paged results.
		 * 
		 * @param results              {Array} This is an array of any object.  It will be rendered as JSON in 
		 *                                     a generic tree-like fashion.
		 * @param resultSize           {int} The total size of the results.  The results will only represent
		 *                                   one page of the total result set.
		 * @param currentGraphName     {String} The current name of the graph being accessed.
		 * @param pageStart            {int} The start index of the paged set.
		 * @param pageEnd              {int} The end index of the paged set.
		 * @param onPageChangeComplete {Function}    Call back function to execute when the render is finished.
		 */
		this.renderPagedResults = function(results, resultSize, currentGraphName, pageStart, pageEnd, onPageChangeComplete){
			
			var that = this,
			    metaDataLabel = "";
			
			if (results.length > 0) {
				for (var ix = 0; ix < results.length; ix++) {
					containerPanelBrowserMain.append("<div class='make-space'>");
					
					metaDataLabel = "Type:[" + results[ix]._type + "] ID:[" + results[ix]._id + "]";
					if (results[ix]._type == "edge") {
						metaDataLabel = metaDataLabel + " In:[" + results[ix]._inV + "] Out:[" + results[ix]._outV + "] Label:[" + results[ix]._label + "]";
					}
					
					containerPanelBrowserMain.children().last().jsonviewer({ "json_name": "#" + (pageStart + ix + 1) + " | " + metaDataLabel, "json_data": results[ix], "outer-padding":"0px" });
					
					if(ix % 2 > 0) {
						containerPanelBrowserMain.children().last().find(".json-widget-header").addClass("json-widget-alt");
						containerPanelBrowserMain.children().last().find(".json-widget-content").addClass("json-widget-alt");
					}
				}
				
				// display the paging information plus total record count
				containerPanelBrowser.find(".pager-label").text("Results " + (pageStart + 1) + " - " + (pageStart + results.length) + " of " + resultSize);
				
				currentPageStart = pageStart;
				currentTotal = resultSize;
				
			} else {
				// no results - hide pagers and show message
				containerPanelBrowser.find(".pager").hide();
				
				currentPageStart = 0;
				currentTotal = 0;
				
				// TODO: there are no records
				
			}
			
			// set the links for the paging...kind of a nicety.  not really used in
			// the paging process at this point.  just makes for clean uris
			nextRange = that.calculateNextPageRange();
			previousRange = that.calculatePreviousPageRange();
			
			containerPanelBrowser.find(".ui-icon-seek-first").attr("href", "/main/graph/" + currentGraphName + "/" + currentFeatureBrowsed + "?start=0&end=" + pageSize);
			containerPanelBrowser.find(".ui-icon-seek-end").attr("href", "/main/graph/" + currentGraphName + "/" + currentFeatureBrowsed + "?start=" + that.calculateStartForLastPage() + "&end=" + currentTotal);
			containerPanelBrowser.find(".ui-icon-seek-prev").attr("href", "/main/graph/" + currentGraphName + "/" + currentFeatureBrowsed + "?start=" + previousRange.start + "&end=" + previousRange.end);
			containerPanelBrowser.find(".ui-icon-seek-next").attr("href", "/main/graph/" + currentGraphName + "/" + currentFeatureBrowsed + "?start=" + nextRange.start + "&end=" + nextRange.end);
			
			if (onPageChangeComplete != undefined) {
				onPageChangeComplete();
			}
		}
		
		/**
		 * The page of data to view has been changed.  The data must be requested and the results
		 * rendered to the view.
		 * 
		 * @param api                  {Object} A Rexster API instance.
		 * @param start                {int} The start index of the paged set.
		 * @param end                  {int} The end index of the paged set.
		 * @param onPageChangeComplete {Function}    Call back function to execute when the render is finished.
		 */
		this.panelGraphNavigationPaged = function(api, start, end, onPageChangeComplete) {
			var pageStart = 0,
			    pageEnd = 10,
			    nextRange = {},
			    previousRange = {},
			    that = this;
			
			if (start != undefined) {
				pageStart = start;
			}
			
			if (end != undefined) {
				pageEnd = end;
			}
			
			// these better be numbers.  perhaps the state coming from the uri
			// is sending the params in as strings.
			pageStart = parseInt(pageStart);
			pageEnd = parseInt(pageEnd);

			containerPanelBrowserMain.empty();
			
			if (currentFeatureBrowsed === "vertices") {
				api.getVertices(currentGraphName, pageStart, pageEnd, function(data) {
					that.renderPagedResults(data.results, data.total_size, currentGraphName, pageStart, pageEnd, onPageChangeComplete);
				},
				function(err) {
					api.showMessageError("Could not get the vertices of graphs from Rexster.");
				});
			} else if (currentFeatureBrowsed === "edges") {
				api.getEdges(currentGraphName, pageStart, pageEnd, function(data) {
					that.renderPagedResults(data.results, data.total_size, currentGraphName, pageStart, pageEnd, onPageChangeComplete);
				},
				function(err) {
					api.showMessageError("Could not get the edges of graphs from Rexster.");
				});
			}
		}
		
		/**
		 * A choice has been made to browse a particular aspect of the graph. 
		 * 
		 * @param api             {Object} A Rexster API instance.
		 * @param featureToBrowse {String} The feature that was selected (ie. vertices, edges, etc).
		 * @param start           {int} The start index of the paged set.
		 * @param end             {int} The end index of the paged set.
		 */
		this.panelGraphNavigationSelected = function(api, featureToBrowse, start, end, onComplete) {
			
			var startPoint = 0,
			    endPoint = pageSize;
			
			if (start != undefined) {
				startPoint = start;
			}
			
			if (end != undefined) {
				endPoint = end;
			}
			
			currentFeatureBrowsed = featureToBrowse;
			
			containerPanelTraversals.hide();
			containerPanelBrowser.show();
			
			containerPanelBrowser.find(".pager").show();;
			
			this.panelGraphNavigationPaged(api, startPoint, endPoint, function() {
				// it is expected that the onComplete will call Elastic.refresh() 
				// at some point
				if (onComplete != undefined) {
					onComplete();
				} else {
					Elastic.refresh();
				}
			});
			
		}
		
		this.resetMenuGraph = function() {
			containerMenuGraph.empty();
		}
	} 
	
	/**
	 * Initializes the graph list.
	 * 
	 * @param onInitComplete   {Function} The callback made when graph initialization is completed. 
	 */
	api.initGraphList = function(onInitComplete){
		
		mediator.resetMenuGraph();
		Rexster("ajax", "template", "info", "history", function(api) {
			api.getGraphs(function(result){
				
				var ix = 0,
					max = 0,
				    graphs = [],
				    state = {};
				
				state = api.getApplicationState();
				
				// construct a list of graphs that can be pushed into the graph menu
				max = result.graphs.length;
				for (ix = 0; ix < max; ix += 1) {
					graphs.push({ "menuName": result.graphs[ix], "panel" : "graph" });
				}

				api.applyMenuGraphTemplate(graphs, mediator.getContainerMenuGraph());
				
				mediator.getContainerPanelGraphMenu().find("a").button({ icons: {primary:"ui-icon-search"}});
				mediator.getContainerPanelGraphMenu().find("a").unbind("click");
				mediator.getContainerPanelGraphMenu().find("a").click(function(evt) {
					evt.preventDefault();
	                var uri = $(this).attr('href');
	                api.historyPush(uri);
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
	                api.historyPush(uri);
	                
	                mediator.graphSelectionChanged(api);
				});
				
				// check the state, if it is at least two items deep then the state 
				// of the graph is also selected and an attempt to make the graph active
				// should be made.
				if (state.hasOwnProperty("graph")) {
	                mediator.graphSelectionChanged(api, onInitComplete);
				}
				
				// if the state does not specify a graph then select the first one. 
				if (!state.hasOwnProperty("graph")) {
					mediator.getContainerMenuGraph().find("#graphItemgraph" + graphs[0].menuName).click();
					if (onInitComplete != undefined) {
						onInitComplete();
					}
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
					evt.preventDefault();
					var uri, selectedLink;
					
					// need to push state first before navigation so that when the mediator
					// is done it will place a new href value in for the next set of links
					selectedLink = $(this).find("a"); 
	                uri = selectedLink.attr("href");
	                api.historyPush(uri);
					
					if ($(this).children().first().hasClass("ui-icon-seek-first")) {
						// go to first batch of records on the list
						mediator.panelGraphNavigationPagedFirst(api);
					} else if ($(this).children().first().hasClass("ui-icon-seek-end")) {
						// go to the last batch of records on the list
						mediator.panelGraphNavigationPagedLast(api);
					} else if ($(this).children().first().hasClass("ui-icon-seek-prev")) {
						// go to the previous batch of records on the list
						mediator.panelGraphNavigationPagedPrevious(api);
					} else if ($(this).children().first().hasClass("ui-icon-seek-next")) {
						// go to the next batch of records on the list
						mediator.panelGraphNavigationPagedNext(api);
					}
					
				})
			}, 
			function(err) {
				api.showMessageError("Could not get the list of graphs from Rexster.");
			});
		});

		
	};
};