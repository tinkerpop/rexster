package com.tinkerpop.rexster.protocol;

import com.tinkerpop.pipes.util.iterators.SingleIterator;
import com.tinkerpop.rexster.Tokens;
import com.tinkerpop.rexster.protocol.message.ConsoleScriptResponseMessage;
import com.tinkerpop.rexster.protocol.message.ErrorResponseMessage;
import com.tinkerpop.rexster.protocol.message.RexProMessage;
import com.tinkerpop.rexster.protocol.message.ScriptRequestMessage;
import jline.ConsoleReader;
import jline.History;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class RexsterConsole {

    private RemoteRexsterSession session = null;
    private String host;
    private String language;
    private String username = "";
    private String password = "";
    private int port;
    private int timeout;
    private List<String> currentBindings = new ArrayList<String>();

    private final PrintStream output = System.out;

    private static final String REXSTER_HISTORY = ".rexster_history";

    public RexsterConsole(String host, int port, String language, int timeout, String username, String password) throws Exception {

        this.output.println("        (l_(l");
        this.output.println("(_______( 0 0");
        this.output.println("(        (-Y-) <woof>");
        this.output.println("l l-----l l");
        this.output.println("l l,,   l l,,");

        this.host = host;
        this.port = port;
        this.language = language;
        this.timeout = timeout;
        this.username = username;
        this.password = password;

        this.output.println("opening session [" + this.host + ":" + this.port + "]");
        this.session = new RemoteRexsterSession(this.host, this.port, this.timeout, this.username, this.password);
        this.session.open();

        if (this.session.isOpen()) {
            this.output.println("?h for help");

            this.primaryLoop();
        } else {
            this.output.println("could not connect to the Rexster server");
        }

    }

    public RexsterConsole(String host, int port, String language, int timeout, String script, String username, String password) throws Exception {
        this.host = host;
        this.port = port;
        this.language = language;
        this.timeout = timeout;
        this.username = username;
        this.password = password;

        this.session = new RemoteRexsterSession(this.host, this.port, this.timeout, this.username, this.password);
        this.session.open();

        if (!this.session.isOpen()) {
            this.output.println("could not connect to the Rexster server");
        } else {
            this.executeScript(script, false);
        }
    }

    public void primaryLoop() throws Exception {

        final ConsoleReader reader = new ConsoleReader();
        reader.setBellEnabled(false);
        reader.setUseHistory(true);

        try {
            History history = new History();
            history.setHistoryFile(new File(REXSTER_HISTORY));
            reader.setHistory(history);
        } catch (IOException e) {
            System.err.println("Could not find history file");
        }

        String line = "";
        this.output.println();

        while (line != null) {

            try {
                line = "";
                boolean submit = false;
                boolean newline = false;
                while (!submit) {
                    if (newline)
                        line = line + "\n" + reader.readLine(RexsterConsole.makeSpace(this.getPrompt().length()));
                    else
                        line = line + "\n" + reader.readLine(this.getPrompt());
                    if (line.endsWith(" .")) {
                        newline = true;
                        line = line.substring(0, line.length() - 2);
                    } else {
                        line = line.trim();
                        submit = true;
                    }
                }

                if (line.isEmpty())
                    continue;
                if (line.equals(Tokens.REXSTER_CONSOLE_QUIT)) {
                    this.output.print("closing session with Rexster [" + this.host + ":" + this.port + "]");
                    if (this.session != null) {
                        this.session.close();
                        this.session = null;
                    }
                    this.output.println("--> done");
                    return;
                } else if (line.equals(Tokens.REXSTER_CONSOLE_HELP)) {
                    this.printHelp();
                } else if (line.equals(Tokens.REXSTER_CONSOLE_BINDINGS)) {
                    this.printBindings();
                } else if (line.equals(Tokens.REXSTER_CONSOLE_RESET)) {
                    this.output.print("resetting session with Rexster [" + this.host + ":" + this.port + "]");
                    if (this.session != null) {
                        this.session.reset();
                    } else {
                        this.session = new RemoteRexsterSession(this.host, this.port, this.timeout, this.username, this.password);
                    }
                    this.output.println("--> done");
                } else if (line.startsWith(Tokens.REXSTER_CONSOLE_EXECUTE)) {
                    String fileToExecute = line.substring(Tokens.REXSTER_CONSOLE_EXECUTE.length()).trim();
                    if (fileToExecute == null || fileToExecute.isEmpty()) {
                        this.output.print("specify the file to execute");
                    } else {
                        try {
                            this.executeScript(readFile(fileToExecute));
                        } catch (IOException ioe) {
                            this.output.println("could not read the file specified");
                        }
                    }
                } else if (line.equals(Tokens.REXSTER_CONSOLE_LANGUAGES)) {
                    this.printAvailableLanguages();
                } else if (line.startsWith(Tokens.REXSTER_CONSOLE_LANGUAGE)) {
                    String langToChangeTo = line.substring(1);
                    if (langToChangeTo == null || langToChangeTo.isEmpty()) {
                        this.output.println("specify a language on Rexster ?<language-name>");
                        this.printAvailableLanguages();
                    } else if (this.session.isAvailableLanguage(langToChangeTo)) {
                        this.language = langToChangeTo;
                    } else {
                        this.output.println("not a valid language on Rexster: [" + langToChangeTo + "].");
                        this.printAvailableLanguages();
                    }
                } else {
                    executeScript(line);
                }

            } catch (Exception e) {
                this.output.println("Evaluation error: " + e.getMessage());
            }
        }
    }

    private void executeScript(String line) {
        executeScript(line, true);
    }

    private void executeScript(String line, boolean showPrefix) {
        ResultAndBindings result = eval(line, this.language, this.session);
        Iterator itty;
        if (result.getResult() instanceof Iterator) {
            itty = (Iterator) result.getResult();
        } else if (result.getResult() instanceof Iterable) {
            itty = ((Iterable) result.getResult()).iterator();
        } else if (result.getResult() instanceof Map) {
            itty = ((Map) result.getResult()).entrySet().iterator();
        } else {
            itty = new SingleIterator<Object>(result.getResult());
        }

        while (itty.hasNext()) {
            if (showPrefix) {
                this.output.println("==>" + itty.next());
            } else {
                this.output.println(itty.next());
            }
        }

        this.currentBindings = result.getBindings();
    }

    private void printAvailableLanguages() {
        this.output.println("-= Available Languages =-");

        Iterator<String> languages = this.session.getAvailableLanguages();
        while (languages.hasNext()) {
            this.output.println("?" + languages.next());
        }
    }

    public void printHelp() {
        this.output.println("-= Console Specific =-");
        this.output.println("?<language-name>: jump to engine");
        this.output.println(Tokens.REXSTER_CONSOLE_LANGUAGES + ": list of available languages on Rexster");
        this.output.println(Tokens.REXSTER_CONSOLE_RESET + ": reset the rexster session");
        this.output.println(Tokens.REXSTER_CONSOLE_EXECUTE + " <file-name>: execute a script file");
        this.output.println(Tokens.REXSTER_CONSOLE_QUIT + ": quit");
        this.output.println(Tokens.REXSTER_CONSOLE_HELP + ": displays this message");

        this.output.println("");
        this.output.println("-= Rexster Context =-");
        this.output.println("rexster.getGraph(graphName) - gets a Graph instance");
        this.output.println("   :graphName - [String] - the name of a graph configured within Rexster");
        this.output.println("rexster.getGraphNames() - gets the set of graph names configured within Rexster");
        this.output.println("rexster.getVersion() - gets the version of Rexster server");
        this.output.println("");
    }

    public void printBindings() {
        for (String binding : this.currentBindings) {
            this.output.println("==>" + binding);
        }
    }

    public String getPrompt() {
        return "rexster[" + this.language + "]> ";
    }

    public static String makeSpace(int number) {
        String space = new String();
        for (int i = 0; i < number; i++) {
            space = space + " ";
        }
        return space;
    }

    private static ResultAndBindings eval(String script, String scriptEngineName, RemoteRexsterSession session) {

        ResultAndBindings returnValue = null;

        try {
            session.open();

            // pass in some dummy rexster bindings...not really fully working quite right for scriptengine usage
            final RexProMessage scriptMessage = new ScriptRequestMessage(
                    session.getSessionKey(), scriptEngineName, new RexsterBindings(), script);

            final RexProMessage resultMessage = session.sendRequest(scriptMessage, 3, 500);

            ArrayList<String> lines = new ArrayList<String>();
            List<String> bindings = new ArrayList<String>();
            try {
                ConsoleScriptResponseMessage responseMessage = new ConsoleScriptResponseMessage(resultMessage);

                bindings = responseMessage.getBindings();

                ByteBuffer bb = ByteBuffer.wrap(responseMessage.getBody());

                // navigate to the start of the results...bindings are attached if there is no error present
                int lengthOfBindings = bb.getInt();
                bb.position(lengthOfBindings + 4);


                while (bb.hasRemaining()) {
                    int segmentLength = bb.getInt();
                    byte[] resultObjectBytes = new byte[segmentLength];
                    bb.get(resultObjectBytes);

                    lines.add(new String(resultObjectBytes));
                }

            } catch (IllegalArgumentException iae) {
                ErrorResponseMessage errorMessage = new ErrorResponseMessage(resultMessage);
                lines.add(errorMessage.getErrorMessage());
            }

            Object result = lines.iterator();

            if (lines.size() == 1) {
                result = lines.get(0);
            }

            returnValue = new ResultAndBindings(result, bindings);

        } catch (Exception e) {
            System.out.println("The session with Rexster Server may have been lost.  Please try again or refresh your session with ?r");
        } finally {
        }

        return returnValue;
    }

    private static String readFile(String file) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line = null;
        StringBuilder stringBuilder = new StringBuilder();
        String ls = System.getProperty("line.separator");
        while ((line = reader.readLine()) != null) {
            stringBuilder.append(line);
            stringBuilder.append(ls);
        }
        return stringBuilder.toString();
    }

    @SuppressWarnings("static-access")
    private static Options getCliOptions() {
        Option help = new Option("h", "help", false, "print this message");

        Option hostName = OptionBuilder.withArgName("host-name")
                .hasArg()
                .withDescription("the rexster server to connect to")
                .withLongOpt("rexsterhost")
                .create("rh");

        Option port = OptionBuilder.withArgName("port")
                .hasArg()
                .withDescription("the port of the rexster server that is serving rexpro")
                .withLongOpt("rexsterport")
                .create("rp");

        Option language = OptionBuilder.withArgName("language")
                .hasArg()
                .withDescription("the script engine language to use by default")
                .withLongOpt("language")
                .create("l");

        Option timeout = OptionBuilder.withArgName("seconds")
                .hasArg()
                .withDescription("time allowed when waiting for results from server (default 100 seconds)")
                .withLongOpt("timeout")
                .create("t");

        Option scriptFile = OptionBuilder.withArgName("file")
                .hasArg()
                .withDescription("script to execute remotely")
                .withLongOpt("execute")
                .create("e");

        Option username = OptionBuilder.withArgName("username")
                .hasArg()
                .withDescription("username for authentication (if needed)")
                .withLongOpt("user")
                .create("u");

        Option password = OptionBuilder.withArgName("password")
                .hasArg()
                .withDescription("password for authentication (if needed)")
                .withLongOpt("pass")
                .create("p");

        Options options = new Options();
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
        Options options = getCliOptions();
        CommandLineParser parser = new GnuParser();
        CommandLine line;

        try {
            line = parser.parse(options, args);
        } catch (ParseException exp) {
            throw new Exception("Parsing failed.  Reason: " + exp.getMessage());
        }

        if (line.hasOption("help")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("rexster console", options);
            System.exit(0);
        }

        return line;
    }

    public static void main(String[] args) throws Exception {

        CommandLine line = getCliInput(args);

        String host = "localhost";
        int port = 8184;
        String language = "groovy";
        int timeout = RexPro.DEFAULT_TIMEOUT_SECONDS;
        String username = "";
        String password = "";

        if (line.hasOption("rexsterhost")) {
            host = line.getOptionValue("rexsterhost");
        }

        if (line.hasOption("rexsterport")) {
            String portString = line.getOptionValue("rexsterport");
            try {
                port = Integer.parseInt(portString);
            } catch (NumberFormatException nfe) {
                System.out.println("the rexsterport parameter must be an integer value. Defaulting to: [" + port + "]");
            }
        }

        if (line.hasOption("language")) {
            language = line.getOptionValue("language");
        }

        if (line.hasOption("timeout")) {
            String timeoutString = line.getOptionValue("timeout");
            try {
                port = Integer.parseInt(timeoutString);
            } catch (NumberFormatException nfe) {
                System.out.println("the timeout parameter must be an integer value. Defaulting to: " + timeout);
            }
        }

        if (line.hasOption("user")) {
            username = line.getOptionValue("user");
        }

        if (line.hasOption("pass")) {
            password = line.getOptionValue("pass");
        }

        String fileToExecute = null;
        if (line.hasOption("execute")) {
            fileToExecute = line.getOptionValue("execute");

            try {
                new RexsterConsole(host, port, language, timeout, readFile(fileToExecute), username, password);
            } catch (IOException ioe) {
                System.out.println("could not read the file specified");
            }

        } else {
            new RexsterConsole(host, port, language, timeout, username, password);
        }
    }
}
