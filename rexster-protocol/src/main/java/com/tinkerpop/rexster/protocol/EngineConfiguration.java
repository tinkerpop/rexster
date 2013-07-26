package com.tinkerpop.rexster.protocol;

import org.apache.commons.configuration.HierarchicalConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Configuration class for a ScriptEngine implementation.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 * @author Blake Eggleston (bdeggleston.github.com)
 */
public class EngineConfiguration {

    private final int resetCount;
    private final Set<String> initScriptFiles;
    private final String scriptEngineName;
    private final Set<String> imports;
    private final Set<String> staticImports;

    public EngineConfiguration(final HierarchicalConfiguration configuration) {
        this.scriptEngineName = configuration.getString("name", "gremlin-groovy");
        this.initScriptFiles = new HashSet<String>(configuration.getList("init-scripts", new ArrayList()));
        this.resetCount = configuration.getInt("reset-threshold", EngineController.RESET_NEVER);
        this.imports = new HashSet<String>(configuration.getList("imports", new ArrayList()));
        this.staticImports = new HashSet<String>(configuration.getList("static-imports", new ArrayList()));
    }

    public EngineConfiguration(final String scriptEngineName, final int resetCount, final String initScriptFile,
                               final Set<String> imports, final Set<String> staticImports) {
        this.resetCount = resetCount;
        this.initScriptFiles = new HashSet<String>();
        this.initScriptFiles.add(initScriptFile);
        this.scriptEngineName = scriptEngineName;
        this.imports = imports;
        this.staticImports = staticImports;
    }

    public int getResetCount() {
        return resetCount;
    }

    public Set<String> getInitScriptFiles() {
        return initScriptFiles;
    }

    public String getScriptEngineName() {
        return scriptEngineName;
    }

    public Set<String> getImports() {
        return imports;
    }

    public Set<String> getStaticImports() {
        return staticImports;
    }
}
