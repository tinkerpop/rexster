/**
 * Compiles templates for later use.
 */
Rexster.modules.template = function(api) {
	
	var templater = {};
	
	templater.templateNameMainMenuItem = "mainMenuItem";
	templater.templateNameAccordionGraph = "accordionGraph";
	templater.templateNameListTraversals = "listTraversals";
	templater.templateNameListVertices = "listVertices";
	
	templater.init = function() {
		// expects {id, menuName, [checked], [disabled]}
		var templateMainMenuMarkup = '<input type="radio" id="radioMenu${id}" name="radioMenu" {{if checked}}checked="checked"{{/if}} {{if disabled}}disabled="disabled"{{/if}}/><label for="radioMenu${id}">${menuName}</label>';
		$.template(templater.templateNameMainMenuItem, templateMainMenuMarkup);
		
		// expects {graphName, graphDescription}
		var templateAccordionGraph = '<h3><a href="#">${graphName}</a></h3><div><p>${graphDescription}</p><p><a _type="vertex" _graph="${graphName}" href="#">Browse Vertices</a></p><p><a _type="edge" _graph="${graphName}" href="#">Browse Edges</a></p></div>';
		$.template(templater.templateNameAccordionGraph, templateAccordionGraph);
		
		// expects {traversalName}
		var templateListTraversal = '<li class="column"><a href="http://www.google.com">${path}</a></li>';
		$.template(templater.templateNameListTraversals, templateListTraversal);
		
		// expects {_id}
		var templateListVertices = '<li class="column"><a href="http://www.google.com">${_id}</a></li>';
		$.template(templater.templateNameListVertices, templateListVertices);
	}
	
	/**
	 * Applies data to a template and appends it to a specific.
	 * 
	 * @param templateName 	{String} The name of the template to render.
	 * @param data 			{Array} The data to render into the template which is a set of objects.
	 * @param target 		{String} A jQuery selector or element  to append the template to. 
	 */
	templater.applyTemplate = function(templateName, data, target) {
		$.tmpl(templateName, data).appendTo(target);
	}
	
	api.initTemplates = templater.init;
	
	/**
	 * Constructs <li> values for an array of vertices.
	 * 
	 * @param data 		{Array} The list of vertex objects to render.
	 * @param selector	{Object} A jQuery selector or element to append the template to.
	 */
	api.applyListVerticesTemplate = function(data, selector) {
		if (data.length > 0) { 
			templater.applyTemplate(templater.templateNameListVertices, data, selector);
		} else {
			templater.applyTemplate(templater.templateNameListVertices, [{ "_id":"No vertices in this graph"}], selector);
		}
	}
	
	/**
	 * Constructs <li> values for an array of traversals.
	 * 
	 * @param data 		{Array} The list of traversal objects to render.
	 * @param selector	{String} A jQuery selector or element to append the template to.
	 */
	api.applyListTraversalsTemplate = function(data, selector) {
		if (data.length > 0) { 
			templater.applyTemplate(templater.templateNameListTraversals, data, selector);
		} else {
			templater.applyTemplate(templater.templateNameListTraversals, [{ "path":"No traversals configured"}], selector);
		}
	}
	
	/**
	 * Constructs accordion menu values for an array of graphs.
	 * 
	 * @param data 		{Array} The list of graph objects to render.
	 * @param selector	{String} A jQuery selector or element to append the template to.
	 */
	api.applyAccordionGraphTemplate = function(data, selector) {
		if (data.length > 0) { 
			templater.applyTemplate(templater.templateNameAccordionGraph, data, selector);
		} else {
			// TODO: need something here if nothing is configured ???
		}
	}
	
	/**
	 * Constructs main menu.
	 * 
	 * @param data 		{Array} The list of menu item objects to render.
	 * @param selector	{String} A jQuery selector or element to append the template to.
	 */
	api.applyMainMenuTemplate = function(data, selector) {
		templater.applyTemplate(templater.templateNameMainMenuItem, data, selector);
	}
};