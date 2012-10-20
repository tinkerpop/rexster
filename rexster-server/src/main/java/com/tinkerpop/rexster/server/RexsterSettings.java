package com.tinkerpop.rexster.server;

import com.tinkerpop.rexster.Application;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.LogManager;

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class RexsterSettings {

    private static final Logger logger = Logger.getLogger(RexsterSettings.class);

    public static final int DEFAULT_HTTP_PORT = 8182;
    public static final int DEFAULT_SHUTDOWN_PORT = 8183;
    public static final int DEFAULT_REXPRO_PORT = 8184;
    public static final String DEFAULT_WEB_ROOT_PATH = "public";
    public static final String DEFAULT_BASE_URI = "http://localhost";
    public static final long DEFAULT_REXPRO_SESSION_MAX_IDLE = 1790000;
    public static final long DEFAULT_REXPRO_SESSION_CHECK_INTERVAL = 3000000;
    public static final String DEFAULT_HOST = "127.0.0.1";

    public static final String COMMAND_START = "start";
    public static final String COMMAND_VERSION = "version";
    public static final String COMMAND_STOP = "stop";
    public static final String COMMAND_STATUS = "status";
    public static final String COMMAND_HELP = "help";

    private final RexsterCommandLine line;

    public RexsterSettings(final String[] arguments) {
        this.line = getCliInput(cleanArguments(arguments));
    }

    public RexsterCommandLine getCommand(){
        return this.line;
    }

    public String getPrimeCommand() {
        if (this.line.getCommand().hasOption(COMMAND_START)) {
            return COMMAND_START;
        } else if (this.line.getCommand().hasOption(COMMAND_VERSION)) {
            return COMMAND_VERSION;
        } else if (this.line.getCommand().hasOption(COMMAND_STOP)) {
            return COMMAND_STOP;
        } else if (this.line.getCommand().hasOption(COMMAND_STATUS)) {
            return COMMAND_STATUS;
        } else {
            return COMMAND_HELP;
        }
    }

    public void printHelp() {
        final HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("rexster", line.getCommandOptions());
    }

    public XMLConfiguration getProperties() {

        final XMLConfiguration properties = new XMLConfiguration();

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
                    properties.load(Application.class.getResourceAsStream(rexsterXmlFileLocation));
                    logger.info("Using [" + rexsterXmlFileLocation + "] resource as configuration source.");
                } catch (Exception ex){
                    logger.fatal("None of the default rexster.xml can be found or read.");
                }
            }
        }

        // overrides rexster-server-port from command line
        if (line.hasCommandParameters() && line.getCommandParameters().hasOption("rexsterport")) {
            properties.setProperty("http.server-port", line.getCommandParameters().getOptionValue("rexsterport"));
        }

        // overrides web-root from command line
        if (line.hasCommandParameters() && line.getCommandParameters().hasOption("webroot")) {
            properties.setProperty("http.web-root", line.getCommandParameters().getOptionValue("webroot"));
        }

        return properties;
    }

    @SuppressWarnings("static-access")
    private static Options getCliOptions() {
        final Option help = new Option("h", "help", false, "print this message");

        final Option rexsterStart = OptionBuilder.withArgName("parameters")
                .hasOptionalArgs()
                .withDescription("start rexster (learn more with start -h)")
                .withLongOpt("start")
                .create("s");

        final Option rexsterStop = OptionBuilder.withArgName("parameters")
                .hasOptionalArgs()
                .withDescription("stop rexster (learn more with stop -h)")
                .withLongOpt("stop")
                .create("x");

        final Option rexsterStatus = OptionBuilder.withArgName("parameters")
                .hasOptionalArgs()
                .withDescription("status of rexster (learn more with status -h)")
                .withLongOpt("status")
                .create("u");

        final Option rexsterVersion = new Option("v", "version", false, "print the version of rexster server");

        final Options options = new Options();
        options.addOption(rexsterStart);
        options.addOption(rexsterStop);
        options.addOption(rexsterStatus);
        options.addOption(rexsterVersion);
        options.addOption(help);

        return options;
    }

    @SuppressWarnings("static-access")
    private static Options getStartCliOptions() {
        final Option help = new Option("h", "help", false, "print this message");

        final Option rexsterFile = OptionBuilder.withArgName("file")
                .hasArg()
                .withDescription("use given file for rexster.xml")
                .withLongOpt("configuration")
                .create("c");

        final Option webServerPort = OptionBuilder.withArgName("port")
                .hasArg()
                .withDescription("override port used for rexster-server-port in rexster.xml")
                .withLongOpt("rexsterport")
                .create("rp");

        final Option webRoot = OptionBuilder.withArgName("path")
                .hasArg()
                .withDescription("override web-root in rexster.xml")
                .withLongOpt("webroot")
                .create("wr");

        final Option debug = new Option("d", "debug", false, "run rexster with full console logging output from jersey");

        final Options options = new Options();
        options.addOption(help);
        options.addOption(rexsterFile);
        options.addOption(webServerPort);
        options.addOption(webRoot);
        options.addOption(debug);

        return options;
    }

    @SuppressWarnings("static-access")
    private static Options getStopCliOptions() {
        final Option help = new Option("h", "help", false, "print this message");

        final Option rexsterFile = OptionBuilder.withArgName("host-name")
                .hasArg()
                .withDescription("rexster web server hostname or ip address (default is 127.0.0.1)")
                .withLongOpt("rexsterhost")
                .create("rh");

        final Option webServerPort = OptionBuilder.withArgName("port")
                .hasArg()
                .withDescription("rexster web server shutdown port (default is 8183)")
                .withLongOpt("rexsterport")
                .create("rp");

        final Option stopAndWait = new Option("w", "wait", false, "wait for server confirmation of shutdown");

        final Options options = new Options();
        options.addOption(help);
        options.addOption(rexsterFile);
        options.addOption(webServerPort);
        options.addOption(stopAndWait);

        return options;
    }

    @SuppressWarnings("static-access")
    private static Options getStatusCliOptions() {
        final Option help = new Option("h", "help", false, "print this message");

        final Option rexsterFile = OptionBuilder.withArgName("host-name")
                .hasArg()
                .withDescription("rexster web server hostname or ip address (default is 127.0.0.1)")
                .withLongOpt("rexsterhost")
                .create("rh");

        final Option webServerPort = OptionBuilder.withArgName("port")
                .hasArg()
                .withDescription("rexster web server status port (default is 8183)")
                .withLongOpt("rexsterport")
                .create("rp");

        final Options options = new Options();
        options.addOption(help);
        options.addOption(rexsterFile);
        options.addOption(webServerPort);

        return options;
    }

    private static RexsterCommandLine getCliInput(final String[] args) {
        final Options options = getCliOptions();
        final GnuParser parser = new GnuParser();
        Options innerOptions = null;
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
                final String[] optionValues = line.getOptionValues("start");

                if (optionValues != null && optionValues.length > 0) {
                    innerLine = parser.parse(innerOptions, optionValues, true);
                }
            } else if (line.hasOption("stop")) {
                commandText = "stop";
                innerOptions = getStopCliOptions();
                final String[] optionValues = line.getOptionValues("stop");

                if (optionValues != null && optionValues.length > 0) {
                    innerLine = parser.parse(innerOptions, optionValues, true);
                }
            } else if (line.hasOption("status")) {
                commandText = "status";
                innerOptions = getStatusCliOptions();
                final String[] optionValues = line.getOptionValues("status");

                if (optionValues != null && optionValues.length > 0) {
                    innerLine = parser.parse(innerOptions, optionValues, true);
                }
            }

        } catch (ParseException exp) {
            final HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("rexster", options);
            System.exit(0);
        }

        if (line.hasOption("help") && (line.hasOption("start") || line.hasOption("status") || line.hasOption("stop"))) {
            final HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("rexster - " + commandText, innerOptions);
            System.exit(0);
        } else if (line.hasOption("help")) {
            final HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("rexster", options);
            System.exit(0);
        }

        return new RexsterCommandLine(line, innerLine, options);
    }

    private static String[] cleanArguments(final String[] arguments) {

        // this method is a bit of a hack to get around the global argument of
        // -webroot which only applies to the -start command.  it pulls out -webroot
        // of the argument list so the parser doesn't fail...kinda gross
        final ArrayList<String> cleanedArguments = new ArrayList<String>();

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

        final String[] cleanedArgumentsAsArray = new String[cleanedArguments.size()];
        cleanedArguments.toArray(cleanedArgumentsAsArray);
        return cleanedArgumentsAsArray;
    }
}
