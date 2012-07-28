package com.tinkerpop.rexster.server;


import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

/**
 * Holds command line options and parameters.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class RexsterCommandLine {
    private CommandLine command;

    private CommandLine commandParameters;

    private Options commandOptions;

    public RexsterCommandLine(CommandLine command, CommandLine commandParameters, Options commandOptions) {
        this.command = command;
        this.commandParameters = commandParameters;
        this.commandOptions = commandOptions;
    }

    public Options getCommandOptions() {
        return commandOptions;
    }

    public CommandLine getCommand() {
        return command;
    }

    public CommandLine getCommandParameters() {
        return commandParameters;
    }

    public String getCommandOption(final String opt, final String defaultValue) {
        return hasCommandParameters() ? commandParameters.getOptionValue(opt, defaultValue) : defaultValue;
    }

    public boolean hasCommandOption(final String opt) {
        return hasCommandParameters() && commandParameters.hasOption(opt);
    }

    public boolean hasCommandParameters() {
        return this.commandParameters != null;
    }
}
