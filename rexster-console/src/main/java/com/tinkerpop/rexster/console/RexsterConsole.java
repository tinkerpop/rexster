package com.tinkerpop.rexster.console;

import com.tinkerpop.pipes.util.iterators.SingleIterator;
import com.tinkerpop.rexster.Tokens;
import com.tinkerpop.rexster.protocol.RemoteRexsterSession;
import com.tinkerpop.rexster.protocol.ResultAndBindings;
import com.tinkerpop.rexster.protocol.RexsterBindings;
import com.tinkerpop.rexster.protocol.msg.ConsoleScriptResponseMessage;
import com.tinkerpop.rexster.protocol.msg.ErrorResponseMessage;
import com.tinkerpop.rexster.protocol.msg.MessageFlag;
import com.tinkerpop.rexster.protocol.msg.RexProMessage;
import com.tinkerpop.rexster.protocol.msg.ScriptRequestMessage;
import jline.ConsoleReader;
import jline.History;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class RexsterConsole {

    private RemoteRexsterSession session = null;
    private List<String> currentBindings = new ArrayList<String>();

    private final PrintStream output = System.out;
    private final ConsoleSettings settings;

    private static final String REXSTER_HISTORY = ".rexster_history";

    public RexsterConsole(final ConsoleSettings settings) {
        this.settings = settings;
    }

    public void start() throws Exception {
        if (this.settings.isExecuteMode()) {
            oneTimeExecuteScript(readFile(this.settings.getFileToExecute()));
        } else {
            this.writeAsciiArt();
            this.acceptReplCommands();
        }
    }

    private void writeAsciiArt() {
        this.output.println("        (l_(l");
        this.output.println("(_______( 0 0");
        this.output.println("(        (-Y-) <woof>");
        this.output.println("l l-----l l");
        this.output.println("l l,,   l l,,");
    }

    private void acceptReplCommands() throws Exception {
        this.output.println("opening session " + this.settings.getHostPort());
        this.initAndOpenSessionFromSettings();

        if (this.session.isOpen()) {
            this.output.println("?h for help");
            this.primaryLoop();
        } else {
            this.output.println("could not connect to the Rexster server");
        }
    }

    private void oneTimeExecuteScript(final String script) {
        this.initAndOpenSessionFromSettings();

        if (!this.session.isOpen()) {
            this.output.println("could not connect to the Rexster server");
        } else {
            this.executeScript(script, false);
        }
    }

    private void initAndOpenSessionFromSettings() {
        this.session = new RemoteRexsterSession(this.settings.getHost(), this.settings.getPort(),
                this.settings.getTimeout(), this.settings.getUsername(), this.settings.getPassword());
        this.session.open();
    }

    private void primaryLoop() throws Exception {

        final ConsoleReader reader = getInputReader();

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
                    this.closeConsole();
                    return;
                } else if (line.equals(Tokens.REXSTER_CONSOLE_HELP)) {
                    this.printHelp();
                } else if (line.equals(Tokens.REXSTER_CONSOLE_BINDINGS)) {
                    this.printBindings();
                } else if (line.equals(Tokens.REXSTER_CONSOLE_RESET)) {
                    this.resetSessionWithRexster();
                } else if (line.startsWith(Tokens.REXSTER_CONSOLE_EXECUTE)) {
                    final String fileToExecute = line.substring(Tokens.REXSTER_CONSOLE_EXECUTE.length()).trim();
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
                    changeLanugage(line);
                } else {
                    executeScript(line);
                }

            } catch (Exception e) {
                this.output.println("Evaluation error: " + e.getMessage());
            }
        }
    }

    private void changeLanugage(final String line) {
        final String langToChangeTo = line.substring(1);
        if (langToChangeTo == null || langToChangeTo.isEmpty()) {
            this.output.println("specify a language on Rexster ?<language-name>");
            this.printAvailableLanguages();
        } else if (this.session.isAvailableLanguage(langToChangeTo)) {
            this.settings.setLanguage(langToChangeTo);
        } else {
            this.output.println("not a valid language on Rexster: [" + langToChangeTo + "].");
            this.printAvailableLanguages();
        }
    }

    private void resetSessionWithRexster() {
        this.output.print("resetting session with Rexster " + this.settings.getHostPort());
        if (this.session != null) {
            this.session.reset();
        } else {
            this.initAndOpenSessionFromSettings();
        }

        // reset binding cache in the console...it will come back fresh from the server on the
        // next script eval
        this.currentBindings.clear();

        this.output.println("--> done");
    }

    private void closeConsole() {
        this.output.print("closing session with Rexster " + this.settings.getHostPort());
        if (this.session != null) {
            this.session.close();
            this.session = null;
        }
        this.output.println("--> done");
    }

    private ConsoleReader getInputReader() throws IOException {
        final ConsoleReader reader = new ConsoleReader();
        reader.setBellEnabled(false);
        reader.setUseHistory(true);

        try {
            final History history = new History();
            history.setHistoryFile(new File(REXSTER_HISTORY));
            reader.setHistory(history);
        } catch (IOException e) {
            System.err.println("Could not find history file");
        }
        return reader;
    }

    private void executeScript(final String line) {
        executeScript(line, true);
    }

    private void executeScript(final String line, final boolean showPrefix) {
        final ResultAndBindings result = eval(line, this.settings.getLanguage(), this.session);
        final Iterator itty;
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
            final Object o = itty.next();
            if (o != null) {
                if (showPrefix) {
                    this.output.println("==>" + o);
                } else {
                    this.output.println(o);
                }
            }
        }

        this.currentBindings = result.getBindings();
    }

    private void printAvailableLanguages() {
        this.output.println("-= Available Languages =-");

        final Iterator<String> languages = this.session.getAvailableLanguages();
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
        return "rexster[" + this.settings.getLanguage() + "]> ";
    }

    public static String makeSpace(final int number) {
        String space = "";
        for (int i = 0; i < number; i++) {
            space = space + " ";
        }
        return space;
    }

    private static ResultAndBindings eval(final String script, final String scriptEngineName,
                                          final RemoteRexsterSession session) {

        ResultAndBindings returnValue = null;

        try {
            session.open();

            // the session field gets set by the RemoteRexsterSession class automatically
            final ScriptRequestMessage scriptMessage = new ScriptRequestMessage();
            scriptMessage.Script = script;
            scriptMessage.Bindings = ConsoleScriptResponseMessage.convertBindingsToByteArray(new RexsterBindings());
            scriptMessage.LanguageName = scriptEngineName;
            scriptMessage.Flag = MessageFlag.SCRIPT_REQUEST_IN_SESSION;
            scriptMessage.setRequestAsUUID(UUID.randomUUID());

            final RexProMessage resultMessage = session.sendRequest(scriptMessage, 3, 500);

            List<String> lines = new ArrayList<String>();
            List<String> bindings = new ArrayList<String>();
            try {
                if (resultMessage instanceof ConsoleScriptResponseMessage) {
                    final ConsoleScriptResponseMessage responseMessage = (ConsoleScriptResponseMessage) resultMessage;

                    bindings = responseMessage.bindingsAsList();
                    lines = responseMessage.consoleLinesAsList();
                } else if (resultMessage instanceof ErrorResponseMessage) {
                    final ErrorResponseMessage errorMessage = (ErrorResponseMessage) resultMessage;
                    lines = new ArrayList() {{
                        add(errorMessage.ErrorMessage);
                    }};
                }
            } catch (IllegalArgumentException iae) {
                ErrorResponseMessage errorMessage = (ErrorResponseMessage) resultMessage;
                lines.add(errorMessage.ErrorMessage);
            }

            Object result = lines.iterator();

            if (lines.size() == 1) {
                result = lines.get(0);
            }

            returnValue = new ResultAndBindings(result, bindings);

        } catch (Exception e) {
            System.out.println("The session with Rexster Server may have been lost.  Please try again or refresh your session with ?r");
        }

        return returnValue;
    }

    private static String readFile(final String file) throws IOException {
        final BufferedReader reader = new BufferedReader(new FileReader(file));
        String line;

        final StringBuilder stringBuilder = new StringBuilder();
        final String ls = System.getProperty("line.separator");
        while ((line = reader.readLine()) != null) {
            stringBuilder.append(line);
            stringBuilder.append(ls);
        }

        return stringBuilder.toString();
    }

    public static void main(final String[] args) throws Exception {
        try {
            final ConsoleSettings settings = new ConsoleSettings(args);
            new RexsterConsole(settings).start();
        } catch (Exception ex) {
            die(ex);
        }
    }

    private static void die(final Throwable ex) {
        System.out.println(ex.getMessage() + " (stack trace follows)");
        ex.printStackTrace();
        System.exit(1);
    }
}
