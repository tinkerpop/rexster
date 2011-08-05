package com.tinkerpop.rexster.protocol;

import com.tinkerpop.rexster.RexsterApplication;
import com.tinkerpop.rexster.protocol.message.ConsoleScriptResponseMessage;
import com.tinkerpop.rexster.protocol.message.RexProMessage;
import com.tinkerpop.rexster.protocol.message.ScriptRequestMessage;
import com.tinkerpop.rexster.protocol.message.ScriptResponseMessage;

import javax.script.ScriptException;
import java.io.IOException;
import java.util.UUID;

/**
 * Server-side RexPro session representing a Console Channel.
 */
public class ConsoleRexProSession extends AbstractRexProSession {

    public ConsoleRexProSession(final UUID sessionKey, final RexsterApplication rexsterApplication) {
        super(sessionKey, rexsterApplication);
    }

    public RexProMessage evaluateToRexProMessage(ScriptRequestMessage request) throws ScriptException, IOException {
        Object result = this.evaluate(request.getScript(),
                request.getLanguageName(), request.getBindings());

        return new ConsoleScriptResponseMessage(request.getSessionAsUUID(),
                ScriptResponseMessage.FLAG_COMPLETE_MESSAGE, result, this.getBindings());
    }
}
