package com.tinkerpop.rexster.protocol;

import org.apache.log4j.Logger;
import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;

import javax.script.ScriptException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;

public class ScriptFilter extends BaseFilter {
    private static final Logger logger = Logger.getLogger(RexProSession.class);

    public NextAction handleRead(final FilterChainContext ctx) throws IOException {
        final RexProMessage message = ctx.getMessage();

        if (message.getType() == MessageType.SCRIPT_REQUEST) {
            ScriptRequestMessage specificMessage = new ScriptRequestMessage(message);

            RexProSession session = RexProSessions.getSession(specificMessage.getSessionAsUUID());
            try {
                Object result = session.evaluate(specificMessage.getScript(), specificMessage.getLanguageName());

                ByteArrayOutputStream byteOuputStream = new ByteArrayOutputStream();
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteOuputStream);
                objectOutputStream.writeObject(result);
                objectOutputStream.close();

                ByteBuffer bb = ByteBuffer.allocate(4 + byteOuputStream.size());
                bb.putInt(byteOuputStream.size());
                bb.put(byteOuputStream.toByteArray());

                ScriptResponseMessage resultMessage = new ScriptResponseMessage(message.getSessionAsUUID(),
                        ScriptResponseMessage.FLAG_COMPLETE_MESSAGE, bb.array());

                ctx.write(resultMessage);

            } catch (ScriptException se) {
                logger.warn("Could not process script [" + specificMessage.getScript() + "] for language ["
                        + specificMessage.getLanguageName() + "] on session [" + message.getSessionAsUUID()
                        + "] and request [" + message.getRequestAsUUID() + "]");

                ctx.write(new ErrorResponseMessage(message.getSessionAsUUID(), message.getRequestAsUUID(),
                        ErrorResponseMessage.FLAG_ERROR_SCRIPT_FAILURE,
                        "An error occurred while processing the script for language [" + specificMessage.getLanguageName() + "]: " + se.getMessage()));

                return ctx.getStopAction();
            }

            return ctx.getStopAction();
        }

        return ctx.getInvokeAction();
    }
}
