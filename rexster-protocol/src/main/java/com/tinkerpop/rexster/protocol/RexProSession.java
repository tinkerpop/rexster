package com.tinkerpop.rexster.protocol;

import com.tinkerpop.rexster.server.RexsterApplication;
import com.tinkerpop.rexster.Tokens;

import javax.script.Bindings;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import java.util.Date;

/**
 * Server-side rexster session.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class RexProSession {

    private final Bindings bindings = new SimpleBindings();

    private final String sessionKey;

    private final byte channel;

    private final EngineController controller = EngineController.getInstance();

    private Date lastTimeUsed = new Date();

    public RexProSession(final String sessionKey, final RexsterApplication rexsterApplication, final byte channel, final int chunkSize) {
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

    public Object evaluate(final String script, final String languageName, final RexsterBindings rexsterBindings) throws ScriptException {
        Object result = null;
        try {
            final EngineHolder engine = this.controller.getEngineByLanguageName(languageName);

            if (rexsterBindings != null) {
                this.bindings.putAll(rexsterBindings);
            }

            result = engine.getEngine().eval(script, this.bindings);
        } catch (ScriptException se) {
            throw se;
        } finally {
            this.lastTimeUsed = new Date();
        }

        return result;
    }
}
