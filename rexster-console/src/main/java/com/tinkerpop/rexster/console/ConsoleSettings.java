package com.tinkerpop.rexster.console;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * Settings from command line to pass to RexsterConsole instance.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class ConsoleSettings {

    private final String DEFAULT_HOST = "127.0.0.1";
    private final int DEFAULT_PORT = 8184;
    private final String DEFAULT_LANGUAGE = "groovy";
    private final int DEFAULT_TIMEOUT = 100;

    private final String host;
    private final int port;
    private final int timeout;
    private final String username;
    private final String password;
    private final String fileToExecute;
    private String language;

    public ConsoleSettings(final String [] commandLineArgs) throws Exception {
        final CommandLine line = getCliInput(commandLineArgs);
        this.host = line.getOptionValue("rexsterhost", DEFAULT_HOST);

        final String portString = line.getOptionValue("rexsterport");
        this.port = parseInt(portString, DEFAULT_PORT);

        if (line.hasOption("rexsterport") && !Integer.toString(this.port).equals(portString)) {
            System.out.println("the rexsterport parameter must be an integer value. Defaulting to: [" + port + "]");
        }

        this.language = line.getOptionValue("language", DEFAULT_LANGUAGE);

        final String timeoutString = line.getOptionValue("timeout");
        this.timeout = parseInt(timeoutString, DEFAULT_TIMEOUT);

        if (line.hasOption("timeout") && !Integer.toString(this.timeout).equals(timeoutString)){
            System.out.println("the timeout parameter must be an integer value. Defaulting to: " + timeout);
        }

        this.username = line.getOptionValue("user", "");
        this.password = line.getOptionValue("pass", "");
        this.fileToExecute = line.getOptionValue("execute", null);
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getUsername() {
        return username;
    }

    public int getTimeout() {
        return timeout;
    }

    public String getPassword() {
        return password;
    }

    public String getFileToExecute() {
        return fileToExecute;
    }

    public boolean isExecuteMode() {
        return fileToExecute != null;
    }

    public String getHostPort() {
        return "[" + this.host + ":" + this.port + "]";
    }

    private int parseInt(final String intString, final int intDefault) {
        try {
            return Integer.parseInt(intString);
        } catch (NumberFormatException nfe) {
            return intDefault;
        }
    }

    @SuppressWarnings("static-access")
    private static Options getCliOptions() {
        final Option help = new Option("h", "help", false, "print this message");

        final Option hostName = OptionBuilder.withArgName("host-name")
                .hasArg()
                .withDescription("the rexster server to connect to")
                .withLongOpt("rexsterhost")
                .create("rh");

        final Option port = OptionBuilder.withArgName("port")
                .hasArg()
                .withDescription("the port of the rexster server that is serving rexpro")
                .withLongOpt("rexsterport")
                .create("rp");

        final Option language = OptionBuilder.withArgName("language")
                .hasArg()
                .withDescription("the script engine language to use by default")
                .withLongOpt("language")
                .create("l");

        final Option timeout = OptionBuilder.withArgName("seconds")
                .hasArg()
                .withDescription("time allowed when waiting for results from server (default 100 seconds)")
                .withLongOpt("timeout")
                .create("t");

        final Option scriptFile = OptionBuilder.withArgName("file")
                .hasArg()
                .withDescription("script to execute remotely")
                .withLongOpt("execute")
                .create("e");

        final Option username = OptionBuilder.withArgName("username")
                .hasArg()
                .withDescription("username for authentication (if needed)")
                .withLongOpt("user")
                .create("u");

        final Option password = OptionBuilder.withArgName("password")
                .hasArg()
                .withDescription("password for authentication (if needed)")
                .withLongOpt("pass")
                .create("p");

        final Options options = new Options();
        options.addOption(help);
        options.addOption(hostName);
        options.addOption(port);
        options.addOption(language);
        options.addOption(timeout);
        options.addOption(scriptFile);
        options.addOption(username);
        options.addOption(password);

        return options;
    }

    private static CommandLine getCliInput(final String[] args) throws Exception {
        final Options options = getCliOptions();
        final CommandLineParser parser = new GnuParser();
        final CommandLine line;

        try {
            line = parser.parse(options, args);
        } catch (ParseException exp) {
            throw new Exception("Parsing failed.  Reason: " + exp.getMessage());
        }

        if (line.hasOption("help")) {
            final HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("rexster console", options);
            System.exit(0);
        }

        return line;
    }
}
