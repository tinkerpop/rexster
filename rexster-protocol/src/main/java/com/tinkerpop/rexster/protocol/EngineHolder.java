package com.tinkerpop.rexster.protocol;

import org.apache.log4j.Logger;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.util.StringTokenizer;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Holder class for different script engines.  It keeps track of the number of scripts that have been
 * evaluated against it and re-instantiates it to clear memory that it may hang on to.  This is an
 * issue that is prevalent in gremlin-groovy.  Unsure if it carries over to other implementations.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class EngineHolder {
    private static final Logger logger = Logger.getLogger(EngineHolder.class);

    private final String languageName;
    private final String languageVersion;
    private final String engineName;
    private final String engineVersion;
    private final String initializationScriptFile;
    private final ScriptEngineFactory factory;
    private final int engineResetThreshold;

    private ScriptEngine engine;
    private AtomicInteger numberOfScriptsEvaluated = new AtomicInteger(1);

    public EngineHolder(final ScriptEngineFactory factory, final int engineResetThreshold,
                        final String initializationScriptFile) {
        this.languageName = factory.getLanguageName();
        this.languageVersion = factory.getLanguageVersion();
        this.engineName = factory.getEngineName();
        this.engineVersion = factory.getEngineVersion();
        this.engineResetThreshold = engineResetThreshold;
        this.initializationScriptFile = initializationScriptFile;
        this.factory = factory;
        this.engine = initEngine(this.factory, this.initializationScriptFile);
    }

    public String getEngineName() {
        return engineName;
    }

    public String getEngineVersion() {
        return engineVersion;
    }

    public String getLanguageName() {
        return languageName;
    }

    public ScriptEngine getEngine() {
        if (engineResetThreshold > EngineController.RESET_NEVER) {
            // determine if a reset is necessary.
            if (numberOfScriptsEvaluated.get() >= engineResetThreshold) {
                // IMPORTANT: assumes that the factory implementation is not pooling engine instances
                this.engine = initEngine(this.factory, this.initializationScriptFile);
                numberOfScriptsEvaluated.set(1);
            } else {
                numberOfScriptsEvaluated.incrementAndGet();
            }
        }

        return this.engine;
    }

    private static ScriptEngine initEngine(final ScriptEngineFactory factory, final String scriptFile) {
        final ScriptEngine engine = factory.getScriptEngine();

        if (scriptFile != null && !scriptFile.isEmpty()) {
            final File scriptEngineInitFile = new File(scriptFile);

            if (scriptEngineInitFile.exists()) {
                try {
                    final Reader reader = new FileReader(scriptEngineInitFile);
                    logger.info("ScriptEngine initializing with a custom script");
                    engine.eval(reader);
                } catch (FileNotFoundException fnfe) {
                    logger.warn("Could not read ScriptEngine initialization file.  Check script-engine-init.groovy on classpath.");
                } catch (ScriptException ex) {
                    logger.warn("ScriptEngine initialization failure. Custom scripts and imports will not be initialized.", ex);
                }
            }
        }


        return engine;
    }
}
