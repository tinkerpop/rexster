package com.tinkerpop.rexster.protocol;

import com.tinkerpop.pipes.SingleIterator;
import com.tinkerpop.rexster.Tokens;
import jline.ConsoleReader;
import jline.History;

import javax.script.Bindings;
import javax.script.ScriptContext;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.Map;

public class RexsterConsole {

    private final RexsterScriptEngine rexster = (RexsterScriptEngine) new RexsterScriptEngineFactory().getScriptEngine();
    private final PrintStream output = System.out;

    private static final String REXSTER_HISTORY = ".rexster_history";

    public RexsterConsole(String host, int port, String language) throws Exception {

        this.output.println("        (l_(l");
        this.output.println("(_______( 0 0");
        this.output.println("(        (-Y-)");
        this.output.println("l l-----l l");
        this.output.println("l l,,   l l,,");

        this.rexster.put(RexsterScriptEngine.CONFIG_SCOPE_HOST, host);
        this.rexster.put(RexsterScriptEngine.CONFIG_SCOPE_PORT, port);
        this.rexster.put(RexsterScriptEngine.CONFIG_SCOPE_LANGUAGE, language);

        this.rexster.setBindings(this.rexster.createBindings(), ScriptContext.ENGINE_SCOPE);
        this.rexster.setBindings(this.rexster.createBindings(), ScriptContext.GLOBAL_SCOPE);

        this.primaryLoop();
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
                if (line.equals(Tokens.REXSTER_CONSOLE_QUIT))
                    return;
                else if (line.equals(Tokens.REXSTER_CONSOLE_HELP))
                    this.printHelp();
                else if (line.equals(Tokens.REXSTER_CONSOLE_BINDINGS))
                    this.printBindings(this.rexster.getBindings(ScriptContext.ENGINE_SCOPE));
                else {
                    Object result = this.rexster.eval(line);
                    Iterator itty;
                    if (result instanceof Iterator) {
                        itty = (Iterator) result;
                    } else if (result instanceof Iterable) {
                        itty = ((Iterable) result).iterator();
                    } else if (result instanceof Map) {
                        itty = ((Map) result).entrySet().iterator();
                    } else {
                        itty = new SingleIterator<Object>(result);
                    }

                    while (itty.hasNext()) {
                        this.output.println("==>" + itty.next());
                    }
                }

            } catch (Exception e) {
                this.output.println("Evaluation error: " + e.getMessage());
            }
        }
    }

    public void printHelp() {
        this.output.println("-= Console Specific =-");
        this.output.println(Tokens.REXSTER_CONSOLE_QUIT + ": quit");
    }

    public void printBindings(final Bindings bindings) {
        for (Map.Entry<String, Object> entry : bindings.entrySet()) {
            this.output.println(entry);
        }
    }

    public String getPrompt() {
        return "rexster> ";
    }

    public static String makeSpace(int number) {
        String space = new String();
        for (int i = 0; i < number; i++) {
            space = space + " ";
        }
        return space;
    }

    public static void main(String[] args) throws Exception {

        String host = args[0];
        int port = Integer.parseInt(args[1]);
        String language = args[2];

        new RexsterConsole(host, port, language);
    }
}
