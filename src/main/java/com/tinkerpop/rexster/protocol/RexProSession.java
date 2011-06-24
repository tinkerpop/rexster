package com.tinkerpop.rexster.protocol;

import org.apache.log4j.Logger;

import javax.script.Bindings;
import javax.script.ScriptException;
import java.util.Date;
import java.util.UUID;

public class RexProSession {
    private static final Logger logger = Logger.getLogger(RexProSession.class);

    private Bindings bindings;

    private final UUID sessionIdentifier;

    protected Date lastTimeUsed = new Date();

    public RexProSession() {
        this.sessionIdentifier = UUID.randomUUID();

        logger.info("New RexPro Session created: " + this.sessionIdentifier.toString());
    }

    public UUID getSessionIdentifier() {
        return this.sessionIdentifier;
    }

    public Bindings getBindings() {
        return this.bindings;
    }

    public void setBindings(Bindings bindings) {
        this.bindings = bindings;
    }

    public long getIdleTime() {
        return (new Date()).getTime() - this.lastTimeUsed.getTime();
    }

    public Object evaluate(String script, String languageName) throws ScriptException{
        EngineController controller = EngineController.getInstance();

        Object result = null;
        try {
            EngineHolder engine = controller.getEngineByLanguageName(languageName);
            result = engine.getEngine().eval(script, this.bindings);
        } catch (ScriptException se) {
            throw se;
        } finally {
            this.lastTimeUsed = new Date();
        }

        return result;
    }
}
