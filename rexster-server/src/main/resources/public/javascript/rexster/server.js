define(
    [
    ],
    function () {
        // public methods
        return {
            servers : [{
                           serverName : "localhost",
                           uri : BASE_URI
                       }],
	        getBaseUri : function(ix){
                return this.servers[ix].uri;
            }
        };
    });