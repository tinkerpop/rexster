/**
 * Manages loading of the main menu for the application.
 */
Rexster.modules.mainMenu = function(api) {
	api.initMainMenu = function(state){
		
		$("#radiosetMainMenu").empty();
		
		var menuItems = [
		                  {"id":1, "menuName":"Dashboard", "disabled":true},
		                  {"id":2, "menuName":"Graphs", "checked":true},
		                  {"id":3, "menuName":"Gremlin", "disabled":true},
		                  {"id":4, "menuName":"Pacer", "disabled":true}
		                ];
		
		Rexster("template", function(api) {
			api.applyMainMenuTemplate(menuItems, "#radiosetMainMenu")
		});
		
		$("#radiosetMainMenu").buttonset();
	};
};