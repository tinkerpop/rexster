package com.tinkerpop.rexster;


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

    public boolean hasCommandParameters() {
        return this.commandParameters != null;
    }
}
