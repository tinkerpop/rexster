package com.tinkerpop.rexster;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.Socket;

import org.apache.commons.cli.*;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

/**
 * Shutdowns command for the Rexster web server.
 *
 * Adapted from http://code.google.com/p/shutdown-listener/
 */
public class Shutdown {

    protected static Logger logger = Logger.getLogger(Shutdown.class);

    @SuppressWarnings("static-access")
    private static Options getCliOptions() {
    	Option help = new Option( "help", "print this message" );

		Option rexsterFile  = OptionBuilder.withArgName("host")
									       .hasArg()
									       .withDescription("rexster web server hostname or ip address (default is 127.0.0.1)")
									       .create("host");

		Option webServerPort  = OptionBuilder.withArgName("port")
										     .hasArg()
										     .withDescription("rexster web server shutdown port (default is 8184)")
										     .create("port");

		Option serverCommand  = OptionBuilder.withArgName("command")
										       .hasArg()
										       .withDescription("command to issue to rexster web server (-s for shutdown)")
										       .create("cmd");

		Options options = new Options();
		options.addOption(help);
		options.addOption(rexsterFile);
		options.addOption(webServerPort);
		options.addOption(serverCommand);

		return options;
    }

	private static CommandLine getCliInput(final String[] args) throws Exception {
		Options options = getCliOptions();
		CommandLineParser parser = new GnuParser();
		CommandLine line = null;

		try {
		    line = parser.parse(options, args);
		}
		catch(ParseException exp) {
			throw new Exception("Parsing failed.  Reason: " + exp.getMessage());
		}

		if (line.hasOption("help")) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("rexster", options);
			System.exit(0);
		}

		return line;
	}

    public static void main(String[] args) throws Exception {

        CommandLine cmdLine = getCliInput(args);

        // default command line values
        String command = ShutdownManager.COMMAND_STATUS;
        String host = "127.0.0.1";
        int port = 8184;

        if (cmdLine.hasOption("host")) {
            host = cmdLine.getOptionValue("host");
        }

        if (cmdLine.hasOption("port")) {
            String portString = cmdLine.getOptionValue("port");

            try {
                port = Integer.parseInt(portString);
            } catch (NumberFormatException nfe) {
                logger.warn("The value of the <port> parameter was not a valid value.  Utilizing the default port of " + port + ".");
            }
        }

        if (cmdLine.hasOption("cmd")) {
            command = cmdLine.getOptionValue("cmd");
        }

        final InetAddress hostAddress = InetAddress.getByName(host);
        final Socket shutdownConnection = new Socket(hostAddress, port);
        try {
            shutdownConnection.setSoTimeout(5000);
            final BufferedReader reader = new BufferedReader(new InputStreamReader(shutdownConnection.getInputStream()));
            final PrintStream writer = new PrintStream(shutdownConnection.getOutputStream());
            try {
                writer.println(command);
                writer.flush();

                while (true) {
                    final String line = reader.readLine();
                    if (line == null) {
                        break;
                    }

                    logger.info(line);
                }
            }
            finally {
                IOUtils.closeQuietly(reader);
                IOUtils.closeQuietly(writer);
            }
        }
        finally {
            try {
                shutdownConnection.close();
            }
            catch (IOException ioe) {
            }
        }
    }
}
