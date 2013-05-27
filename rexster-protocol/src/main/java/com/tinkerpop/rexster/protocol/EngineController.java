package com.tinkerpop.rexster.protocol;

import org.apache.log4j.Logger;

import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Manages the list of EngineHolder items for the current ScriptEngineManager.
 * By default, the ScriptEngine instance is never reset.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class EngineController {
    private static final Logger logger = Logger.getLogger(EngineController.class);

    public static final int RESET_NEVER = -1;

    private final Map<String, EngineHolder> engines = new HashMap<String, EngineHolder>();

    /**
     * All gremlin engines are prefixed with this value.
     */
    private static final String ENGINE_NAME_PREFIX = "gremlin-";

    /**
     * Add all flavors of gremlin to this list. This should be the true name of the language.
     */
    private static Set<String> gremlinEngineNames;

    private static EngineController engineController;
    private static String initializationScriptFile;
    private static int engineResetThreshold = RESET_NEVER;

    private EngineController() {
        // for ruby
        System.setProperty("org.jruby.embed.localvariable.behavior", "persistent");
        final ScriptEngineManager manager = new ScriptEngineManager();
        for (ScriptEngineFactory factory : manager.getEngineFactories()) {

            logger.info(String.format("ScriptEngineManager has factory for: %s", factory.getLanguageName()));

            // only add engine factories for those languages that are gremlin based.
            if (gremlinEngineNames.contains(factory.getLanguageName())) {
                logger.info(String.format("Registered ScriptEngine for: %s", factory.getLanguageName()));
                this.engines.put(factory.getLanguageName(), new EngineHolder(
                        factory, engineResetThreshold, initializationScriptFile));
            }
        }
    }

    /**
     * Must call this before a call to getInstance() if the reset count is to be taken into account. Defaults
     * to the gremlin groovy script engine.
     */
    public static void configure(final int resetCount, final String initScriptFile){
        configure(resetCount, initScriptFile, new HashSet<String>() {{
            add("gremlin-groovy");
        }});
    }

    /**
     * Must call this before a call to getInstance() if the reset count is to be taken into account.
     *
     * @param configuredEngineNames A list of script engine names that should be exposed.
     */
    public static void configure(final int resetCount, final String initScriptFile, final Set<String> configuredEngineNames){
        engineResetThreshold = resetCount;
        initializationScriptFile = initScriptFile;
        gremlinEngineNames = configuredEngineNames;
    }

    public static EngineController getInstance() {
        if (engineController == null) {
            engineController = new EngineController();
        }

        return engineController;
    }

    public List<String> getAvailableEngineLanguages() {
        final List<String> languages = new ArrayList<String>();
        for (String fullLanguageName : this.engines.keySet()) {
            languages.add(fullLanguageName.substring(fullLanguageName.indexOf(ENGINE_NAME_PREFIX) + ENGINE_NAME_PREFIX.length()));
        }

        return Collections.unmodifiableList(languages);
    }

    public boolean isEngineAvailable(final String languageName) {
        boolean available;
        try {
            getEngineByLanguageName(languageName);
            available = true;
        } catch (ScriptException se) {
            available = false;
        }

        return available;
    }

    public EngineHolder getEngineByLanguageName(final String languageName) throws ScriptException {

        final EngineHolder engine = this.engines.get(ENGINE_NAME_PREFIX + languageName);
        if (engine == null) {
            throw new ScriptException("No engine for the language: " + languageName);
        }

        return engine;
    }
}
