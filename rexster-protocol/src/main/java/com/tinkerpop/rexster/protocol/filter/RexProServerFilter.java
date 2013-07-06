package com.tinkerpop.rexster.protocol.filter;

import com.tinkerpop.rexster.protocol.server.IncompleteRexProRequestException;
import com.tinkerpop.rexster.protocol.server.RexProRequest;
import com.tinkerpop.rexster.server.RexsterApplication;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;
import org.glassfish.grizzly.memory.MemoryManager;

import java.io.IOException;

/**
 * Handles incoming/outgoing RexProMessage instances.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 * @author Blake Eggleston (bdeggleston.github.com)
 */
public class RexProServerFilter extends BaseFilter {
    private static final Logger logger = Logger.getLogger(RexProServerFilter.class);
    private RexsterApplication rexsterApplication;


    public RexProServerFilter(final RexsterApplication application) {
        rexsterApplication = application;
    }

    public NextAction handleRead(final FilterChainContext ctx) throws IOException {
        // Get the source buffer from the context
        final Buffer sourceBuffer = ctx.getMessage();
        final int sourceBufferLength = sourceBuffer.remaining();

        // If source buffer doesn't contain version byte
        if (sourceBufferLength < 1) {
            // stop the filterchain processing and store sourceBuffer to be
            // used next time
            return ctx.getStopAction(sourceBuffer);
        }

        RexProRequest request = null;
        final byte messageVersion = sourceBuffer.get(0);
        try {
            switch (messageVersion) {
                case 1:
                    request = new RexProRequest(sourceBuffer.toByteBuffer(), sourceBufferLength, rexsterApplication);
                    break;
                default:
                    //unexpected rexpro version
                    logger.warn("unsupported rexpro version: " + messageVersion);
                    return ctx.getStopAction();
            }
        } catch (IncompleteRexProRequestException ex) {
            // If the source message doesn't contain entire body
            // stop the filterchain processing and store sourceBuffer to be
            // used next time
            logger.warn(ex);
            return ctx.getStopAction(sourceBuffer);
        }

        // Check if the source buffer has more than 1 complete message
        // If yes - split up the first message and the remainder
        final Buffer remainder = sourceBufferLength > request.getCompleteRequestMessageLength() ?
                sourceBuffer.split(request.getCompleteRequestMessageLength()) : null;

        if (logger.isDebugEnabled()) {
            final StringBuilder sb = new StringBuilder();
            for (byte b : request.getRequestMessageBytes()) {
                sb.append(StringUtils.rightPad(Byte.toString(b), 4));
                sb.append(" ");
            }

            logger.debug(String.format("Received message [version:%s][message type:%s][body length:%s][body:%s]",
                    messageVersion, request.getRequestMessageType(), request.getRequestBodyLength(), sb.toString().trim()));
        }

        ctx.setMessage(request);
        sourceBuffer.tryDispose();
        return ctx.getInvokeAction(remainder);
    }

    public NextAction handleWrite(final FilterChainContext ctx) throws IOException {
        RexProRequest request = ctx.getMessage();

        // Retrieve the memory manager
        final MemoryManager memoryManager =
                ctx.getConnection().getTransport().getMemoryManager();

        // Write the response to the buffer
        final Buffer bb = memoryManager.allocate(request.getResponseSize());
        request.writeToBuffer(bb);

        // Allow Grizzly core to dispose the buffer, once it's written
        bb.allowBufferDispose(true);

        // Set the Buffer as a context message
        ctx.setMessage(bb.flip());

        // Instruct the FilterChain to call the next filter
        return ctx.getInvokeAction();
    }
}