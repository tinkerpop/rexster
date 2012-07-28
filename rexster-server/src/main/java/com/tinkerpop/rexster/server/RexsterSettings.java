package com.tinkerpop.rexster.server;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.util.ArrayList;

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class RexsterSettings {

    private final RexsterCommandLine line;

    public RexsterSettings(final String[] arguments) {
        this.line = getCliInput(cleanArguments(arguments));
    }

    public RexsterCommandLine getCommand(){
        return this.line;
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
