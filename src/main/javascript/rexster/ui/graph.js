/**
 * Manages the graph panel user interface.
 */
Rexster.modules.graph = function(api) {
	
	var mediator = new GraphPanelMediator("#panelGraphMenuGraph", "#panelTraversals", "#panelElementViewer", "#panelGraphMenu", "#panelBrowser", "#panelBrowserMain"),
	    currentGraph;
	
	/**
	 * Manages graph panel interactions.
	 */
	function GraphPanelMediator(menuGraph, panelTraversals, panelElementViewer, panelGraphMenu, panelBrowser, panelBrowserMain) {
		var  containerMenuGraph = $(menuGraph), // graph menu in the left panel
	         containerPanelBrowser = $(panelBrowser),
		     containerPanelBrowserMain = $(panelBrowserMain),
		     containerPanelElementViewer = $(panelElementViewer),
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

            // look to hide the browser vertices button because sail graphs have infinite vertices.
            containerPanelGraphMenu.find("a[_type='vertices']").show();
			api.getGraph(currentGraphName, function(result){
			    if (result.type === "com.tinkerpop.blueprints.pgm.impls.sail.impls.MemoryStoreSailGraph"
			        || result.type === "com.tinkerpop.blueprints.pgm.impls.sail.impls.NativeStoreSailGraph") {
                    containerPanelGraphMenu.find("a[_type='vertices']").hide();
			    }
			});

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
					containerPanelElementViewer.hide();
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
			} else if (state.objectId != undefined) {
				// since the browse is defined then the check is to see if there is a browse
				// of a individual element or the element list.  if the objectId is set then
				// that means that there is an individual element being viewed.
				this.panelGraphElementViewSelected(api, state.browse.element, state.objectId, onComplete);
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
		this.renderPagedResults = function(api, results, resultSize, currentGraphName, pageStart, pageEnd, onPageChangeComplete){
			
			var that = this,
			    metaDataLabel = "";
			
			if (results.length > 0) {
				for (var ix = 0; ix < results.length; ix++) {
					containerPanelBrowserMain.append("<div class='make-space'>");
					
					metaDataLabel = "Type:[" + results[ix]._type + "] ID:[" + results[ix]._id + "]";
					if (results[ix]._type == "edge") {
						metaDataLabel = metaDataLabel + " In:[" + results[ix]._inV + "] Out:[" + results[ix]._outV + "] Label:[" + results[ix]._label + "]";
					}
					
					var toolbar = $("<ul/>");
					
					// extra css here overrides some elastic css settings
					toolbar.addClass("unit on-1 columns")
						.css({ "margin" : "0px", "margin-left" : "10px" });
					
					var toolbarButtonGraph = toolbar.append("<li/>").children().first();
					
					toolbarButtonGraph.addClass("fixed column ui-state-default ui-corner-all pager-button")
						.css({"width": "30px"});
					toolbarButtonGraph.attr("title", "View Element");
					
					toolbarButtonGraph.hover(function(){
						$(this).addClass("ui-state-hover");
						$(this).removeClass("ui-state-default");
					}, 
					function(){
						$(this).addClass("ui-state-default");
						$(this).removeClass("ui-state-hover");
					});
					
					var toolbarButtonGraphLink = toolbarButtonGraph.append("<a/>").children().first();
					toolbarButtonGraphLink.attr("href", "/main/graph/" + currentGraphName + "/" + currentFeatureBrowsed + "/" + results[ix]._id);
					toolbarButtonGraphLink.addClass("ui-icon ui-icon-arrow-4-diag");
					
					$(toolbarButtonGraphLink).click(function(event) {
						event.preventDefault();
						var uri = $(this).attr('href');
						var split = uri.split("/");
	                	api.historyPush(uri);
						
						that.panelGraphElementViewSelected(api, split[4], split[5]);
						
						/* bah...visualization is not working nicely
						var split = $(this).attr("href").split("/");
						alert(split[2]);
						
						var colors = pv.Colors.category20();
						var vis = new pv.Panel()
							.canvas("dialogGraphView")
						    .width(760)
						    .height(530)
						    .fillStyle("white")
						    .event("mousedown", pv.Behavior.pan())
						    .event("mousewheel", pv.Behavior.zoom(1/8));
						
						var graph;
						// this sux right now
						api.getVertexCenteredGraph("gratefulgraph", split[2], 2, function(g) {
								graph = g;
							}, 
							function() {
								alert("junk");
							});
						 
						var force = vis.add(pv.Layout.Arc)
						    .nodes(graph.vertices)
						    .links(graph.edges);
						 
						force.link.add(pv.Line);
						
						force.node.add(pv.Dot)
						    .size(function(d) { return (d.linkDegree + 4) * Math.pow(this.scale, -1.5) })
						    .fillStyle(function(d) { return d.fix ? "brown" : colors(d.group) })
						    .strokeStyle(function() { return this.fillStyle().darker() })
						    .lineWidth(1)
						    .title(function(d) { return d.nodeName })
						    .event("mousedown", pv.Behavior.drag())
						    .event("drag", force);
						    
						$("#dialogGraphView").dialog({height:600, width:800});
						 
						vis.render();
					    */
					});
					
					containerPanelBrowserMain.children().last().jsonviewer({ 
						"json_name": "#" + (pageStart + ix + 1) + " | " + metaDataLabel, 
						"json_data": results[ix], 
						"outer-padding":"0px",
						"toolbar": toolbar});
					
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
					that.renderPagedResults(api, data.results, data.total_size, currentGraphName, pageStart, pageEnd, onPageChangeComplete);
				},
				function(err) {
					api.showMessageError("Could not get the vertices of graphs from Rexster.");
				});
			} else if (currentFeatureBrowsed === "edges") {
				api.getEdges(currentGraphName, pageStart, pageEnd, function(data) {
					that.renderPagedResults(api, data.results, data.total_size, currentGraphName, pageStart, pageEnd, onPageChangeComplete);
				},
				function(err) {
					api.showMessageError("Could not get the edges of graphs from Rexster.");
				});
			}
		}
		
		this.panelGraphElementViewSelected = function(api, featureToBrowse, objectId, onComplete) {
			var that = this,
			    elementHeaderTitle = "Vertex",
			    containerPanelElementViewerMain = containerPanelElementViewer.find(".ui-widget-content");
			
			currentFeatureBrowsed = featureToBrowse;
			
			containerPanelTraversals.hide();
			containerPanelBrowser.hide();
			containerPanelElementViewer.show();
			$("#panelElementViewerLeft > ul").empty();
			$("#panelElementViewerRight > ul").empty();
			$("#panelElementViewerMiddle").empty();
			$("#panelElementViewerLeft .intense").show();
			$("#panelElementViewerRight .intense").show();
			
			// a bit of hack to get around some jsonviewer margins
			$("#panelElementViewerLeft > ul").css("margin-left", "0px");
			$("#panelElementViewerRight > ul").css("margin-left", "0px");
			
			if (featureToBrowse === "edges") {
				elementHeaderTitle = "Edge";
				$("#panelElementViewerLeft .intense").hide();
				$("#panelElementViewerRight .intense").hide();
			} 
			
			elementHeaderTitle = elementHeaderTitle + " [" + objectId + "]";
			
			containerPanelElementViewer.find(".ui-widget-header").text(elementHeaderTitle); 
			
			if (featureToBrowse === "vertices") {
				$("#panelElementViewerLeft h3").text("In");
				$("#panelElementViewerRight h3").text("Out");
				
				api.getVertexElement(currentGraphName, objectId, function(result) {
					var element = result.results;
					
					metaDataLabel = "Type:[" + element._type + "] ID:[" + element._id + "]";
					
					$("#panelElementViewerMiddle").jsonviewer({ 
						"json_name": metaDataLabel, 
						"json_data": element, 
						"outer-padding":"0px"});
				}, 
				function(err) {
				},
				false);
				
				api.getVertexOutEdges(currentGraphName, objectId, function(result) {
					var outEdges = result.results;
					
					$("#panelElementViewerRight .intense .value").text(outEdges.length);
					$("#panelElementViewerRight .intense .label").text("Edges");
					
					// add the current graph name as a property
					for (var ix = 0; ix < outEdges.length; ix++) {
						outEdges[ix].currentGraphName = currentGraphName;
					}
					
					api.applyListVertexViewInEdgeListTempate(outEdges, $("#panelElementViewerRight > ul"));
				}, 
				function(err) {
				},
				false);
				
				api.getVertexInEdges(currentGraphName, objectId, function(result) {
					var inEdges = result.results;
					
					$("#panelElementViewerLeft .intense .value").text(inEdges.length);
					$("#panelElementViewerLeft .intense .label").text("Edges");
					
					// add the current graph name as a property
					for (var ix = 0; ix < inEdges.length; ix++) {
						inEdges[ix].currentGraphName = currentGraphName;
					}
					
					api.applyListVertexViewOutEdgeListTempate(inEdges, $("#panelElementViewerLeft > ul"));
				}, 
				function(err) {
				},
				false);
				
				$("#panelElementViewerLeft > ul > li > a, #panelElementViewerRight > ul > li > a").click(function(evt) {
					evt.preventDefault();
					var uri = $(this).attr("href");
					var split = uri.split("/");
                	api.historyPush(uri);
                	
                	// don't want the refresh to be called so pass an empty function
					that.panelGraphElementViewSelected(api, split[4], split[5]);
				});
			} else {

				$("#panelElementViewerLeft h3").text("Out");
				$("#panelElementViewerRight h3").text("In");
				
				api.getEdgeElement(currentGraphName, objectId, function(result) {
					var element = result.results;
					
					metaDataLabel = "Type:[" + element._type + "] ID:[" + element._id + "] Label:[" + element._label + "]";
					
					$("#panelElementViewerMiddle").jsonviewer({ 
						"json_name": metaDataLabel, 
						"json_data": element, 
						"outer-padding":"0px"});
					
					$("#panelElementViewerRight .intense .value").text("");
					$("#panelElementViewerRight .intense .label").text("");

					$("#panelElementViewerLeft .intense .value").text("");
					$("#panelElementViewerLeft .intense .label").text("");
					
					// have to add this in to get around the indent on jsonviewer.  ugh...
					$("#panelElementViewerLeft > ul").css("margin-left", "-12px");
					$("#panelElementViewerRight > ul").css("margin-left", "-12px");
					
					// get the in vertex of the edge
					api.getVertexElement(currentGraphName, element._inV, function(result) {
						var element = result.results;
						
						metaDataLabel = "Type:[" + element._type + "] ID:[" + element._id + "]";
						
						var toolbar = $("<ul/>");
						
						// extra css here overrides some elastic css settings
						toolbar.addClass("unit on-1 columns")
							.css({ "margin" : "0px", "margin-left" : "10px" });
						
						var toolbarButtonGraph = toolbar.append("<li/>").children().first();
						
						toolbarButtonGraph.addClass("fixed column ui-state-default ui-corner-all pager-button")
							.css({"width": "30px"});
						toolbarButtonGraph.attr("title", "View Element");
						
						toolbarButtonGraph.hover(function(){
							$(this).addClass("ui-state-hover");
							$(this).removeClass("ui-state-default");
						}, 
						function(){
							$(this).addClass("ui-state-default");
							$(this).removeClass("ui-state-hover");
						});
						
						var toolbarButtonGraphLink = toolbarButtonGraph.append("<a/>").children().first();
						toolbarButtonGraphLink.attr("href", "/main/graph/" + currentGraphName + "/vertices/" + element._id);
						toolbarButtonGraphLink.addClass("ui-icon ui-icon-arrow-4-diag");
						
						$(toolbarButtonGraphLink).click(function(event) {
							event.preventDefault();
							var uri = $(this).attr('href');
							var split = uri.split("/");
		                	api.historyPush(uri);
							
						    that.panelGraphElementViewSelected(api, split[4], split[5]);
						});
						
						$("#panelElementViewerRight > ul").jsonviewer({ 
							"json_name": metaDataLabel, 
							"json_data": element, 
							"outer-padding":"0px",
							"toolbar":toolbar});
					}, 
					function(err) {
					},
					false);
					
					// get the out vertex of the edge
					api.getVertexElement(currentGraphName, element._outV, function(result) {
						var element = result.results;
						
						metaDataLabel = "Type:[" + element._type + "] ID:[" + element._id + "]";
						
						var toolbar = $("<ul/>");
						
						// extra css here overrides some elastic css settings
						toolbar.addClass("unit on-1 columns")
							.css({ "margin" : "0px", "margin-left" : "10px" });
						
						var toolbarButtonGraph = toolbar.append("<li/>").children().first();
						
						toolbarButtonGraph.addClass("fixed column ui-state-default ui-corner-all pager-button")
							.css({"width": "30px"});
						toolbarButtonGraph.attr("title", "View Element");
						
						toolbarButtonGraph.hover(function(){
							$(this).addClass("ui-state-hover");
							$(this).removeClass("ui-state-default");
						}, 
						function(){
							$(this).addClass("ui-state-default");
							$(this).removeClass("ui-state-hover");
						});
						
						var toolbarButtonGraphLink = toolbarButtonGraph.append("<a/>").children().first();
						toolbarButtonGraphLink.attr("href", "/main/graph/" + currentGraphName + "/vertices/" + element._id);
						toolbarButtonGraphLink.addClass("ui-icon ui-icon-arrow-4-diag");
						
						$(toolbarButtonGraphLink).click(function(event) {
							event.preventDefault();
							var uri = $(this).attr('href');
							var split = uri.split("/");
		                	api.historyPush(uri);
								
							that.panelGraphElementViewSelected(api, split[4], split[5]);
						});
						
						$("#panelElementViewerLeft > ul").jsonviewer({ 
							"json_name": metaDataLabel, 
							"json_data": element, 
							"outer-padding":"0px",
							"toolbar":toolbar});
					}, 
					function(err) {
					},
					false);
					
				}, 
				function(err) {
				},
				false);
			}
			
			if (onComplete != undefined) {
				onComplete();
			} else {
				Elastic.refresh();
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
			containerPanelElementViewer.hide();
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