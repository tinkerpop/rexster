package com.tinkerpop.rexster.protocol;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;

public class EngineHolder {

    private final String languageName;
    private final String languageVersion;
    private final String engineName;
    private final String engineVersion;
    private final ScriptEngine engine;

    public EngineHolder(final ScriptEngineFactory factory) {
        this.languageName = factory.getLanguageName();
        this.languageVersion = factory.getLanguageVersion();
        this.engineName = factory.getEngineName();
        this.engineVersion = factory.getEngineVersion();
        this.engine = factory.getScriptEngine();
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
        return this.engine;
    }


}
