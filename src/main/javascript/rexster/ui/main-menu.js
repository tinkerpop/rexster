/**
 * Manages loading of the main menu for the application.
 */
Rexster.modules.mainMenu = function(api) {
	
	function slideGraphPanel(newState) {
		var options = { direction:"right" },
		    different = isPanelDifferent("mainGraph");
		
		// hide the current panel as a new one has been selected
		if (Rexster.currentPanel != undefined && Rexster.currentPanel != null && different) {
			Rexster.currentPanel.hide("slide", options, 500);
		}
		
		Rexster("graph", "history", function(innerApi) {
			
			if (newState != undefined) {
				innerApi.historyPush(newState);
			}
			
			// show the graph panel 
			innerApi.initGraphList(function(){
				if (different) {
					Rexster.currentPanel = $("#mainGraph");
					$("#footer").fadeOut();
					$("#slideHolder").prepend(Rexster.currentPanel); 
					Rexster.currentPanel.delay(500).show("slide", null, function() {
						$("#footer").fadeIn()
					});
				}
			});
		});
	}
	
	function slideGremlinPanel(newState) {
		var options = { direction:"right" },
	    different = isPanelDifferent("mainGremlin");;
		
		// hide the current panel as a new one has been selected
		if (Rexster.currentPanel != undefined && Rexster.currentPanel != null && different) {
			Rexster.currentPanel.hide("slide", options);
		}

		Rexster("terminal", "history", function(innerApi) {
			if (newState != undefined) {
				innerApi.historyPush(newState);
			}
			
			innerApi.initTerminal(function(){
				if (different) {
					Rexster.currentPanel = $("#mainGremlin");
					$("#footer").fadeOut();
					$("#slideHolder").prepend(Rexster.currentPanel);
					Rexster.currentPanel.delay(500).show("slide", null, function() {
						$("#footer").fadeIn()
					});
				}
			});
		});
	}
	
	function isPanelDifferent(newPanel) {
		var diff = true;
		
		if (Rexster.currentPanel != undefined && Rexster.currentPanel != null) {
			diff = Rexster.currentPanel.attr("id") != newPanel;
		}
		
		return diff;
	}
	
	api.initMainMenu = function(){
		
		var menuItems = [
		                  {"id":"Dashboard", "menuName":"Dashboard", "disabled":true},
		                  {"id":"Graph", "menuName":"Browse", "checked":true},
		                  {"id":"Gremlin", "menuName":"Gremlin"},
		                  {"id":"Pacer", "menuName":"Pacer", "disabled":true}
		                ],
		     state = {};
		
		Rexster("history", function(innerApi) {
			state = innerApi.getApplicationState();
		});
		
		$("#radiosetMainMenu").empty();
		
		if (state != undefined && state.menu != undefined) {
			if (state.menu == "graph") {
				menuItems[1].checked = true;
				menuItems[2].checked = false;
			} else if (state.menu == "gremlin") {
				menuItems[1].checked = false;
				menuItems[2].checked = true;
			}
		} 
		
		Rexster("template", function(innerApi) {
			innerApi.applyMainMenuTemplate(menuItems, "#radiosetMainMenu")
		});
		
		$("#radiosetMainMenu").buttonset();
		
		$("#radioMenuGraph").unbind("click");
		$("#radioMenuGraph").click(function() {
			slideGraphPanel("/main/graph");
			return false;
		});

		$("#radioMenuGremlin").unbind("click");
		$("#radioMenuGremlin").click(function() {
			slideGremlinPanel("/main/gremlin");
            return false;
		});
		
		if (state.menu === "graph") {
			if (state.hasOwnProperty("graph")) {
				// the state is defined for the graph so no need to initialize
				slideGraphPanel();
			} else {
				// no graph is set so let it default the first one 
				$("#radioMenuGraph").click();			
			}
		} else if (state.menu === "gremlin") {
			if (state.hasOwnProperty("graph")) {
				slideGremlinPanel();
			} else {
				$("#radioMenuGremlin").click();
			}
		}
	};
};