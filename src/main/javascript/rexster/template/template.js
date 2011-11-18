/**
 * Compiles templates for later use.
 */
Rexster.modules.template = function(api) {
	
	var templater = {};
	
	templater.templateNameMainMenuItem = "mainMenuItem";
	templater.templateNameMenuGraph = "menuGraph";
	templater.templateNameListVertices = "listVertices";
	templater.templateNameListVertexViewInEdgeList = "listVertexViewInEdgeList";
	templater.templateNameListVertexViewOutEdgeList = "listVertexViewOutEdgeList";
	templater.templateNameListExtensionList = "listExtensionList";
	templater.templateNameListExtensionParameterEntries = "listExtensionParameterEntries";
	
	templater.init = function() {
		// expects {id, menuName, [checked], [disabled]}
		var templateMainMenuMarkup = '<input type="radio" id="radioMenu${id}" value="${id}" name="radioMenu" {{if checked}}checked="checked"{{/if}} {{if disabled}}disabled="disabled"{{/if}}/><label for="radioMenu${id}">${menuName}</label>';
		$.template(templater.templateNameMainMenuItem, templateMainMenuMarkup);
		
		// expects {menuName, panel}
		var templateMenuGraph = '<div id="graphItem${panel}${menuName}" class="graph-item ui-state-default ui-corner-all" style="cursor:pointer;padding:2px;margin:1px"><a href="/doghouse/main/${panel}/${menuName}">${menuName}</a></div>';
		$.template(templater.templateNameMenuGraph, templateMenuGraph);
		
		// expects {_id}
		var templateListVertices = '<li class="column"><a href="http://www.google.com">${_id}</a></li>';
		$.template(templater.templateNameListVertices, templateListVertices);
		
		// expects {_label, _inV, _outV, _id, currentGraphName}
		var templateListVertexViewInEdgeList = '<li>${_outV} - <a href="/doghouse/main/graph/${currentGraphName}/edges/${_id}">${_label}</a> - <a href="/doghouse/main/graph/${currentGraphName}/vertices/${_inV}">${_inV}</a></li>';
		$.template(templater.templateNameListVertexViewInEdgeList, templateListVertexViewInEdgeList);
		
		// expects {_label, _outV, _inV, _id, currentGraphName}
		var templateListVertexViewOutEdgeList = '<li><a href="/doghouse/main/graph/${currentGraphName}/vertices/${_outV}">${_outV}</a> - <a href="/doghouse/main/graph/${currentGraphName}/edges/${_id}">${_label}</a> - ${_inV}</li>';
		$.template(templater.templateNameListVertexViewOutEdgeList, templateListVertexViewOutEdgeList);

        // expects {href, title, parameters[]}
		var templateListExtensionList = '<h3><a href="#" {{if description}}title="${description}"{{/if}}>${title}</a></h3><div><form><fieldset><label for="extensionUri">Extension URI</label><input type="text" name="extensionUri" class="text ui-widget-content ui-corner-all" value="${href}" />{{each parameters}}<label for="${name}">${name}</label><input type="text" name="${name}" class="text ui-widget-content ui-corner-all" title="${description}" />{{/each}}</fieldset></form><a href="#" title="${title}">Execute</a></div>';
		$.template(templater.templateNameListExtensionList, templateListExtensionList);

        // expects {name, description}
		var templateListExtensionParameterEntries = '<label for="${name}">${name}</label><input type="text" name="${name}" class="text ui-widget-content ui-corner-all" title="${description}" />';
		$.template(templater.templateNameListExtensionParameterEntries, templateListExtensionParameterEntries);

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
	 * Constructs graph menu values for an array of graphs.
	 * 
	 * @param data 		{Array} The list of graph objects to render.
	 * @param selector	{String} A jQuery selector or element to append the template to.
	 */
	api.applyMenuGraphTemplate = function(data, selector) {
		if (data.length > 0) { 
			templater.applyTemplate(templater.templateNameMenuGraph, data, selector);
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
	
	api.applyListVertexViewOutEdgeListTempate = function(data, selector) {
		templater.applyTemplate(templater.templateNameListVertexViewOutEdgeList, data, selector);
	}
	
	api.applyListVertexViewInEdgeListTempate = function(data, selector) {
		templater.applyTemplate(templater.templateNameListVertexViewInEdgeList, data, selector);
	}

	api.applyListExtensionList = function(data, selector) {
	    templater.applyTemplate(templater.templateNameListExtensionList,
	                            data.filter(function(extension){ return extension.op === "GET" }), selector);
	}

	api.applyListExtensionParameterEntries = function(data, selector) {
	    templater.applyTemplate(templater.templateNameListExtensionParameterEntries, data, selector);
	}

};