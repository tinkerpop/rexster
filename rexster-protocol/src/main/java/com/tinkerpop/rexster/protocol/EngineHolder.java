package com.tinkerpop.rexster.protocol;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;

public class EngineHolder {

    private final String languageName;
    private final String languageVersion;
    private final String engineName;
    private final String engineVersion;
    private final ScriptEngineFactory factory;
    public static final int ENGINE_RESET_THRESHOLD = 1000;

    private ScriptEngine engine;
    private int numberOfScriptsEvaluated = 0;

    public EngineHolder(final ScriptEngineFactory factory) {
        this.languageName = factory.getLanguageName();
        this.languageVersion = factory.getLanguageVersion();
        this.engineName = factory.getEngineName();
        this.engineVersion = factory.getEngineVersion();
        this.engine = factory.getScriptEngine();
        this.factory = factory;
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
        if (numberOfScriptsEvaluated >= ENGINE_RESET_THRESHOLD) {
            // IMPORTANT: assumes that the factory implementation is not pooling engine instances
            this.engine = this.factory.getScriptEngine();
            numberOfScriptsEvaluated = 1;
        } else {
            numberOfScriptsEvaluated++;
        }

        return this.engine;
    }


}
