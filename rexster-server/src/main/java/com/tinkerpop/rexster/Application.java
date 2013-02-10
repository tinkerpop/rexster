package com.tinkerpop.rexster;

import com.tinkerpop.rexster.protocol.EngineController;
import com.tinkerpop.rexster.server.HttpRexsterServer;
import com.tinkerpop.rexster.server.RexProRexsterServer;
import com.tinkerpop.rexster.server.RexsterApplication;
import com.tinkerpop.rexster.server.RexsterCommandLine;
import com.tinkerpop.rexster.server.RexsterServer;
import com.tinkerpop.rexster.server.RexsterSettings;
import com.tinkerpop.rexster.server.ShutdownManager;
import com.tinkerpop.rexster.server.XmlRexsterApplication;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.io.IOUtils;
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
import java.net.URL;
import java.util.HashSet;
import java.util.List;

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
    private final XMLConfiguration properties;

    public Application(final XMLConfiguration properties, final boolean isDebug) throws Exception {
        // get the graph configurations from the XML config file
        this.properties = properties;
        this.properties.addProperty("debug", isDebug);
        final List<HierarchicalConfiguration> graphConfigs = properties.configurationsAt(Tokens.REXSTER_GRAPH_PATH);
        this.rexsterApplication = new XmlRexsterApplication(graphConfigs);

        this.httpServer = new HttpRexsterServer(properties);
        this.rexproServer = new RexProRexsterServer(properties);
    }

    public void start() throws Exception {

        // the EngineController needs to be configured statically before requests start serving so that it can
        // properly construct ScriptEngine objects with the correct reset policy.
        final int scriptEngineThreshold = this.properties.getInt("script-engine-reset-threshold", EngineController.RESET_NEVER);
        final String scriptEngineInitFile = this.properties.getString("script-engine-init", "");

        // allow scriptengines to be configured so that folks can drop in different gremlin flavors.
        final List configuredScriptEngineNames = this.properties.getList("script-engines");
        if (configuredScriptEngineNames == null) {
            // configure to default with gremlin-groovy
            logger.info("No configuration for <script-engines>.  Using gremlin-groovy by default.");
            EngineController.configure(scriptEngineThreshold, scriptEngineInitFile);
        } else {
            EngineController.configure(scriptEngineThreshold, scriptEngineInitFile, new HashSet<String>(configuredScriptEngineNames));
        }


        logger.info(String.format(
                "Gremlin ScriptEngine configured to reset every [%s] requests. Set to -1 to never reset.",
                scriptEngineThreshold));

        this.httpServer.start(this.rexsterApplication);
        this.rexproServer.start(this.rexsterApplication);
        startShutdownManager(this.properties);
    }

    public void stop() {
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

    private void startShutdownManager(final XMLConfiguration properties) throws Exception {
        final Integer shutdownServerPort = properties.getInteger("rexster-shutdown-port",
                new Integer(RexsterSettings.DEFAULT_SHUTDOWN_PORT));
        final String shutdownServerHost = properties.getString("rexster-shutdown-host", RexsterSettings.DEFAULT_HOST);
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

    public static void main(final String[] args)  {

        logger.info(".:Welcome to Rexster:.");

        // properties from XML can be overriden by entries issued from the command line
        final RexsterSettings settings = new RexsterSettings(args);
        final RexsterCommandLine line = settings.getCommand();

        if (settings.getPrimeCommand().equals(RexsterSettings.COMMAND_START)) {
            try {
                new Application(settings.getProperties(), settings.isDebug()).start();
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
        } catch (SocketException se) {
            logger.debug(se);
        } catch (IOException ioe) {
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
