/**
 * Manages loading of the main menu for the application.
 */
Rexster.modules.mainMenu = function(api) {
	
	function slideGraphPanel(newState) {
		var options = {},
		    different = isPanelDifferent("mainGraph");
		
		// hide the current panel as a new one has been selected
		if (Rexster.currentPanel != undefined && Rexster.currentPanel != null && different) {
			Rexster.currentPanel.hide("slide");
		}
		
		Rexster("graph", "history", function(innerApi) {
			
			if (newState != undefined) {
				innerApi.historyPush(newState);
			}
			
			// show the graph panel 
			innerApi.initGraphList(function(){
				if (different) {
					Rexster.currentPanel = $("#mainGraph");
					Rexster.currentPanel.show("slide");
				}
				
				Elastic.refresh();
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
		                  {"id":"Graph", "menuName":"Graphs", "checked":true},
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
			var options = {};
			
			// hide the current panel as a new one has been selected
			if (Rexster.currentPanel != undefined && Rexster.currentPanel != null) {
				Rexster.currentPanel.hide("slide");
			}

			Rexster.currentPanel = $("#mainGremlin");
			Rexster.currentPanel.show("slide");
			Elastic.refresh();
 
			Rexster("history", "terminal", function(innerApi) {
				innerApi.historyPush("/main/gremlin");
				
				innerApi.initTerminal();
			});

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
				var options = {};
				
				// hide the current panel as a new one has been selected
				if (Rexster.currentPanel != undefined && Rexster.currentPanel != null) {
					Rexster.currentPanel.hide("slide");
				}

				Rexster.currentPanel = $("#mainGremlin");
				Rexster.currentPanel.show("slide");
				Elastic.refresh();
	 
				Rexster("terminal", function(innerApi) {
					innerApi.initTerminal();
				});
			} else {
				$("#radioMenuGremlin").click();
			}
		}
	};
};