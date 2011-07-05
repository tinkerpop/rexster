package com.tinkerpop.rexster.protocol;

import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages the list of EngineHolder items for the current ScriptEngineManager.
 */
public class EngineController {
    private final ScriptEngineManager manager = new ScriptEngineManager();
    private final List<EngineHolder> engines = new ArrayList<EngineHolder>();

    private static EngineController engineController;

    private EngineController() {
        // for ruby
        System.setProperty("org.jruby.embed.localvariable.behavior", "persistent");
        for (ScriptEngineFactory factory : this.manager.getEngineFactories()) {
            this.engines.add(new EngineHolder(factory));
        }
    }

    public static EngineController getInstance() {
        if (engineController == null) {
            engineController = new EngineController();
        }

        return engineController;
    }

    public EngineHolder getEngineByLanguageName(String languageName) throws ScriptException {
        for (EngineHolder engine : this.engines) {
            if (engine.getLanguageName().equals(languageName))
                return engine;
        }

        throw new ScriptException("No engine for the language: " + languageName);
    }
}
