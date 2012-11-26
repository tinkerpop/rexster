package com.tinkerpop.rexster.protocol;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Holder class for different script engines.  It keeps track of the number of scripts that have been
 * evaluated against it and re-instantiates it to clear memory that it may hang on to.  This is an
 * issue that is prevalent in gremlin-groovy.  Unsure if it carries over to other implementations.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class EngineHolder {

    private final String languageName;
    private final String languageVersion;
    private final String engineName;
    private final String engineVersion;
    private final ScriptEngineFactory factory;
    private final int engineResetThreshold;

    private ScriptEngine engine;
    private AtomicInteger numberOfScriptsEvaluated = new AtomicInteger(1);

    public EngineHolder(final ScriptEngineFactory factory, final int engineResetThreshold) {
        this.languageName = factory.getLanguageName();
        this.languageVersion = factory.getLanguageVersion();
        this.engineName = factory.getEngineName();
        this.engineVersion = factory.getEngineVersion();
        this.engine = factory.getScriptEngine();
        this.factory = factory;
        this.engineResetThreshold = engineResetThreshold;
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
                this.engine = this.factory.getScriptEngine();
                numberOfScriptsEvaluated.set(1);
            } else {
                numberOfScriptsEvaluated.incrementAndGet();
            }
        }

        return this.engine;
    }


}
