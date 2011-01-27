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
    private static RexsterApplication rexster;

    static {
        PropertyConfigurator.configure(RexsterApplication.class.getResource("log4j.properties"));
    }

    protected GrizzlyWebServer server;
    protected GrizzlyWebServer adminServer;
    
    public WebServer(final XMLConfiguration properties, boolean user) throws Exception {
        logger.info(".:Welcome to Rexster:.");
        if (user)
            this.startUser(properties);
        else
            this.start(properties);
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
        rexster = new RexsterApplication(properties);
        Integer port = properties.getInteger("webserver-port", new Integer(8182));
        Integer adminPort = properties.getInteger("adminserver-port", new Integer(8183));
        String webRootPath = properties.getString("web-root", DEFAULT_WEB_ROOT_PATH);

        HierarchicalConfiguration webServerConfig = properties.configurationAt("web-server-configuration");

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

        this.server = new GrizzlyWebServer(port);
        this.adminServer = new GrizzlyWebServer(adminPort);
        
        ServletAdapter jerseyAdapter = new ServletAdapter();
        for (Map.Entry<String, String> entry : initParams.entrySet()) {
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
        
        // servlet for gremlin console
        ServletAdapter visualizationAdapter = new ServletAdapter();
        visualizationAdapter.setContextPath("/visualize");
        visualizationAdapter.setServletInstance(new VisualizationServlet());
        visualizationAdapter.setHandleStaticResources(false);
        
        // servlet for gremlin console
        ServletAdapter evaluatorAdapter = new ServletAdapter();
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

    protected void stop() throws Exception {
    	this.server.stop();
    	this.adminServer.stop();
        rexster.stop();
    }

    public static RexsterApplication getRexsterApplication() {
        return rexster;
    }

    public static void main(final String[] args) throws Exception {
        XMLConfiguration properties = new XMLConfiguration();
        if (args.length == 1) {
            try {
                properties.load(new FileReader(args[0]));
            } catch (IOException e) {
                throw new Exception("Could not locate " + args[0] + " properties file.");
            }
        } else {
            properties.load(RexsterApplication.class.getResourceAsStream("rexster.xml"));
        }

        new WebServer(properties, true);
    }
}