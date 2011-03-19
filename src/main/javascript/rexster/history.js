/**
 * Manages browser history and application state. 
 */
Rexster.modules.history = function(api) {
	
	function tryReadStateFromUri() {
		var encodedState = jQuery.url.setUrl(location.href),
		    state = {};
		
		if (!has("native-history-state")) {
			encodedState = jQuery.url.setUrl(Rexster.currentState);
		}
		
		if (encodedState.segment() >= 5) {
			state.objectId = encodedState.segment(4);
		}
		
		if (encodedState.segment() >= 4) {
			state.browse = {
				element : encodedState.segment(3),
				start : 0,
				end : 10 
			};
			
			if (encodedState.param("start") != null && encodedState.param("end")) {
				state.browse.start = encodedState.param("start");
				state.browse.end = encodedState.param("end");
			}
		}
		
		if (encodedState.segment() >= 3) {
			state.graph = encodedState.segment(2);
		}
		
		if (encodedState.segment() >= 2) {
			state.menu = encodedState.segment(1);
		} 
		
		return state;
	}
	
	/**
	 * Pushes a URI into the browser history and parses it to current state. 
	 */
	api.historyPush = function(uri) {
		Rexster.currentState = uri;
		if (has("native-history-state")) {
			window.history.pushState({"uri":uri}, '', uri);
		} 
	}
	
	/**
	 * Gets the current application state given the current URI.  
	 * 
	 * It is important that changes to browser history happen prior to getting
	 * state as the state is read from the current URI.
	 */
	api.getApplicationState = function() {
		return tryReadStateFromUri();
	}
};
	