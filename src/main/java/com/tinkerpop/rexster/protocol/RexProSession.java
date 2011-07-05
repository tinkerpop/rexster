package com.tinkerpop.rexster.protocol;

import com.tinkerpop.rexster.RexsterApplication;
import com.tinkerpop.rexster.Tokens;
import com.tinkerpop.rexster.WebServer;
import com.tinkerpop.rexster.WebServerRexsterApplicationProvider;
import org.apache.log4j.Logger;

import javax.script.Bindings;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import java.util.Date;
import java.util.UUID;

/**
 * Server-side rexster session.
 */
public class RexProSession {

    private final Bindings bindings = new SimpleBindings();

    private final UUID sessionKey;

    protected Date lastTimeUsed = new Date();

    public RexProSession(final UUID sessionKey, final RexsterApplication rexsterApplication) {
        this.sessionKey = sessionKey;

        this.bindings.put(Tokens.REXPRO_REXSTER_CONTEXT, rexsterApplication);
    }

    public UUID getSessionKey() {
        return this.sessionKey;
    }

    public Bindings getBindings() {
        return this.bindings;
    }

    public long getIdleTime() {
        return (new Date()).getTime() - this.lastTimeUsed.getTime();
    }

    public Object evaluate(String script, String languageName, RexsterBindings rexsterBindings) throws ScriptException{
        EngineController controller = EngineController.getInstance();

        Object result = null;
        try {
            EngineHolder engine = controller.getEngineByLanguageName(languageName);

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
