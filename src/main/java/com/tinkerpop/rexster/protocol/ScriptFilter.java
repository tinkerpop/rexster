package com.tinkerpop.rexster.protocol;

import org.apache.log4j.Logger;
import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;

import javax.script.ScriptException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.Iterator;

public class ScriptFilter extends BaseFilter {
    private static final Logger logger = Logger.getLogger(RexProSession.class);

    public NextAction handleRead(final FilterChainContext ctx) throws IOException {
        final RexProMessage message = ctx.getMessage();

        if (message.getType() == MessageType.SCRIPT_REQUEST) {
            ScriptRequestMessage specificMessage = new ScriptRequestMessage(message);

            RexProSession session = RexProSessions.getSession(specificMessage.getSessionAsUUID());
            try {
                Object result = session.evaluate(specificMessage.getScript(), specificMessage.getLanguageName());

                ScriptResponseMessage resultMessage = new ScriptResponseMessage(message.getSessionAsUUID(),
                        ScriptResponseMessage.FLAG_COMPLETE_MESSAGE, getBytesBasedOnObject(result));

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

    private byte[] getBytesBasedOnObject(Object result) throws IOException {
        if (result instanceof Iterable) {
            ByteArrayOutputStream byteOuputStream = new ByteArrayOutputStream();
            for (Object o : (Iterable) result) {
                byte[] bytesToWrite = getBytes(o);
                byteOuputStream.write(bytesToWrite, 0, bytesToWrite.length);
            }

            return byteOuputStream.toByteArray();
        } else if (result instanceof Iterator) {
            ByteArrayOutputStream byteOuputStream = new ByteArrayOutputStream();
            Iterator itty = (Iterator) result;
            while (itty.hasNext()) {
               byte[] bytesToWrite = getBytes(itty.next());
                byteOuputStream.write(bytesToWrite, 0, bytesToWrite.length);
            }

            return byteOuputStream.toByteArray();
        } else {
            return getBytes(result);
        }
    }

    private byte[] getBytes(Object result) throws IOException {

        if (result == null) {
            return null;
        } else if (result instanceof Serializable) {
            ByteArrayOutputStream byteOuputStream = new ByteArrayOutputStream();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteOuputStream);
            objectOutputStream.writeObject(result);
            objectOutputStream.close();

            ByteBuffer bb = ByteBuffer.allocate(4 + byteOuputStream.size());
            bb.putInt(byteOuputStream.size());
            bb.put(byteOuputStream.toByteArray());

            return bb.array();
        } else {
            return result.toString().getBytes();
        }
    }
}
