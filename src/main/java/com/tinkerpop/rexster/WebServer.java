package com.tinkerpop.rexster;

import com.sun.jersey.spi.container.servlet.ServletContainer;
import com.sun.jersey.api.container.grizzly2.GrizzlyServerFactory;
import com.tinkerpop.rexster.protocol.RexProMessageFilter;
import com.tinkerpop.rexster.protocol.SessionFilter;
import com.tinkerpop.rexster.servlet.EvaluatorServlet;
import com.tinkerpop.rexster.servlet.ToolServlet;
import com.tinkerpop.rexster.servlet.VisualizationServlet;
import org.apache.commons.cli.*;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.codehaus.groovy.util.Finalizable;
import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.filterchain.TransportFilter;
import org.glassfish.grizzly.http.server.*;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.nio.transport.TCPNIOTransportBuilder;
import org.glassfish.grizzly.strategies.WorkerThreadIOStrategy;
import org.glassfish.grizzly.utils.EchoFilter;
import org.glassfish.grizzly.servlet.ServletHandler;

import javax.ws.rs.core.UriBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
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

    protected HttpServer rexsterServer;
    protected HttpServer doghouseServer;
    protected TCPNIOTransport rexproServer;

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

    private void startUser(final XMLConfiguration properties) throws Exception {
        this.start(properties);
    }

    private void start(final XMLConfiguration properties) throws Exception {
        WebServerRexsterApplicationProvider.start(properties);
        Integer rexsterServerPort = properties.getInteger("rexster-server-port", new Integer(8182));
        Integer doghouseServerPort = properties.getInteger("doghouse-server-port", new Integer(8183));
        Integer rexproServerPort = properties.getInteger("rexpro-server-port", new Integer(8185));
        String webRootPath = properties.getString("web-root", DEFAULT_WEB_ROOT_PATH);
        String baseUri = properties.getString("base-uri", DEFAULT_BASE_URI);
        characterEncoding = properties.getString("character-set", "ISO-8859-1");

        this.startRexsterServer(properties, baseUri, rexsterServerPort);
        this.startDogHouseServer(properties, webRootPath, doghouseServerPort, baseUri, rexsterServerPort);
        this.startRexProServer(rexproServerPort);

    }

    private void startRexsterServer(final XMLConfiguration properties,
                                    final String baseUri, final Integer rexsterServerPort) throws Exception{
        final Map<String, String> jerseyInitParameters = this.getServletInitParameters("web-server-configuration", properties);

        ServletHandler jerseyHandler = new ServletHandler();
        for (Map.Entry<String, String> entry : jerseyInitParameters.entrySet()) {
            jerseyHandler.addInitParameter(entry.getKey(), entry.getValue());
        }

        jerseyHandler.setContextPath("/");
        jerseyHandler.setServletInstance(new ServletContainer());


        this.rexsterServer = GrizzlyServerFactory.createHttpServer(
                UriBuilder.fromUri(baseUri).port(rexsterServerPort).build(),
                jerseyHandler);
        this.rexsterServer.start();

        logger.info("Rexster Server running on: [" + baseUri + ":" + rexsterServerPort + "]");
    }

    private void startDogHouseServer(final XMLConfiguration properties,
                                     final String webRootPath,
                                     final Integer doghouseServerPort,
                                     final String baseUri,
                                     final Integer rexsterServerPort) throws Exception {
        // servlet that services all url from "main" by simply sending
        // main.html back to the calling client.  main.html handles its own
        // state given the uri
        ServletHandler dogHouseHandler = new ServletHandler();
        dogHouseHandler.setContextPath("/main");
        dogHouseHandler.setServletInstance(new ToolServlet());

        final Map<String, String> adminInitParameters = getServletInitParameters("admin-server-configuration", properties);

        // servlet for gremlin console
        ServletHandler visualizationHandler = new ServletHandler();
        for (Map.Entry<String, String> entry : adminInitParameters.entrySet()) {
            visualizationHandler.addInitParameter(entry.getKey(), entry.getValue());
        }

        visualizationHandler.setContextPath("/visualize");
        visualizationHandler.setServletInstance(new VisualizationServlet());

        // servlet for gremlin console
        ServletHandler evaluatorHandler = new ServletHandler();
        for (Map.Entry<String, String> entry : adminInitParameters.entrySet()) {
            evaluatorHandler.addInitParameter(entry.getKey(), entry.getValue());
        }
        evaluatorHandler.setContextPath("/exec");
        evaluatorHandler.setServletInstance(new EvaluatorServlet());

        String absoluteWebRootPath = (new File(webRootPath)).getAbsolutePath();
        dogHouseHandler.addInitParameter("com.tinkerpop.rexster.config.root", "/" + webRootPath);
        dogHouseHandler.addInitParameter("com.tinkerpop.rexster.config.rexsterApiBaseUri", baseUri + ":" + rexsterServerPort.toString());

        this.doghouseServer = GrizzlyServerFactory.createHttpServer(
                UriBuilder.fromUri(baseUri).port(doghouseServerPort).build(),
                new StaticHttpHandler(absoluteWebRootPath));
        final ServerConfiguration config = this.doghouseServer.getServerConfiguration();
        config.addHttpHandler(dogHouseHandler, "/main");
        config.addHttpHandler(visualizationHandler, "/visualize");
        config.addHttpHandler(evaluatorHandler, "/exec");
        this.doghouseServer.start();

        logger.info("Dog House Server running on: [" + baseUri + ":" + doghouseServerPort + "]");
    }

    private void startRexProServer(final Integer rexproServerPort) throws Exception {
        FilterChainBuilder filterChainBuilder = FilterChainBuilder.stateless();
        filterChainBuilder.add(new TransportFilter());
        filterChainBuilder.add(new RexProMessageFilter());
        filterChainBuilder.add(new SessionFilter());
        filterChainBuilder.add(new EchoFilter());

        this.rexproServer = TCPNIOTransportBuilder.newInstance().build();
        this.rexproServer.setIOStrategy(WorkerThreadIOStrategy.getInstance());
        this.rexproServer.setProcessor(filterChainBuilder.build());
        this.rexproServer.bind(rexproServerPort);

        this.rexproServer.start();

        logger.info("RexPro serving on port: [" + rexproServerPort + "]");
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
        this.rexproServer.stop();
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