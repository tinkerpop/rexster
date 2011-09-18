package com.tinkerpop.rexster.protocol.filter;

import com.tinkerpop.rexster.protocol.RexProSession;
import com.tinkerpop.rexster.protocol.message.ErrorResponseMessage;
import com.tinkerpop.rexster.protocol.message.RexProMessage;
import org.apache.log4j.Logger;
import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;
import org.glassfish.grizzly.memory.MemoryManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RexProMessageFilter extends BaseFilter {

    private static final Logger logger = Logger.getLogger(RexProSession.class);

    public NextAction handleRead(final FilterChainContext ctx) throws IOException {
        // Get the source buffer from the context
        final Buffer sourceBuffer = ctx.getMessage();

        final int sourceBufferLength = sourceBuffer.remaining();

        // If source buffer doesn't contain header
        if (sourceBufferLength < RexProMessage.HEADER_SIZE) {
            // stop the filterchain processing and store sourceBuffer to be
            // used next time
            return ctx.getStopAction(sourceBuffer);
        }

        // Get the body length
        byte[] grr = new byte[sourceBufferLength];
        sourceBuffer.get(grr);
        sourceBuffer.rewind();
        final int bodyLength = sourceBuffer.getInt(RexProMessage.HEADER_SIZE - 4);
        // The complete message length
        final int completeMessageLength = RexProMessage.HEADER_SIZE + bodyLength;

        // If the source message doesn't contain entire body
        if (sourceBufferLength < completeMessageLength) {
            // stop the filterchain processing and store sourceBuffer to be
            // used next time
            return ctx.getStopAction(sourceBuffer);
        }

        // Check if the source buffer has more than 1 complete message
        // If yes - split up the first message and the remainder
        final Buffer remainder = sourceBufferLength > completeMessageLength ?
                sourceBuffer.split(completeMessageLength) : null;

        // Construct a message
        final RexProMessage message = RexProMessage.read(sourceBuffer);
        if (!message.isValid()) {
            logger.warn("Checksum failure - Message was not valid for session [" + message.getSessionAsUUID()
                    + "] and request [" + message.getRequestAsUUID() + "]");

            ctx.write(new ErrorResponseMessage(message.getSessionAsUUID(), message.getRequestAsUUID(),
                    com.tinkerpop.rexster.protocol.message.ErrorResponseMessage.FLAG_ERROR_MESSAGE_VALIDATION,
                    "Checksum failure"));
        }

        ctx.setMessage(message);

        sourceBuffer.tryDispose();

        // Instruct FilterChain to store the remainder (if any) and continue execution
        return ctx.getInvokeAction(remainder);
    }

    public NextAction handleWrite(final FilterChainContext ctx) throws IOException {
        // Get the source message to be written
        final List<RexProMessage> messages = new ArrayList<RexProMessage>();
        try {
            List<RexProMessage> msgs = ctx.getMessage();
            messages.addAll(msgs);
        } catch (ClassCastException cce) {
            RexProMessage msg = ctx.getMessage();
            messages.add(msg);
        }

        final RexProMessage message = messages.get(0);

        List<RexProMessage> remainingMessages = null;
        if (messages.size() > 1) {
            remainingMessages = messages.subList(1, messages.size());
        }

        final int size = RexProMessage.HEADER_SIZE + message.getBodyLength();

        // Retrieve the memory manager
        final MemoryManager memoryManager =
                ctx.getConnection().getTransport().getMemoryManager();

        // allocate the buffer of required size
        final Buffer output = memoryManager.allocate(size);

        // Allow Grizzly core to dispose the buffer, once it's written
        output.allowBufferDispose(true);

        RexProMessage.write(output, message);

        // Set the Buffer as a context message
        ctx.setMessage(output.flip());

        // Instruct the FilterChain to call the next filter
        return ctx.getInvokeAction(remainingMessages);
    }
}
