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

public class RexProSession {
    private static final Logger logger = Logger.getLogger(RexProSession.class);

    private final Bindings bindings = new SimpleBindings();

    private final UUID sessionIdentifier;

    protected Date lastTimeUsed = new Date();

    public RexProSession(final RexsterApplication rexsterApplication) {
        this.sessionIdentifier = UUID.randomUUID();

        this.bindings.put(Tokens.REXPRO_REXSTER_CONTEXT, rexsterApplication);

        logger.info("New RexPro Session created: " + this.sessionIdentifier.toString());
    }

    public UUID getSessionIdentifier() {
        return this.sessionIdentifier;
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
