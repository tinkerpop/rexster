/** 
 * Credit to http://code.google.com/p/jquery-jsonviewer-plugin/ as the basis for this JSON Viewer
 */
(function($) {

    $.fn.jsonviewer = function(settings) {

        var config =
        {
            'type_prefix': false,
            'json_name': 'unknown',
            'json_data': null,
            'ident' : '12px',
            'inner-padding': '2px',
            'outer-padding': '4px',
            'debug' : false
        };

        if (settings) $.extend(config, settings);

        
        this.each(function(key, element) {
            format_value(element, config['json_name'], config['json_data'], config);
        });

        return this;

    };

    function format_value(element, name, data, config) {
        //debug('name=' + name + "; data=" + data);
    	var isMetaData = name === "_type" || name === "_id" || name === "_outV" || name === "_inV" || name === "_label"; 
    	if (!isMetaData) {
	        var v = new TypeHandler(data);
	        var type_prefix = v.type().charAt(0);
	        var container = $('<div/>');
	        $(container).appendTo(element);
	        $(container).addClass('json-widget').css({'padding': config['outer-padding'], 'padding-left': config['ident'] });
	        $(container).click(function(event) {
	        $(container).children('.json-widget-content').toggleClass('ui-helper-hidden');
	            return false;
	        });
	        
	        if (v.type() == "object" || v.type() == "array") {
	        	var header = $('<div/>');
		        $(header).appendTo(container);
		        $(header).addClass('json-widget-header ui-corner-top')
		            .css({ 'cursor': 'hand', //'float': 'left',
		                'text-align': 'left', 'white-space': 'nowrap'
		            });
		        $(header).text('' + (config['type_prefix'] ? "(" + type_prefix + ")" : "") + name);
	        	
	            var content = $('<div/>');
	            $(content).appendTo(container);
	            $(content).addClass('json-widget-content ui-corner-bottom')
	            .css({ 'white-space': 'nowrap', 'padding': config['inner-padding'] });
	            for (name in data) { format_value(content, name, data[name], config); }
	        }
	        else {
	            var content = $('<div/>');
	            $(content).appendTo(container);
	            $(content).css({ 'overflow': 'hidden', 'white-space': 'nowrap' });
	            $(content).text(name + ': ' + data);
	        }
    	}
    };


    // number, boolean, string, object, array, date
    function TypeHandler(value) {
        this._type = this.get_type(value);
    };

    TypeHandler.prototype.type = function() { return this._type; }

    TypeHandler.prototype.get_type = function(value) {
        var base_type = typeof value;
        var result = "unsupported"
        switch (base_type) {
            case "number": result = base_type; break;
            case "string": result = base_type; break;
            case "boolean": result = base_type; break;
            case "object":
                if (Number.prototype.isPrototypeOf(value)) { result = "number"; break; }
                if (String.prototype.isPrototypeOf(value)) { result = "string"; break; }
                if (Date.prototype.isPrototypeOf(value)) { result = "date"; break; }
                if (Array.prototype.isPrototypeOf(value)) { result = "array"; break; }
                if (Object.prototype.isPrototypeOf(value)) { result = "object"; break; }
        }
        return result;
    };

    //
    // private function for debugging
    //
    function debug(msg) {
        if (window.console && window.console.log)
            window.console.log('debug(jv): ' + msg);
    };
})(jQuery);