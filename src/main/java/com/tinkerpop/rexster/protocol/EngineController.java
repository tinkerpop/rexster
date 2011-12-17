package com.tinkerpop.rexster.protocol;

import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Manages the list of EngineHolder items for the current ScriptEngineManager.
 */
public class EngineController {
    private final ScriptEngineManager manager = new ScriptEngineManager();
    private final List<EngineHolder> engines = new ArrayList<EngineHolder>();

    /**
     * All gremlin engines are prefixed with this value.
     */
    private static final String ENGINE_NAME_PREFIX = "gremlin-";

    /**
     * Add all flavors of gremlin to this list. This should be the true name of the language.
     */
    private final List<String> gremlinEngineNames = new ArrayList<String>(){{
       add("gremlin-groovy");
    }};

    private static EngineController engineController;

    private EngineController() {
        // for ruby
        System.setProperty("org.jruby.embed.localvariable.behavior", "persistent");
        for (ScriptEngineFactory factory : this.manager.getEngineFactories()) {

            // only add engine factories for those languages that are gremlin based.
            if (gremlinEngineNames.contains(factory.getLanguageName())) {
                this.engines.add(new EngineHolder(factory));
            }
        }
    }

    public static EngineController getInstance() {
        if (engineController == null) {
            engineController = new EngineController();
        }

        return engineController;
    }

    public Iterator getAvailableEngineLanguages() {
        List<String> languages = new ArrayList<String>();
        for (EngineHolder engine : this.engines) {
            String fullLanguageName = engine.getLanguageName();
            languages.add(fullLanguageName.substring(fullLanguageName.indexOf(ENGINE_NAME_PREFIX) + ENGINE_NAME_PREFIX.length()));
        }

        return languages.iterator();
    }

    public boolean isEngineAvailable(String languageName) {
        boolean available = false;
        try {
            getEngineByLanguageName(languageName);
            available = true;
        } catch (ScriptException se) {
            available = false;
        }

        return available;
    }

    public EngineHolder getEngineByLanguageName(String languageName) throws ScriptException {
        for (EngineHolder engine : this.engines) {
            if (engine.getLanguageName().equals(ENGINE_NAME_PREFIX + languageName))
                return engine;
        }

        throw new ScriptException("No engine for the language: " + languageName);
    }
}
