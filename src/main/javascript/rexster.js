Rexster.modules = {};

function Rexster() {
	var args = Array.prototype.slice.call(arguments),
	    callback = args.pop(),
	    modules = (args[0] && typeof args[0] == "string") ? args : args[0],
	    i;
	
	if (!(this instanceof Rexster)) {
		return new Rexster(modules, callback);
	}
	
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

    $.history.init(function(hash){
    	if (hash == undefined) {
    		hash = "";
    	}
    	
    	var state = hash.split(/,/);
        restoreApplication(state);
    },
    { unescape: ",/" });
	
    function restoreApplication(state) {
		Rexster("template", "mainMenu", "graph", function(api) {
			
			// compile the templates
			api.initTemplates();
			
			// build the main menu
			api.initMainMenu(state);
			
			// the graph panel is the only active thing right now, so 
			// just initialize for simplicity sake.
			api.initGraphList(state, function(){
				Elastic.refresh($("#main"));
			});
		});
	}
    
});