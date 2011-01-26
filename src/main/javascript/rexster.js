Rexster.modules = {};

function Rexster() {
	var args = Array.prototype.slice.call(arguments),
	    callback = args.pop(),
	    modules = (args[0] && typeof args[0] == "string") ? args : args[0],
	    i;
	
	if (!(this instanceof Rexster)) {
		return new Rexster(modules, callback);
	}
	
	this.currentPanel = null;
	
	if (!modules || modules === '*') {
		modules = [];
		for (i in Rexster.modules) {
			if (Rexster.modules.hasOwnProperty(i)) {
				modules.push(i);
			}
		}
	}
	
	for (i = 0; i < modules.length; i += 1) {
		Rexster.modules[modules[i]](this);
	}
	
	callback(this);
}

$(function(){
	
	window.onpopstate = function(event) {
		restoreApplication();
	};
	
    function restoreApplication() {
		Rexster("template", "mainMenu", "graph", function(api) {
			
			// compile the templates
			api.initTemplates();
			
			// build the main menu.  this action will initialize the 
			// first enabled panel
			api.initMainMenu();
		});
	}
    
    Rexster("history", function(api) {    	
    	// determine if the state is already established
    	var state = api.getApplicationState();
    	if (!state.hasOwnProperty("menu")) {
    		// since there is no menu selected initialized the graph page first.
	    	api.historyPush("/main/graph");
    	}
    });
    
});