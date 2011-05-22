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
import org.apache.commons.cli.*;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.io.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.LogManager;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class WebServer {

    private static String characterEncoding;

    private static final String DEFAULT_WEB_ROOT_PATH = "public";
    private static final String DEFAULT_BASE_URI = "http://localhost";
    protected static Logger logger = Logger.getLogger(WebServer.class);

    static {
        PropertyConfigurator.configure(RexsterApplication.class.getResource("log4j.properties"));
    }

    protected GrizzlyWebServer rexsterServer;
    protected GrizzlyWebServer doghouseServer;

    public WebServer(final XMLConfiguration properties, boolean user) throws Exception {
        logger.info(".:Welcome to Rexster:.");

        if (user) {
            this.startUser(properties);
        } else {
            this.start(properties);
        }

        Integer shutdownServerPort = properties.getInteger("rexster-shutdown-port", new Integer(8184));
        String shutdownServerHost = properties.getString("rexster-shutdown-host", "127.0.0.1");
        final ShutdownManager shutdownManager = new ShutdownManager(shutdownServerHost, shutdownServerPort);

        //Register a shutdown hook
        shutdownManager.registerShutdownListener(new ShutdownManager.ShutdownListener() {
            public void shutdown() {
                try {
                    stop();
                } catch (Exception ex) {

                }
            }
        });

        //Start the shutdown listener
        shutdownManager.start();

        //Wait for a shutdown request and all shutdown listeners to complete
        shutdownManager.waitForShutdown();
    }

    public static String getCharacterEncoding() {
        return characterEncoding;
    }

    protected void startUser(final XMLConfiguration properties) throws Exception {
        this.start(properties);
    }

    protected void start(final XMLConfiguration properties) throws Exception {
        WebServerRexsterApplicationProvider.start(properties);
        Integer rexsterServerPort = properties.getInteger("rexster-server-port", new Integer(8182));
        Integer doghouseServerPort = properties.getInteger("doghouse-server-port", new Integer(8183));
        String webRootPath = properties.getString("web-root", DEFAULT_WEB_ROOT_PATH);
        String baseUri = properties.getString("base-uri", DEFAULT_BASE_URI);
        characterEncoding = properties.getString("character-set", "ISO-8859-1");

        final Map<String, String> jerseyInitParameters = getServletInitParameters("web-server-configuration", properties);

        this.rexsterServer = new GrizzlyWebServer(rexsterServerPort);
        this.doghouseServer = new GrizzlyWebServer(doghouseServerPort);

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

        final Map<String, String> adminInitParameters = getServletInitParameters("admin-server-configuration", properties);

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
        webToolAdapter.addInitParameter("com.tinkerpop.rexster.config.root", "/" + webRootPath);
        webToolAdapter.addInitParameter("com.tinkerpop.rexster.config.rexsterApiBaseUri", baseUri + ":" + rexsterServerPort.toString());
        GrizzlyAdapter staticAdapter = new GrizzlyAdapter(absoluteWebRootPath) {
            public void service(GrizzlyRequest req, GrizzlyResponse res) {
            }
        };
        staticAdapter.setHandleStaticResources(true);

        this.rexsterServer.addGrizzlyAdapter(jerseyAdapter, new String[]{""});
        this.doghouseServer.addGrizzlyAdapter(webToolAdapter, new String[]{"/main"});
        this.doghouseServer.addGrizzlyAdapter(visualizationAdapter, new String[]{"/visualize"});
        this.doghouseServer.addGrizzlyAdapter(evaluatorAdapter, new String[]{"/exec"});
        this.doghouseServer.addGrizzlyAdapter(staticAdapter, new String[]{""});

        this.rexsterServer.start();
        this.doghouseServer.start();

        logger.info("Rexster Server running on: [" + baseUri + ":" + rexsterServerPort + "]");
        logger.info("Dog House Server running on: [" + baseUri + ":" + doghouseServerPort + "]");

    }

    /**
     * Extracts servlet configuration parameters from rexster.xml
     */
    private Map<String, String> getServletInitParameters(
            String webServerConfigKey, XMLConfiguration properties) {

        final Map<String, String> initParams = new HashMap<String, String>();

        HierarchicalConfiguration webServerConfig = null;
        try {
            webServerConfig = properties.configurationAt(webServerConfigKey);
        } catch (IllegalArgumentException iae) {
            logger.info("No servlet initialization parameters passed for configuration: " + webServerConfigKey);
        }

        if (webServerConfig != null) {
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
        }

        // give all servlets access to rexster.xml location
        initParams.put("com.tinkerpop.rexster.config", properties.getString("self-xml"));

        return initParams;
    }

    protected void stop() throws Exception {
        this.rexsterServer.stop();
        this.doghouseServer.stop();
        WebServerRexsterApplicationProvider.stop();
    }

    @SuppressWarnings("static-access")
    private static Options getCliOptions() {
        Option help = new Option("help", "print this message");

        Option rexsterFile = OptionBuilder.withArgName("file")
                .hasArg()
                .withDescription("use given file for rexster.xml")
                .create("configuration");

        Option webServerPort = OptionBuilder.withArgName("port")
                .hasArg()
                .withDescription("override port used for rexster-server-port in rexster.xml")
                .create("rexsterport");

        Option adminServerPort = OptionBuilder.withArgName("port")
                .hasArg()
                .withDescription("override port used for doghouse-server-port in rexster.xml")
                .create("doghouseport");

        Option webRoot = OptionBuilder.withArgName("path")
                .hasArg()
                .withDescription("override web-root in rexster.xml")
                .create("webroot");

        Option debug = new Option("debug", "run rexster with full console logging output from jersey");

        Options options = new Options();
        options.addOption(help);
        options.addOption(rexsterFile);
        options.addOption(webServerPort);
        options.addOption(adminServerPort);
        options.addOption(webRoot);
        options.addOption(debug);

        return options;
    }

    private static CommandLine getCliInput(final String[] args) throws Exception {
        Options options = getCliOptions();
        CommandLineParser parser = new GnuParser();
        CommandLine line = null;

        try {
            line = parser.parse(options, args);
        } catch (ParseException exp) {
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

        if (line.hasOption("debug")) {
            // turn on all logging for jersey
            for (String l : Collections.list(LogManager.getLogManager().getLoggerNames())) {
                java.util.logging.Logger.getLogger(l).setLevel(Level.ALL);
            }
        } else {
            // turn off all logging for jersey
            for (String l : Collections.list(LogManager.getLogManager().getLoggerNames())) {
                java.util.logging.Logger.getLogger(l).setLevel(Level.OFF);
            }
        }

        String rexsterXmlFile = "rexster.xml";

        if (line.hasOption("configuration")) {

            rexsterXmlFile = line.getOptionValue("configuration");

            try {
                properties.load(new FileReader(rexsterXmlFile));
            } catch (IOException e) {
                throw new Exception("Could not locate " + rexsterXmlFile + " properties file.");
            }
        } else {
            // no arguments to parse
            properties.load(RexsterApplication.class.getResourceAsStream(rexsterXmlFile));
        }

        // reference the location of the xml file used to configure the server.
        // this will allow the configuration to be passed into components that 
        // do not have access to the configuration file and need it for graph
        // initialization preventing it from having to be explicitly defined
        // in rexster.xml itself.  there's probably an even better way to do
        // this *sigh*
        properties.addProperty("self-xml", rexsterXmlFile);

        // overrides rexster-server-port from command line
        if (line.hasOption("rexsterport")) {
            properties.setProperty("rexster-server-port", line.getOptionValue("rexsterport"));
        }

        // overrides doghouse-server-port from command line
        if (line.hasOption("doghouseport")) {
            properties.setProperty("doghouse-server-port", line.getOptionValue("doghouseport"));
        }

        // overrides web-root from command line	
        if (line.hasOption("webroot")) {
            properties.setProperty("web-root", line.getOptionValue("webroot"));
        }

        new WebServer(properties, true);
    }


}