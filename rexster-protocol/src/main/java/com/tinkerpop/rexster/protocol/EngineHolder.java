package com.tinkerpop.rexster.protocol;

import com.tinkerpop.gremlin.groovy.jsr223.DefaultImportCustomizerProvider;
import org.apache.log4j.Logger;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Holder class for different script engines.  It keeps track of the number of scripts that have been
 * evaluated against it and re-instantiates it to clear memory that it may hang on to.  This is an
 * issue that is prevalent in gremlin-groovy.  Unsure if it carries over to other implementations.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 * @author Blake Eggleston (bdeggleston.github.com)
 */
public class EngineHolder {
    private static final Logger logger = Logger.getLogger(EngineHolder.class);

    private final String languageName;
    private final String languageVersion;
    private final String engineName;
    private final String engineVersion;
    private final Set<String> initScriptFiles;
    private final ScriptEngineFactory factory;
    private final int engineResetThreshold;

    private ScriptEngine engine;
    private AtomicInteger numberOfScriptsEvaluated = new AtomicInteger(1);

    public EngineHolder(final ScriptEngineFactory factory, final EngineConfiguration configuration) {
        this.languageName = factory.getLanguageName();
        this.languageVersion = factory.getLanguageVersion();
        this.engineName = factory.getEngineName();
        this.engineVersion = factory.getEngineVersion();
        this.engineResetThreshold = configuration.getResetCount();
        this.initScriptFiles = configuration.getInitScriptFiles();
        this.factory = factory;

        // gremlin-groovy allows imports to be customized.  need to figure out how to implement this generically
        // across scriptengines.
        if (this.languageName.equals("gremlin-groovy")) {
            logger.info("Initializing gremlin-groovy engine with additional imports.");
            DefaultImportCustomizerProvider.initializeStatically(configuration.getImports(), configuration.getStaticImports());
        }

        this.engine = initEngine(this.factory, this.initScriptFiles);
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
        /*
        // commit in Gremlin in 2.3.0 removes the need for this reset. unsure if this should be removed completely
        // though as other scriptengine implementations might need it???
        if (engineResetThreshold > EngineController.RESET_NEVER) {
            // determine if a reset is necessary.
            if (numberOfScriptsEvaluated.get() >= engineResetThreshold) {
                // IMPORTANT: assumes that the factory implementation is not pooling engine instances
                this.engine = initEngine(this.factory, this.initScriptFiles);
                numberOfScriptsEvaluated.set(1);
            } else {
                numberOfScriptsEvaluated.incrementAndGet();
            }
        }
        */

        if (engine == null) {
            // IMPORTANT: assumes that the factory implementation is not pooling engine instances
            this.engine = initEngine(this.factory, this.initScriptFiles);
        }

        return this.engine;
    }

    private static ScriptEngine initEngine(final ScriptEngineFactory factory, final Set<String> scriptFiles) {
        final ScriptEngine engine = factory.getScriptEngine();

        for (String scriptFile : scriptFiles) {
            if (scriptFile != null && !scriptFile.isEmpty()) {
                final File scriptEngineInitFile = new File(scriptFile);

                if (scriptEngineInitFile.exists()) {
                    try {
                        final Reader reader = new FileReader(scriptEngineInitFile);
                        logger.info("ScriptEngine initializing with a custom script");
                        engine.eval(reader);
                    } catch (FileNotFoundException fnfe) {
                        logger.warn(String.format("Could not read ScriptEngine initialization file.  Check location of [%s].", scriptEngineInitFile.getAbsolutePath()));
                    } catch (ScriptException ex) {
                        logger.warn(String.format("ScriptEngine initialization failure. Custom scripts and imports will not be initialized by [%s].", scriptEngineInitFile.getAbsolutePath()), ex);
                    }
                } else {
                    logger.warn(String.format("ScriptEngine initialization file does not exist.  Check location of [%s].", scriptEngineInitFile.getAbsolutePath()));
                }
            }
        }


        return engine;
    }
}
