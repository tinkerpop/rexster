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
	
	window.onpopstate = function(event) {
		restoreApplication(tryReadState(null, event));
	};
	
	function tryReadState(defaultState, event) {
		var url = jQuery.url.setUrl(location.href);
		var state = {};
		
		if (url.segment() >= 4) {
			state.browse = {
				element : url.segment(3),
				start : 0,
				end : 10 
			};
			
			if (url.param("start") != null && url.param("end")) {
				state.browse.start = url.param("start");
				state.browse.end = url.param("end");
			}
		}
		
		if (url.segment() >= 3) {
			state.graph = url.segment(2);
		}
		
		if (url.segment() >= 2) {
			state.menu = url.segment(1);
		} else {
			// this means that the state is not defined by the uri.
			state = defaultState;
		}
		
		return state;
	}
	
    function restoreApplication(state) {
		Rexster("template", "mainMenu", "graph", function(api) {
			
			// compile the templates
			api.initTemplates();
			
			// build the main menu
			api.initMainMenu(state);
			
			// the graph panel is the only active thing right now, so 
			// just initialize for simplicity sake.
			api.initGraphList(state, function(){
				Elastic.refresh();
			});
		});
	}
    
    restoreApplication(tryReadState({ "menu":"graph" }));
    
});