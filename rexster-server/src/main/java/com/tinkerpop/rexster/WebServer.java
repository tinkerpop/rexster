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
import org.glassfish.grizzly.utils.EchoFilter;

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
    protected HttpServer doghouseServer;
    protected TCPNIOTransport rexproServer;

    public WebServer(final XMLConfiguration properties, boolean user) throws Exception {
        logger.info(".:Welcome to Rexster:.");

        if (user) {
            this.startUser(properties);
        } else {
            this.start(properties);
        }

        // initialize teh session monitor for rexpro to clean up dead sessions.
        Long rexProSessionMaxIdle = properties.getLong("rexpro-session-max-idle", new Long(1790000));
        Long rexProSessionCheckInterval = properties.getLong("rexpro-session-check-interval", new Long(3000000));
        new RexProSessionMonitor(rexProSessionMaxIdle, rexProSessionCheckInterval);

        Integer shutdownServerPort = properties.getInteger("rexster-shutdown-port", new Integer(8183));
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
        RexsterApplication rexsterApplication = WebServerRexsterApplicationProvider.start(properties);
        final Integer rexsterServerPort = properties.getInteger("rexster-server-port", new Integer(8182));
        final String rexsterServerHost = properties.getString("rexster-server-host", "0.0.0.0");
        final Integer rexproServerPort = properties.getInteger("rexpro-server-port", new Integer(8184));
        final String webRootPath = properties.getString("web-root", DEFAULT_WEB_ROOT_PATH);
        final String baseUri = properties.getString("base-uri", DEFAULT_BASE_URI);
        characterEncoding = properties.getString("character-set", "ISO-8859-1");

        this.startRexsterServer(properties, baseUri, rexsterServerPort, rexsterServerHost, webRootPath);
        this.startRexProServer(properties, rexproServerPort, rexsterApplication);

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
        visualizationHandler.setServletInstance(new VisualizationServlet());

        // servlet for gremlin console
        ServletHandler evaluatorHandler = new ServletHandler();
        evaluatorHandler.addInitParameter("com.tinkerpop.rexster.config", properties.getString("self-xml"));

        evaluatorHandler.setContextPath("/doghouse/exec");
        evaluatorHandler.setServletInstance(new EvaluatorServlet());

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

    private void startRexProServer(final XMLConfiguration properties, final Integer rexproServerPort,
                                   final RexsterApplication rexsterApplication) throws Exception {
        FilterChainBuilder filterChainBuilder = FilterChainBuilder.stateless();
        filterChainBuilder.add(new TransportFilter());
        filterChainBuilder.add(new RexProMessageFilter());

        HierarchicalConfiguration securityConfiguration = properties.configurationAt("security.authentication");
        String securityFilterType = securityConfiguration.getString("type");
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

        filterChainBuilder.add(new SessionFilter(rexsterApplication));
        filterChainBuilder.add(new ScriptFilter());
        filterChainBuilder.add(new EchoFilter());

        this.rexproServer = TCPNIOTransportBuilder.newInstance().build();
        this.rexproServer.setIOStrategy(WorkerThreadIOStrategy.getInstance());
        this.rexproServer.setProcessor(filterChainBuilder.build());
        this.rexproServer.bind(rexproServerPort);

        this.rexproServer.start();

        logger.info("RexPro serving on port: [" + rexproServerPort + "]");
    }

    protected void stop() throws Exception {
        this.rexsterServer.stop();
        this.doghouseServer.stop();
        this.rexproServer.stop();
        WebServerRexsterApplicationProvider.stop();
    }

    @SuppressWarnings("static-access")
    private static Options getCliOptions() {
        Option help = new Option("h", "help", false, "print this message");

        Option rexsterStart = OptionBuilder.withArgName("parameters")
                .hasOptionalArgs()
                .withDescription("start rexster (learn more with start -h)")
                .withLongOpt("start")
                .create("s");

        Option rexsterStop = OptionBuilder.withArgName("parameters")
                .hasOptionalArgs()
                .withDescription("stop rexster (learn more with stop -h)")
                .withLongOpt("stop")
                .create("x");

        Option rexsterStatus = OptionBuilder.withArgName("parameters")
                .hasOptionalArgs()
                .withDescription("status of rexster (learn more with status -h)")
                .withLongOpt("status")
                .create("u");

        Option rexsterVersion = new Option("v", "version", false, "print the version of rexster server");

        Options options = new Options();
        options.addOption(rexsterStart);
        options.addOption(rexsterStop);
        options.addOption(rexsterStatus);
        options.addOption(rexsterVersion);
        options.addOption(help);

        return options;
    }

    @SuppressWarnings("static-access")
    private static Options getStartCliOptions() {
        Option help = new Option("h", "help", false, "print this message");

        Option rexsterFile = OptionBuilder.withArgName("file")
                .hasArg()
                .withDescription("use given file for rexster.xml")
                .withLongOpt("configuration")
                .create("c");

        Option webServerPort = OptionBuilder.withArgName("port")
                .hasArg()
                .withDescription("override port used for rexster-server-port in rexster.xml")
                .withLongOpt("rexsterport")
                .create("rp");

        Option webRoot = OptionBuilder.withArgName("path")
                .hasArg()
                .withDescription("override web-root in rexster.xml")
                .withLongOpt("webroot")
                .create("wr");

        Option debug = new Option("d", "debug", false, "run rexster with full console logging output from jersey");

        Options options = new Options();
        options.addOption(help);
        options.addOption(rexsterFile);
        options.addOption(webServerPort);
        options.addOption(webRoot);
        options.addOption(debug);

        return options;
    }

    @SuppressWarnings("static-access")
    private static Options getStopCliOptions() {
        Option help = new Option("h", "help", false, "print this message");

        Option rexsterFile = OptionBuilder.withArgName("host-name")
                .hasArg()
                .withDescription("rexster web server hostname or ip address (default is 127.0.0.1)")
                .withLongOpt("rexsterhost")
                .create("rh");

        Option webServerPort = OptionBuilder.withArgName("port")
                .hasArg()
                .withDescription("rexster web server shutdown port (default is 8183)")
                .withLongOpt("rexsterport")
                .create("rp");

        Option stopAndWait = new Option("w", "wait", false, "wait for server confirmation of shutdown");

        Options options = new Options();
        options.addOption(help);
        options.addOption(rexsterFile);
        options.addOption(webServerPort);
        options.addOption(stopAndWait);

        return options;
    }

    @SuppressWarnings("static-access")
    private static Options getStatusCliOptions() {
        Option help = new Option("h", "help", false, "print this message");

        Option rexsterFile = OptionBuilder.withArgName("host-name")
                .hasArg()
                .withDescription("rexster web server hostname or ip address (default is 127.0.0.1)")
                .withLongOpt("rexsterhost")
                .create("rh");

        Option webServerPort = OptionBuilder.withArgName("port")
                .hasArg()
                .withDescription("rexster web server status port (default is 8183)")
                .withLongOpt("rexsterport")
                .create("rp");

        Options options = new Options();
        options.addOption(help);
        options.addOption(rexsterFile);
        options.addOption(webServerPort);

        return options;
    }

    private static RexsterCommandLine getCliInput(final String[] args) throws Exception {
        Options options = getCliOptions();
        Options innerOptions = null;
        GnuParser parser = new GnuParser();
        CommandLine line = null;
        CommandLine innerLine = null;
        String commandText = "";

        try {
            // not sure why the stopAtNonOption should be set to true for the parse methods.
            // would seem like the opposite setting makes more sense.
            line = parser.parse(options, args, true);

            if (line.hasOption("start")) {
                commandText = "start";
                innerOptions = getStartCliOptions();
                String[] optionValues = line.getOptionValues("start");

                if (optionValues != null && optionValues.length > 0) {
                    innerLine = parser.parse(innerOptions, optionValues, true);
                }
            } else if (line.hasOption("stop")) {
                commandText = "stop";
                innerOptions = getStopCliOptions();
                String[] optionValues = line.getOptionValues("stop");

                if (optionValues != null && optionValues.length > 0) {
                    innerLine = parser.parse(innerOptions, optionValues, true);
                }
            } else if (line.hasOption("status")) {
                commandText = "status";
                innerOptions = getStatusCliOptions();
                String[] optionValues = line.getOptionValues("status");

                if (optionValues != null && optionValues.length > 0) {
                    innerLine = parser.parse(innerOptions, optionValues, true);
                }
            }

        } catch (ParseException exp) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("rexster", options);
            System.exit(0);
        }

        if (line.hasOption("help") && (line.hasOption("start") || line.hasOption("status") || line.hasOption("stop"))) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("rexster - " + commandText, innerOptions);
            System.exit(0);
        } else if (line.hasOption("help")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("rexster", options);
            System.exit(0);
        }

        return new RexsterCommandLine(line, innerLine, options);
    }

    private static String[] cleanArguments(String[] arguments) {

        // this method is a bit of a hack to get around the global argument of
        // -webroot which only applies to the -start command.  it pulls out -webroot
        // of the argument list so the parser doesn't fail...kinda gross
        ArrayList<String> cleanedArguments = new ArrayList<String>();

        if (arguments != null && arguments.length > 0) {

            if (!arguments[0].equals("-start") && !arguments[0].equals("--start") && !arguments[0].equals("-s")) {
                for (int ix = 0; ix < arguments.length; ix++) {
                    if (!arguments[ix].equals("-webroot")) {
                        cleanedArguments.add(arguments[ix]);
                    } else {
                        ix++;
                    }
                }
            } else {
                for (String argument : arguments) {
                    cleanedArguments.add(argument);
                }
            }
        }

        String[] cleanedArgumentsAsArray = new String[cleanedArguments.size()];
        cleanedArguments.toArray(cleanedArgumentsAsArray);
        return cleanedArgumentsAsArray;
    }

    public static void main(final String[] args) throws Exception {

        XMLConfiguration properties = new XMLConfiguration();

        RexsterCommandLine line = getCliInput(cleanArguments(args));

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

            String rexsterXmlFile = "rexster.xml";

            if (line.hasCommandParameters() && line.getCommandParameters().hasOption("configuration")) {

                rexsterXmlFile = line.getCommandParameters().getOptionValue("configuration");

                try {
                    properties.load(new FileReader(rexsterXmlFile));
                } catch (IOException e) {
                    throw new Exception("Could not locate " + rexsterXmlFile + " properties file.");
                }
            } else {
                // no arguments to parse...check the default rexster.xml in the root of the working directory
                try {
                    properties.load(new FileReader(rexsterXmlFile));
                } catch (IOException e) {
                    properties.load(RexsterApplication.class.getResourceAsStream(rexsterXmlFile));
                }
            }

            // reference the location of the xml file used to configure the server.
            // this will allow the configuration to be passed into components that
            // do not have access to the configuration file and need it for graph
            // initialization preventing it from having to be explicitly defined
            // in rexster.xml itself.  there's probably an even better way to do
            // this *sigh*
            properties.addProperty("self-xml", rexsterXmlFile);

            // overrides rexster-server-port from command line
            if (line.hasCommandParameters() && line.getCommandParameters().hasOption("rexsterport")) {
                properties.setProperty("rexster-server-port", line.getCommandParameters().getOptionValue("rexsterport"));
            }

            // overrides web-root from command line
            if (line.hasCommandParameters() && line.getCommandParameters().hasOption("webroot")) {
                properties.setProperty("web-root", line.getCommandParameters().getOptionValue("webroot"));
            }

            try {
                new WebServer(properties, true);
            } catch (BindException be) {
                logger.error("Could not start Rexster Server.  A port that Rexster needs is in use.");
            }
        } else if (line.getCommand().hasOption("version")) {
            System.out.println("Rexster version [" + RexsterApplication.getVersion() + "]");
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