package com.tinkerpop.rexster.protocol;

import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.rexster.RexsterApplicationGraph;
import com.tinkerpop.rexster.Tokens;
import com.tinkerpop.rexster.server.RexsterApplication;

import java.util.Set;

/**
 * Proxies call to the RexsterApplication instance to limit the methods available in RexsterConsole.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 * @author Blake Eggleston (bdeggleston.github.com)
 */
public class RexsterApplicationHolder {
    private final RexsterApplication rexsterApplication;

    public RexsterApplicationHolder(final RexsterApplication rexsterApplication) {
        this.rexsterApplication = rexsterApplication;
    }

    public Graph getGraph(final String graphName) {
        return this.rexsterApplication.getGraph(graphName);
    }

    public RexsterApplicationGraph getApplicationGraph(final String graphName) {
        return this.rexsterApplication.getApplicationGraph(graphName);
    }

    public Set<String> getGraphNames() {
        return this.rexsterApplication.getGraphNames();
    }

    public String getVersion() {
        return Tokens.REXSTER_VERSION;
    }
}
