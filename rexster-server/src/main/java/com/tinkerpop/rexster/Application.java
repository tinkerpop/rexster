package com.tinkerpop.rexster;

import com.tinkerpop.rexster.protocol.EngineConfiguration;
import com.tinkerpop.rexster.protocol.EngineController;
import com.tinkerpop.rexster.server.HttpRexsterServer;
import com.tinkerpop.rexster.server.RexProRexsterServer;
import com.tinkerpop.rexster.server.RexsterApplication;
import com.tinkerpop.rexster.server.RexsterCommandLine;
import com.tinkerpop.rexster.server.RexsterProperties;
import com.tinkerpop.rexster.server.RexsterServer;
import com.tinkerpop.rexster.server.RexsterSettings;
import com.tinkerpop.rexster.server.ShutdownManager;
import com.tinkerpop.rexster.server.XmlRexsterApplication;
import com.tinkerpop.rexster.server.metrics.ReporterConfig;
import com.tinkerpop.rexster.util.JuliToLog4jHandler;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.BindException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;

/**
 * Main class for initializing, starting and stopping Rexster.
 *
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class Application {

    private static final Logger logger = Logger.getLogger(Application.class);

    static {
        // try to load a log4j properties file in the root of REXSTER_HOME
        final File logConfigFile = new File("log4j.properties");
        URL logConfigFileUrl;
        try {
            if (logConfigFile.exists()) {
                // this one exists in the root of REXSTER_HOME
                logConfigFileUrl = logConfigFile.toURI().toURL();
            } else {
                // a custom one from a user doesn't exist so use the default one in the jar
                logConfigFileUrl = Application.class.getResource("log4j.properties");
            }
        } catch (MalformedURLException mue) {
            // revert to the properties file in the jar
            logConfigFileUrl = Application.class.getResource("log4j.properties");
        }

        PropertyConfigurator.configure(logConfigFileUrl);
    }

    private final RexsterServer httpServer;
    private final RexsterServer rexproServer;
    private final RexsterApplication rexsterApplication;
    private final RexsterProperties properties;
    private final ReporterConfig reporterConfig;

    /**
     * Check for configuration file changes every 10 seconds.
     */
    private final FileAlterationMonitor configurationMonitor = new FileAlterationMonitor(10000);

    public Application(final RexsterProperties properties,
                       final FileAlterationObserver rexsterConfigurationObserver) throws Exception {

        // watch rexster.xml for changes
        rexsterConfigurationObserver.addListener(properties);
        configurationMonitor.addObserver(rexsterConfigurationObserver);

        // get the graph configurations from the XML config file
        this.properties = properties;
        this.rexsterApplication = new XmlRexsterApplication(this.properties);

        this.reporterConfig = new ReporterConfig(properties, this.rexsterApplication.getMetricRegistry());
        this.httpServer = new HttpRexsterServer(properties);
        this.rexproServer = new RexProRexsterServer(properties, true);
    }

    public void start() throws Exception {
        configureScriptEngine();
        properties.addListener(new RexsterProperties.RexsterPropertiesListener() {
            @Override
            public void propertiesChanged(final XMLConfiguration configuration) {
                configureScriptEngine();
            }
        });

        this.httpServer.start(this.rexsterApplication);
        this.rexproServer.start(this.rexsterApplication);
        this.configurationMonitor.start();

        startShutdownManager(this.properties);
    }

    private void configureScriptEngine() {
        // the EngineController needs to be configured statically before requests start serving so that it can
        // properly construct ScriptEngine objects with the correct reset policy. allow scriptengines to be
        // configured so that folks can drop in different gremlin flavors.
        final List<EngineConfiguration> configuredScriptEngines = new ArrayList<EngineConfiguration>();
        final List<HierarchicalConfiguration> configs = this.properties.getScriptEngines();
        for(HierarchicalConfiguration config : configs) {
            configuredScriptEngines.add(new EngineConfiguration(config));
        }

        EngineController.configure(configuredScriptEngines);
    }

    public void stop() {

        try {
            this.configurationMonitor.stop();
        } catch (Exception ex) {
            logger.debug("Error shutting down the configuration monitor");
        }

        try {
            this.httpServer.stop();
        } catch (Exception ex) {
            logger.debug("Error shutting down Rexster Server ignored.", ex);
        }

        try {
            this.rexproServer.stop();
        } catch (Exception ex) {
            logger.debug("Error shutting down RexPro Server ignored.", ex);
        }

        try {
            this.rexsterApplication.stop();
        } catch (Exception ex) {
            logger.warn("Error while shutting down graphs.  All graphs may not have been shutdown cleanly.");
        }
    }

    private void startShutdownManager(final RexsterProperties properties) throws Exception {
        final ShutdownManager shutdownManager = new ShutdownManager(properties);

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

    public static void main(final String[] args)  {

        logger.info(".:Welcome to Rexster:.");

        // properties from XML can be overriden by entries issued from the command line
        final RexsterSettings settings = new RexsterSettings(args);
        initializeDebugLogging(settings);
        final RexsterCommandLine line = settings.getCommand();

        if (settings.getPrimeCommand().equals(RexsterSettings.COMMAND_START)) {
            try {
                new Application(settings.getProperties(), createRexsterConfigurationObserver(settings)).start();
            } catch (BindException be) {
                logger.fatal("Could not start Rexster Server.  A port that Rexster needs is in use.");
            } catch (Exception ex) {
                logger.fatal("The Rexster Server could not be started", ex);
            }
        } else if (settings.getPrimeCommand().equals(RexsterSettings.COMMAND_VERSION)) {
            logger.info(String.format("Rexster version [%s]", Tokens.REXSTER_VERSION));
        } else if (settings.getPrimeCommand().equals(RexsterSettings.COMMAND_STOP)) {
            if (line.hasCommandParameters() && line.getCommandParameters().hasOption("wait")) {
                issueControlCommand(line, ShutdownManager.COMMAND_SHUTDOWN_WAIT);
            } else {
                issueControlCommand(line, ShutdownManager.COMMAND_SHUTDOWN_NO_WAIT);
            }
        } else if (settings.getPrimeCommand().equals(RexsterSettings.COMMAND_STATUS)) {
            issueControlCommand(line, ShutdownManager.COMMAND_STATUS);
        } else {
            settings.printHelp();
        }
    }

    /**
     * Initialize debug level logging for rexster if the setting is turned on from the command line.
     */
    private static void initializeDebugLogging(final RexsterSettings settings) {
        if (settings.isDebug()) {
            // turn on all logging for jersey -- this is debug mode
            for (String l : Collections.list(LogManager.getLogManager().getLoggerNames())) {
                java.util.logging.Logger logger = java.util.logging.Logger.getLogger(l);
                logger.setLevel(Level.ALL);

                // remove old handlers
                for (Handler handler : logger.getHandlers()) {
                    logger.removeHandler(handler);
                }

                // route all logging from java.util.Logging to log4net
                final Handler handler = new JuliToLog4jHandler();
                handler.setLevel(Level.ALL);
                java.util.logging.Logger.getLogger(l).addHandler(handler);
            }
        } else {
            // turn off all logging for jersey
            for (String l : Collections.list(LogManager.getLogManager().getLoggerNames())) {
                java.util.logging.Logger.getLogger(l).setLevel(Level.OFF);
            }
        }
    }

    /**
     * Construct an observer for watching the rexster configuration file.
     */
    private static FileAlterationObserver createRexsterConfigurationObserver(final RexsterSettings settings) {
        final File rexsterConfigurationFile = settings.getRexsterXmlFile();
        final File absolute = new File(rexsterConfigurationFile.getAbsolutePath());
        final File rexsterConfigurationDirectory = absolute.getParentFile();

        logger.info(String.format("Rexster is watching [%s] for change.", rexsterConfigurationFile.getAbsolutePath()));

        // follow the rexster configuration file directory and only the rexster.xml equivalent within that directory.
        return new FileAlterationObserver(rexsterConfigurationDirectory,
                FileFilterUtils.and(FileFileFilter.FILE, FileFilterUtils.nameFileFilter(rexsterConfigurationFile.getName())));
    }

    private static int parseInt(final String intString, final int intDefault) {
        try {
            return Integer.parseInt(intString);
        } catch (NumberFormatException nfe) {
            return intDefault;
        }
    }

    private static void issueControlCommand(final RexsterCommandLine line, final String command) {

        final String host = line.getCommandOption("rexsterhost", RexsterSettings.DEFAULT_HOST);
        final String portString = line.getCommandOption("rexsterport", null);

        final int port = parseInt(portString, RexsterSettings.DEFAULT_SHUTDOWN_PORT);
        if (line.hasCommandOption("rexsterport") && !Integer.toString(port).equals(portString)) {
            logger.warn("The value of the <port> parameter was not a valid value.  Utilizing the default port of " + port + ".");
        }

        Socket shutdownConnection = null;
        try {
            final InetAddress hostAddress = InetAddress.getByName(host);
            shutdownConnection = new Socket(hostAddress, port);

            shutdownConnection.setSoTimeout(30000);
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
        } catch (SocketTimeoutException ste) {
            // perhaps the -wait option should take an argument for how long to wait for...for now 30 seconds seems
            // long enough.
            logger.warn("Taking longer than 30 seconds to shutdown Rexster.  Check shutdown status with --status");
        } catch (IOException ioe) {
            // SocketException or ConnectionException would be more exacting here, but don't think much can be done
            // with an IOException ... will keep it generic for now.
            logger.warn("Cannot connect to Rexster Server to issue command.  It may not be running.");
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
