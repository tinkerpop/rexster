/**
 * Provides a list of servers that will the admin app is monitoring.  Currently,
 * it will monitor a single server...the one it is installed on.  
 */
Rexster.modules.server = function(api) {
	api.servers = [
	                   { 
	                	   serverName : "localhost",
	                	   uri : "http://localhost:8182/"
	                   }
	                  ];
	api.getBaseUri = function(ix){
		return this.servers[ix].uri;
	}
};