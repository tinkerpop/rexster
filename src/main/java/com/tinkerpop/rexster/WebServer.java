package com.tinkerpop.rexster;

import com.sun.grizzly.http.embed.GrizzlyWebServer;
import com.sun.grizzly.http.servlet.ServletAdapter;
import com.sun.grizzly.tcp.http11.GrizzlyAdapter;
import com.sun.grizzly.tcp.http11.GrizzlyRequest;
import com.sun.grizzly.tcp.http11.GrizzlyResponse;
import com.sun.jersey.spi.container.servlet.ServletContainer;

import com.tinkerpop.rexster.servlet.EvaluatorServlet;
import com.tinkerpop.rexster.servlet.ToolServlet;
import com.tinkerpop.rexster.servlet.VisualizationServlet;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class WebServer {

    private static final String DEFAULT_WEB_ROOT_PATH = "public";
    protected static Logger logger = Logger.getLogger(WebServer.class);

    static {
        PropertyConfigurator.configure(RexsterApplication.class.getResource("log4j.properties"));
    }

    protected GrizzlyWebServer server;
    protected GrizzlyWebServer adminServer;

    public WebServer(final XMLConfiguration properties, boolean user) throws Exception {
        logger.info(".:Welcome to Rexster:.");
        if (user) {
            this.startUser(properties);
        } else {
            this.start(properties);
        }
    }

    protected void startUser(final XMLConfiguration properties) throws Exception {
        this.start(properties);
        // user interaction to shutdown server thread
        logger.info("Hit <enter> to shutdown Rexster");
        System.in.read();
        logger.info("Shutting down Rexster");
        this.stop();
        System.exit(0);
    }

    protected void start(final XMLConfiguration properties) throws Exception {
    	WebServerRexsterApplicationProvider.start(properties);
        Integer port = properties.getInteger("webserver-port", new Integer(8182));
        Integer adminPort = properties.getInteger("adminserver-port", new Integer(8183));
        String webRootPath = properties.getString("web-root", DEFAULT_WEB_ROOT_PATH);

        HierarchicalConfiguration webServerConfig = properties.configurationAt("web-server-configuration");
        final Map<String, String> jerseyInitParameters = getServletInitParameters(webServerConfig);

        this.server = new GrizzlyWebServer(port);
        this.adminServer = new GrizzlyWebServer(adminPort);

        ServletAdapter jerseyAdapter = new ServletAdapter();
        for (Map.Entry<String, String> entry : jerseyInitParameters.entrySet()) {
            jerseyAdapter.addInitParameter(entry.getKey(), entry.getValue());
        }

        jerseyAdapter.setContextPath("/");
        jerseyAdapter.setServletInstance(new ServletContainer());

        // servlet that services all url from "main" by simply sending
        // main.html back to the calling client.  main.html handles its own
        // state given the uri
        ServletAdapter webToolAdapter = new ServletAdapter();
        webToolAdapter.setContextPath("/main");
        webToolAdapter.setServletInstance(new ToolServlet());
        webToolAdapter.setHandleStaticResources(false);
        
        HierarchicalConfiguration adminServerConfig = properties.configurationAt("admin-server-configuration");
        final Map<String, String> adminInitParameters = getServletInitParameters(webServerConfig);
        
        // servlet for gremlin console
        ServletAdapter visualizationAdapter = new ServletAdapter();
        for (Map.Entry<String, String> entry : adminInitParameters.entrySet()) {
        	visualizationAdapter.addInitParameter(entry.getKey(), entry.getValue());
        }
        
        visualizationAdapter.setContextPath("/visualize");
        visualizationAdapter.setServletInstance(new VisualizationServlet());
        visualizationAdapter.setHandleStaticResources(false);
        
        // servlet for gremlin console
        ServletAdapter evaluatorAdapter = new ServletAdapter();
        for (Map.Entry<String, String> entry : adminInitParameters.entrySet()) {
        	evaluatorAdapter.addInitParameter(entry.getKey(), entry.getValue());
        }
        evaluatorAdapter.setContextPath("/exec");
        evaluatorAdapter.setServletInstance(new EvaluatorServlet());
        evaluatorAdapter.setHandleStaticResources(false);
        
        String absoluteWebRootPath = (new File(webRootPath)).getAbsolutePath();
        webToolAdapter.addInitParameter("root", "/" + webRootPath);
        GrizzlyAdapter staticAdapter = new GrizzlyAdapter(absoluteWebRootPath)
        {
            public void service(GrizzlyRequest req, GrizzlyResponse res )
            {
            }
        };
        staticAdapter.setHandleStaticResources(true);

        this.server.addGrizzlyAdapter(jerseyAdapter, new String[]{""});
        this.adminServer.addGrizzlyAdapter(webToolAdapter, new String[]{"/main"});
        this.adminServer.addGrizzlyAdapter(visualizationAdapter, new String[]{"/visualize"});
        this.adminServer.addGrizzlyAdapter(evaluatorAdapter, new String[]{"/exec"});
        this.adminServer.addGrizzlyAdapter(staticAdapter, new String[]{""});

        this.server.start();
        this.adminServer.start();

        logger.info("Server running on:" + port);
        logger.info("Admin server running on:" + adminPort);

    }

	private Map<String, String> getServletInitParameters(
			HierarchicalConfiguration webServerConfig) {
		final Map<String, String> initParams = new HashMap<String, String>();
        Iterator keys = webServerConfig.getKeys();
        while (keys.hasNext()) {
            String key = keys.next().toString();

            // commons config double dots keys with just one period in it.
            // as that represents a path statement for that lib.  need to remove
            // the double dot so that it can pass directly to grizzly.  hopefully
            // there are no cases where this will cause a problem and a double dot
            // is always expected
            String grizzlyKey = key.replace("..", ".");
            String configValue = webServerConfig.getString(key);
            initParams.put(grizzlyKey, configValue);

            logger.info("Web Server configured with " + key + ": " + configValue);
        }
		return initParams;
	}

    protected void stop() throws Exception {
        this.server.stop();
        this.adminServer.stop();
        WebServerRexsterApplicationProvider.stop();
    }

	@SuppressWarnings("static-access")
    private static Options getCliOptions() {
    	Option help = new Option( "help", "print this message" );
    	
		Option rexsterFile  = OptionBuilder.withArgName("file")
									       .hasArg()
									       .withDescription("use given file for rexster.xml")
									       .create("configuration");
		
		Option webServerPort  = OptionBuilder.withArgName("port")
										     .hasArg()
										     .withDescription("override port used for webserver-port in rexster.xml")
										     .create("webserverport");
		
		Option adminServerPort  = OptionBuilder.withArgName("port")
										       .hasArg()
										       .withDescription("override port used for adminserver-port in rexster.xml")
										       .create("adminserverport");

		Option cacheMaxSize  = OptionBuilder.withArgName("max-size")
									        .hasArg()
	 								        .withDescription("override cache-maxsize in rexster.xml")
	 								        .create("cachemaxsize");
		
		Option webRoot  = OptionBuilder.withArgName("path")
								       .hasArg()
 								       .withDescription("override web-root in rexster.xml")
 								       .create("webroot");
    	
		Options options = new Options();
		options.addOption(help);
		options.addOption(rexsterFile);
		options.addOption(webServerPort);
		options.addOption(adminServerPort);
		options.addOption(cacheMaxSize);
		options.addOption(webRoot);
		
		return options;
    }
	
	private static CommandLine getCliInput(final String[] args) throws Exception {
		Options options = getCliOptions();
		CommandLineParser parser = new GnuParser();
		CommandLine line = null;
		
		try {
		    line = parser.parse(options, args);
		}
		catch(ParseException exp) {
			throw new Exception("Parsing failed.  Reason: " + exp.getMessage());
		}
		
		if (line.hasOption("help")) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("rexster", options);
			System.exit(0);
		}
		
		return line;
	}

    public static void main(final String[] args) throws Exception {
        XMLConfiguration properties = new XMLConfiguration();
        
        CommandLine line = getCliInput(args);
        
        if (line.hasOption("configuration")) {
	        
	        String rexsterXmlFile = line.getOptionValue("configuration");
	        
            try {
                properties.load(new FileReader(rexsterXmlFile));
            } catch (IOException e) {
                throw new Exception("Could not locate " + rexsterXmlFile + " properties file.");
            }
        } else {
        	// no arguments to parse
            properties.load(RexsterApplication.class.getResourceAsStream("rexster.xml"));
        }
        
        // overrides webserver-port from command line
        if (line.hasOption("webserverport")) {
        	properties.setProperty("webserver-port", line.getOptionValue("webserverport"));
        }
        
        // overrides adminserver-port from command line
        if (line.hasOption("adminserverport")) {
        	properties.setProperty("adminserver-port", line.getOptionValue("adminserverport"));
        }
        
        // overrides cache-maxsize from command line
        if (line.hasOption("cachemaxsize")) {
        	properties.setProperty("cache-maxsize", line.getOptionValue("cachemaxsize"));
        }
        
        // overrides web-root from command line	
        if (line.hasOption("webroot")) {
        	properties.setProperty("web-root", line.getOptionValue("webroot"));
        }
        
        new WebServer(properties, true);
    }

	
}