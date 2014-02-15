package com.tinkerpop.rexster.protocol;

import org.apache.log4j.Logger;

import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manages the list of EngineHolder items for the current ScriptEngineManager.
 * By default, the ScriptEngine instance is never reset.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 * @author Blake Eggleston (bdeggleston.github.com)
 */
public class EngineController {
    private static final Logger logger = Logger.getLogger(EngineController.class);

    public static final int RESET_NEVER = -1;

    private final Map<String, EngineHolder> engines = new ConcurrentHashMap<String, EngineHolder>();
    private static final List<EngineConfiguration> engineConfigurations = new ArrayList<EngineConfiguration>();

    private static final Object lock = new Object();
    private static final AtomicBoolean initializing = new AtomicBoolean(false);

    /**
     * All gremlin engines are prefixed with this value.
     */
    private static final String ENGINE_NAME_PREFIX = "gremlin-";

    private static EngineController engineController;

    private EngineController() {
        // for ruby
        System.setProperty("org.jruby.embed.localvariable.behavior", "persistent");
        loadEngines();
    }

    private void loadEngines() {
        initializing.set(true);
        engines.clear();

        final ScriptEngineManager manager = new ScriptEngineManager();
        for (ScriptEngineFactory factory : manager.getEngineFactories()) {

            logger.info(String.format("ScriptEngineManager has factory for: %s", factory.getLanguageName()));

            // only add engine factories for those languages that are gremlin based.
            final EngineConfiguration engineConfiguration = findEngineConfiguration(factory.getLanguageName());
            if (engineConfiguration != null) {
                logger.info(String.format("Registered ScriptEngine for: %s", factory.getLanguageName()));
                this.engines.put(factory.getLanguageName(), new EngineHolder(factory, engineConfiguration));
            }
        }

        initializing.set(false);
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
        configure(resetCount, initScriptFile, configuredEngineNames, null, null);
    }

    /**
     * Must call this before a call to getInstance() if the reset count is to be taken into account.  ScriptEngine
     * imports can be defined that are included in addition to the standard imports provided by Gremlin.  These
     * imports only get included for the GremlinGroovyScriptEngine at this time.
     *
     * @param configuredEngineNames A list of script engine names that should be exposed.
     * @param imports A list of imports for the script engine.
     * @param staticImports A list of static imports for the script engine.
     */
    public static void configure(final int resetCount, final String initScriptFile, final Set<String> configuredEngineNames,
                                 final Set<String> imports, final Set<String> staticImports){
        final List<EngineConfiguration> confs = new ArrayList<EngineConfiguration>();
        for (String configuredEngineName : configuredEngineNames) {
            confs.add(new EngineConfiguration(configuredEngineName, resetCount, initScriptFile, imports, staticImports));
        }

        configure(confs);
    }

    public synchronized static void configure(final List<EngineConfiguration> configurations) {
        // need to null the singleton controller.  this will kill script processing dead.  it's as good as
        // restarting the server practically.
        engineController = null;
        engineConfigurations.clear();
        engineConfigurations.addAll(configurations);
    }

    public static EngineController getInstance() {
        if (engineController == null) {
            // hack to deal with this singleton that needs to be created in a synchronous manner without having
            // to make the entire method synchronized.  guess this would work, though a bit of an ugly double check
            synchronized (lock) {
                if (engineController == null)
                    engineController = new EngineController();
            }
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
        while(initializing.get()) {
            try {
                Thread.sleep(100);
            } catch (Exception ex) {
                logger.warn("An engine was requested while the ScriptEngine was initializing.", ex);
                throw new RuntimeException(ex);
            }
        }

        final EngineHolder engine = this.engines.get(ENGINE_NAME_PREFIX + languageName);
        if (engine == null) {
            throw new ScriptException("No engine for the language: " + languageName);
        }

        return engine;
    }

    private static EngineConfiguration findEngineConfiguration(final String engineName) {
        for (EngineConfiguration conf : engineConfigurations) {
            if (conf.getScriptEngineName().equals(engineName)) {
                return conf;
            }
        }

        return null;
    }
}
