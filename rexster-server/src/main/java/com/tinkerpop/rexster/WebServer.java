package com.tinkerpop.rexster;

import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.spi.container.servlet.ServletContainer;
import com.tinkerpop.rexster.filter.AbstractSecurityFilter;
import com.tinkerpop.rexster.filter.DefaultSecurityFilter;
import com.tinkerpop.rexster.filter.HeaderResponseFilter;
import com.tinkerpop.rexster.protocol.RexProSessionMonitor;
import com.tinkerpop.rexster.protocol.filter.RexProMessageFilter;
import com.tinkerpop.rexster.protocol.filter.ScriptFilter;
import com.tinkerpop.rexster.protocol.filter.SessionFilter;
import com.tinkerpop.rexster.server.RexsterApplicationImpl;
import com.tinkerpop.rexster.server.RexsterCommandLine;
import com.tinkerpop.rexster.server.RexsterSettings;
import com.tinkerpop.rexster.server.ShutdownManager;
import com.tinkerpop.rexster.server.WebServerRexsterApplicationProvider;
import com.tinkerpop.rexster.servlet.DogHouseServlet;
import com.tinkerpop.rexster.servlet.EvaluatorServlet;
import com.tinkerpop.rexster.servlet.RexsterStaticHttpHandler;
import com.tinkerpop.rexster.servlet.VisualizationServlet;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.filterchain.TransportFilter;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.http.server.ServerConfiguration;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.nio.transport.TCPNIOTransportBuilder;
import org.glassfish.grizzly.servlet.ServletHandler;
import org.glassfish.grizzly.strategies.WorkerThreadIOStrategy;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.BindException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.LogManager;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class WebServer {

    private static String characterEncoding;

    private static final String DEFAULT_WEB_ROOT_PATH = "public";
    private static final String DEFAULT_BASE_URI = "http://localhost";
    private static Logger logger = Logger.getLogger(WebServer.class);

    static {
        PropertyConfigurator.configure(RexsterApplication.class.getResource("log4j.properties"));
    }

    protected HttpServer rexsterServer;
    protected TCPNIOTransport rexproServer;

    public WebServer(final XMLConfiguration properties) throws Exception {
        this.start(properties);

        // initialize the session monitor for rexpro to clean up dead sessions.
        final Long rexProSessionMaxIdle = properties.getLong("rexpro-session-max-idle", new Long(1790000));
        final Long rexProSessionCheckInterval = properties.getLong("rexpro-session-check-interval", new Long(3000000));
        new RexProSessionMonitor(rexProSessionMaxIdle, rexProSessionCheckInterval);

        final Integer shutdownServerPort = properties.getInteger("rexster-shutdown-port", new Integer(8183));
        final String shutdownServerHost = properties.getString("rexster-shutdown-host", "127.0.0.1");
        final ShutdownManager shutdownManager = new ShutdownManager(shutdownServerHost, shutdownServerPort);

        //Register a shutdown hook
        shutdownManager.registerShutdownListener(new ShutdownManager.ShutdownListener() {
            public void shutdown() {
                // shutdown grizzly/graphs
                stop();
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

    private void start(final XMLConfiguration properties) throws Exception {
        WebServerRexsterApplicationProvider.start(properties);
        final Integer rexsterServerPort = properties.getInteger("rexster-server-port", new Integer(8182));
        final String rexsterServerHost = properties.getString("rexster-server-host", "0.0.0.0");
        final Integer rexproServerPort = properties.getInteger("rexpro-server-port", new Integer(8184));
        final String rexproServerHost = properties.getString("rexpro-server-host", "0.0.0.0");
        final String webRootPath = properties.getString("web-root", DEFAULT_WEB_ROOT_PATH);
        final String baseUri = properties.getString("base-uri", DEFAULT_BASE_URI);
        characterEncoding = properties.getString("character-set", "ISO-8859-1");

        this.startRexsterServer(properties, baseUri, rexsterServerPort, rexsterServerHost, webRootPath);
        this.startRexProServer(properties, rexproServerPort, rexproServerHost);

    }

    private void startRexsterServer(final XMLConfiguration properties,
                                    final String baseUri,
                                    final Integer rexsterServerPort,
                                    final String rexsterServerHost,
                                    final String webRootPath) throws Exception {

        ServletHandler jerseyHandler = new ServletHandler();
        jerseyHandler.addInitParameter("com.sun.jersey.config.property.packages", "com.tinkerpop.rexster");
        jerseyHandler.addInitParameter(ResourceConfig.PROPERTY_CONTAINER_RESPONSE_FILTERS, HeaderResponseFilter.class.getName());
        jerseyHandler.addInitParameter("com.tinkerpop.rexster.config", properties.getString("self-xml"));

        HierarchicalConfiguration securityConfiguration = properties.configurationAt("security.authentication");
        String securityFilterType = securityConfiguration.getString("type");
        if (!securityFilterType.equals("none")) {
            if (securityFilterType.equals("default")) {
                jerseyHandler.addInitParameter(ResourceConfig.PROPERTY_CONTAINER_REQUEST_FILTERS, DefaultSecurityFilter.class.getName());
            } else {
                jerseyHandler.addInitParameter(ResourceConfig.PROPERTY_CONTAINER_REQUEST_FILTERS, securityFilterType);
            }
        }

        jerseyHandler.setContextPath("/");
        jerseyHandler.setServletInstance(new ServletContainer());

        WebServerRexsterApplicationProvider provider = new WebServerRexsterApplicationProvider();
        RexsterApplication application = provider.getValue();

        // servlet that services all url from "main" by simply sending
        // main.html back to the calling client.  main.html handles its own
        // state given the uri
        ServletHandler dogHouseHandler = new ServletHandler();
        dogHouseHandler.setContextPath("/doghouse/main");
        dogHouseHandler.setServletInstance(new DogHouseServlet());

        // servlet for gremlin console
        ServletHandler visualizationHandler = new ServletHandler();
        visualizationHandler.addInitParameter("com.tinkerpop.rexster.config", properties.getString("self-xml"));

        visualizationHandler.setContextPath("/doghouse/visualize");
        visualizationHandler.setServletInstance(new VisualizationServlet(application));

        // servlet for gremlin console
        ServletHandler evaluatorHandler = new ServletHandler();
        evaluatorHandler.addInitParameter("com.tinkerpop.rexster.config", properties.getString("self-xml"));

        evaluatorHandler.setContextPath("/doghouse/exec");
        evaluatorHandler.setServletInstance(new EvaluatorServlet(application));

        String absoluteWebRootPath = (new File(webRootPath)).getAbsolutePath();
        dogHouseHandler.addInitParameter("com.tinkerpop.rexster.config.rexsterApiBaseUri", baseUri + ":" + rexsterServerPort.toString());

        this.rexsterServer = new HttpServer();
        final ServerConfiguration config = this.rexsterServer.getServerConfiguration();
        config.addHttpHandler(jerseyHandler, "/");
        config.addHttpHandler(new RexsterStaticHttpHandler(absoluteWebRootPath), "/doghouse");
        config.addHttpHandler(dogHouseHandler, "/doghouse/main/*");
        config.addHttpHandler(visualizationHandler, "/doghouse/visualize");
        config.addHttpHandler(evaluatorHandler, "/doghouse/exec");

        final NetworkListener listener = new NetworkListener("grizzly", rexsterServerHost, rexsterServerPort);
        this.rexsterServer.addListener(listener);

        this.rexsterServer.start();

        logger.info("Rexster Server running on: [" + baseUri + ":" + rexsterServerPort + "]");
    }

    private void startRexProServer(final XMLConfiguration properties,
                                   final Integer rexproServerPort,
                                   final String rexproServerHost) throws Exception {
        final FilterChainBuilder filterChainBuilder = FilterChainBuilder.stateless();
        filterChainBuilder.add(new TransportFilter());
        filterChainBuilder.add(new RexProMessageFilter());

        final HierarchicalConfiguration securityConfiguration = properties.configurationAt("security.authentication");
        final String securityFilterType = securityConfiguration.getString("type");
        if (securityFilterType.equals("none")) {
            logger.info("Rexster configured with no security.");
        } else {
            final AbstractSecurityFilter filter;
            if (securityFilterType.equals("default")) {
                filter = new DefaultSecurityFilter();
                filterChainBuilder.add(filter);
            } else {
                filter = (AbstractSecurityFilter) Class.forName(securityFilterType).newInstance();
                filterChainBuilder.add(filter);
            }

            filter.configure(properties);

            logger.info("Rexster configured with [" + filter.getName() + "].");
        }

        final WebServerRexsterApplicationProvider provider = new WebServerRexsterApplicationProvider();
        final RexsterApplication application = provider.getValue();

        filterChainBuilder.add(new SessionFilter(application));
        filterChainBuilder.add(new ScriptFilter(application));

        this.rexproServer = TCPNIOTransportBuilder.newInstance().build();
        this.rexproServer.setIOStrategy(WorkerThreadIOStrategy.getInstance());
        this.rexproServer.setProcessor(filterChainBuilder.build());
        this.rexproServer.bind(rexproServerHost, rexproServerPort);

        this.rexproServer.start();

        logger.info("RexPro serving on port: [" + rexproServerPort + "]");
    }

    protected void stop() {
        try {
            this.rexsterServer.stop();
        } catch (Exception ex) {
            logger.debug("Error shutting down Rexster Server ignored.", ex);
        }
        
        try {
            this.rexproServer.stop();
        } catch (Exception ex) {
            logger.debug("Error shutting down RexPro Server ignored.", ex);
        }
        
        try {
            WebServerRexsterApplicationProvider.stop();
        } catch (Exception ex) {
            logger.warn("Error while shutting down graphs.  All graphs may not have been shutdown cleanly.");
        }
    }

    public static void main(final String[] args)  {

        logger.info(".:Welcome to Rexster:.");

        final XMLConfiguration properties = new XMLConfiguration();
        final RexsterSettings settings = new RexsterSettings(args);
        final RexsterCommandLine line = settings.getCommand();

        if (line.getCommand().hasOption("start")) {
            if (line.hasCommandParameters() && line.getCommandParameters().hasOption("debug")) {
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

            final boolean rexsterXmlConfiguredFromCommandLine = line.hasCommandParameters() && line.getCommandParameters().hasOption("configuration");
            final String rexsterXmlFileLocation = rexsterXmlConfiguredFromCommandLine ? line.getCommandParameters().getOptionValue("configuration") : "rexster.xml";
            final File rexsterXmlFile = new File(rexsterXmlFileLocation);
            
            try {
                // load either the rexster.xml from the command line or the default rexster.xml in the root of the
                // working directory
                properties.load(new FileReader(rexsterXmlFileLocation));
                logger.info("Using [" + rexsterXmlFile.getAbsolutePath() + "] as configuration source.");
            } catch (Exception e) {
                logger.warn("Could not load configuration from [" + rexsterXmlFile.getAbsolutePath() + "]");
                
                if (rexsterXmlConfiguredFromCommandLine) {
                    // since an explicit value for rexster.xml was supplied and could not be found then 
                    // we won't continue to load rexster.
                    throw new RuntimeException("Could not load configuration from [" + rexsterXmlFile.getAbsolutePath() + "]");
                } else {
                    // since the default value for rexster.xml was supplied and could not be found, try to 
                    // revert to the rexster.xml stored as a resource.  a good fall back for users just 
                    // getting started with rexster.
                    try {
                        properties.load(RexsterApplication.class.getResourceAsStream(rexsterXmlFileLocation));
                        logger.info("Using [" + rexsterXmlFileLocation + "] resource as configuration source.");
                    } catch (Exception ex){
                        logger.fatal("None of the default rexster.xml can be found or read.");
                    }
                }
            }

            // reference the location of the xml file used to configure the server.
            // this will allow the configuration to be passed into components that
            // do not have access to the configuration file and need it for graph
            // initialization preventing it from having to be explicitly defined
            // in rexster.xml itself.  there's probably an even better way to do
            // this *sigh*
            properties.addProperty("self-xml", rexsterXmlFileLocation);

            // overrides rexster-server-port from command line
            if (line.hasCommandParameters() && line.getCommandParameters().hasOption("rexsterport")) {
                properties.setProperty("rexster-server-port", line.getCommandParameters().getOptionValue("rexsterport"));
            }

            // overrides web-root from command line
            if (line.hasCommandParameters() && line.getCommandParameters().hasOption("webroot")) {
                properties.setProperty("web-root", line.getCommandParameters().getOptionValue("webroot"));
            }

            try {
                new WebServer(properties);
            } catch (BindException be) {
                logger.fatal("Could not start Rexster Server.  A port that Rexster needs is in use.");
            } catch (Exception ex) {
                logger.fatal("The Rexster Server could not be started", ex);
            }
        } else if (line.getCommand().hasOption("version")) {
            System.out.println("Rexster version [" + RexsterApplicationImpl.getVersion() + "]");
        } else if (line.getCommand().hasOption("stop")) {
            if (line.hasCommandParameters() && line.getCommandParameters().hasOption("wait")) {
                issueControlCommand(line, ShutdownManager.COMMAND_SHUTDOWN_WAIT);
            } else {
                issueControlCommand(line, ShutdownManager.COMMAND_SHUTDOWN_NO_WAIT);
            }
        } else if (line.getCommand().hasOption("status")) {
            issueControlCommand(line, ShutdownManager.COMMAND_STATUS);
        } else {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("rexster", line.getCommandOptions());
        }
    }

    private static void issueControlCommand(RexsterCommandLine line, String command) {
        String host = "127.0.0.1";
        int port = 8183;

        if (line.hasCommandParameters() && line.getCommandParameters().hasOption("host")) {
            host = line.getCommandParameters().getOptionValue("host");
        }

        if (line.hasCommandParameters() && line.getCommandParameters().hasOption("port")) {
            String portString = line.getCommandParameters().getOptionValue("port");

            try {
                port = Integer.parseInt(portString);
            } catch (NumberFormatException nfe) {
                logger.warn("The value of the <port> parameter was not a valid value.  Utilizing the default port of " + port + ".");
            }
        }

        if (line.hasCommandParameters() && line.getCommandParameters().hasOption("cmd")) {
            command = line.getCommandParameters().getOptionValue("cmd");

        }

        Socket shutdownConnection = null;
        try {
            final InetAddress hostAddress = InetAddress.getByName(host);
            shutdownConnection = new Socket(hostAddress, port);

            shutdownConnection.setSoTimeout(5000);
            final BufferedReader reader = new BufferedReader(new InputStreamReader(shutdownConnection.getInputStream()));
            final PrintStream writer = new PrintStream(shutdownConnection.getOutputStream());
            try {
                writer.println(command);
                writer.flush();

                while (true) {
                    final String theLine = reader.readLine();
                    if (theLine == null) {
                        break;
                    }

                    System.out.println(theLine);
                }

            } finally {
                IOUtils.closeQuietly(reader);
                IOUtils.closeQuietly(writer);
            }
        } catch (IOException ioe) {
            System.out.println("Cannot connect to Rexster Server to issue command.  It may not be running.");
        } finally {
            try {
                if (shutdownConnection != null) {
                    shutdownConnection.close();
                }
            } catch (IOException ioe) {
            }
        }
    }
}
