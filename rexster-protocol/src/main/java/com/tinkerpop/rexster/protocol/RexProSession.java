package com.tinkerpop.rexster.protocol;

import com.tinkerpop.rexster.server.RexsterApplication;
import com.tinkerpop.rexster.Tokens;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Server-side rexster session.  All requests to a session are bound to a specific thread.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class RexProSession {

    private final Bindings bindings = new SimpleBindings();

    private final String sessionKey;

    private final byte channel;

    private final EngineController controller = EngineController.getInstance();

    private Date lastTimeUsed = new Date();

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public RexProSession(final String sessionKey, final RexsterApplication rexsterApplication, final byte channel) {
        this.sessionKey = sessionKey;
        this.channel = channel;
        this.bindings.put(Tokens.REXPRO_REXSTER_CONTEXT, rexsterApplication);
    }

    public String getSessionKey() {
        return this.sessionKey;
    }

    public Bindings getBindings() {
        return this.bindings;
    }

    public byte getChannel() {
        return this.channel;
    }

    public long getIdleTime() {
        return (new Date()).getTime() - this.lastTimeUsed.getTime();
    }

    public void kill() {
        this.executor.shutdown();
    }

    public Object evaluate(final String script, final String languageName, final RexsterBindings rexsterBindings) throws ScriptException {
        Object result = null;
        try {
            final EngineHolder engine = this.controller.getEngineByLanguageName(languageName);

            if (rexsterBindings != null) {
                this.bindings.putAll(rexsterBindings);
            }

            final Future future = this.executor.submit(new Evaluator(engine.getEngine(), script, this.bindings));
            result = future.get();
        } catch (Exception se) {
            throw new RuntimeException(se);
        } finally {
            this.lastTimeUsed = new Date();
        }

        return result;
    }

    private class Evaluator implements Callable {

        private final ScriptEngine engine;
        private final Bindings bindings;
        private final String script;

        public Evaluator(final ScriptEngine engine, final String script, final Bindings bindings) {
            this.engine = engine;
            this.bindings = bindings;
            this.script = script;
        }

        @Override
        public Object call() throws Exception {
            return this.engine.eval(this.script, this.bindings);
        }
    }
}
