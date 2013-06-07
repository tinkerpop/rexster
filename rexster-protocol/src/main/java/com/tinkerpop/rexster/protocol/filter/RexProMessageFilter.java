package com.tinkerpop.rexster.protocol.filter;

import com.tinkerpop.rexster.protocol.msg.ConsoleScriptResponseMessage;
import com.tinkerpop.rexster.protocol.msg.ErrorResponseMessage;
import com.tinkerpop.rexster.protocol.msg.GraphSONScriptResponseMessage;
import com.tinkerpop.rexster.protocol.msg.MessageTokens;
import com.tinkerpop.rexster.protocol.msg.MessageType;
import com.tinkerpop.rexster.protocol.msg.MessageUtil;
import com.tinkerpop.rexster.protocol.msg.MsgPackScriptResponseMessage;
import com.tinkerpop.rexster.protocol.msg.RexProBindings;
import com.tinkerpop.rexster.protocol.msg.RexProMessage;
import com.tinkerpop.rexster.protocol.msg.RexProMessageMeta;
import com.tinkerpop.rexster.protocol.msg.RexProScriptResult;
import com.tinkerpop.rexster.protocol.msg.ScriptRequestMessage;
import com.tinkerpop.rexster.protocol.msg.SessionRequestMessage;
import com.tinkerpop.rexster.protocol.msg.SessionResponseMessage;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;
import org.glassfish.grizzly.memory.MemoryManager;
import org.msgpack.MessagePack;
import org.msgpack.packer.Packer;
import org.msgpack.unpacker.Unpacker;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Handles incoming/outgoing RexProMessage instances.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class RexProMessageFilter extends BaseFilter {
    private static final Logger logger = Logger.getLogger(RexProMessageFilter.class);
    private static final MessagePack msgpack = new MessagePack();
    static {
        msgpack.register(RexProMessageMeta.class, RexProMessageMeta.SerializationTemplate.getInstance());
        msgpack.register(RexProBindings.class, RexProBindings.SerializationTemplate.getInstance());
        msgpack.register(RexProScriptResult.class, RexProScriptResult.SerializationTemplate.getInstance());
    }

    public NextAction handleRead(final FilterChainContext ctx) throws IOException {
        // Get the source buffer from the context
        final Buffer sourceBuffer = ctx.getMessage();
        final int sourceBufferLength = sourceBuffer.remaining();

        // If source buffer doesn't contain header
        if (sourceBufferLength < RexProMessage.MESSAGE_HEADER_SIZE) {
            // stop the filterchain processing and store sourceBuffer to be
            // used next time
            return ctx.getStopAction(sourceBuffer);
        }

        final byte messageVersion = sourceBuffer.get(0);
        final byte messageType = sourceBuffer.get(1);
        final int bodyLength = sourceBuffer.getInt(2);
        final int completeMessageLength = RexProMessage.MESSAGE_HEADER_SIZE + bodyLength;

        //check message version
        if (messageVersion != 0) {
            logger.warn("unsupported rexpro version: " + messageVersion);
            return ctx.getStopAction();
        }

        // If the source message doesn't contain entire body
        if (sourceBufferLength < completeMessageLength) {
            // stop the filterchain processing and store sourceBuffer to be
            // used next time
            logger.warn(String.format("Message envelope is incomplete. Message length set to %s but the buffer only contains %s", completeMessageLength, sourceBufferLength));
            return ctx.getStopAction(sourceBuffer);
        }

        if (completeMessageLength < 0) {
            // the message length can never be negative
            logger.warn(String.format("Message body length in the envelope is negative: %s.", completeMessageLength));
            return ctx.getStopAction(sourceBuffer);
        }

        // Check if the source buffer has more than 1 complete message
        // If yes - split up the first message and the remainder
        final Buffer remainder = sourceBufferLength > completeMessageLength ?
                sourceBuffer.split(completeMessageLength) : null;

        byte[] messageAsBytes = new byte[bodyLength];
        sourceBuffer.position(RexProMessage.MESSAGE_HEADER_SIZE);
        sourceBuffer.get(messageAsBytes);

        if (logger.isDebugEnabled()) {
            final StringBuilder sb = new StringBuilder();
            for (byte b : messageAsBytes) {
                sb.append(StringUtils.rightPad(Byte.toString(b), 4));
                sb.append(" ");
            }

            logger.debug(String.format("Received message [version:%s][message type:%s][body length:%s][body:%s]",
                    messageVersion, messageType, bodyLength, sb.toString().trim()));
        }

        final ByteArrayInputStream in = new ByteArrayInputStream(messageAsBytes);
        final Unpacker unpacker = msgpack.createUnpacker(in);

        try {
            unpacker.setArraySizeLimit(Integer.MAX_VALUE);
            unpacker.setMapSizeLimit(Integer.MAX_VALUE);
            unpacker.setRawSizeLimit(Integer.MAX_VALUE);

            RexProMessage message = null;
            if (messageType == MessageType.SCRIPT_REQUEST) {
                message = unpacker.read(ScriptRequestMessage.class);
            } else if (messageType == MessageType.SESSION_REQUEST) {
                message = unpacker.read(SessionRequestMessage.class);
            } else if (messageType == MessageType.CONSOLE_SCRIPT_RESPONSE) {
                message = unpacker.read(ConsoleScriptResponseMessage.class);
            } else if (messageType == MessageType.SESSION_RESPONSE) {
                message = unpacker.read(SessionResponseMessage.class);
            } else if (messageType == MessageType.ERROR) {
                message = unpacker.read(ErrorResponseMessage.class);
            } else if (messageType == MessageType.MSGPACK_SCRIPT_RESPONSE) {
                message = unpacker.read(MsgPackScriptResponseMessage.class);
            } else if (messageType == MessageType.GRAPHSON_SCRIPT_RESPONSE) {
                message = unpacker.read(GraphSONScriptResponseMessage.class);
            }

            if (message == null) {
                logger.warn(String.format("Message did not match the specified type [%s]", messageType));

                ctx.write(
                    MessageUtil.createErrorResponse(
                            RexProMessage.EMPTY_REQUEST_AS_BYTES,
                            RexProMessage.EMPTY_SESSION_AS_BYTES,
                            ErrorResponseMessage.INVALID_MESSAGE_ERROR,
                            MessageTokens.ERROR_UNEXPECTED_MESSAGE_TYPE
                    )
                );
                return ctx.getStopAction();
            }

            ctx.setMessage(message);

            sourceBuffer.tryDispose();

            return ctx.getInvokeAction(remainder);
        } catch (Exception ex) {
            logger.error(String.format("Error during message deserialization of a message of type [%s].", messageType), ex);

            final ErrorResponseMessage deserializationMessage = MessageUtil.createErrorResponse(
                    RexProMessage.EMPTY_REQUEST_AS_BYTES,
                    RexProMessage.EMPTY_SESSION_AS_BYTES,
                    ErrorResponseMessage.INVALID_MESSAGE_ERROR,
                    ex.toString());

            try {
                ctx.write(deserializationMessage);
            } catch (Exception inner) {
                logger.error(String.format(
                        "Could not write error message back to client for request [%s] session [%s].  Should have reported flag [%s] message [%s] to client",
                        deserializationMessage.requestAsUUID(),
                        deserializationMessage.sessionAsUUID(),
                        deserializationMessage.metaGetFlag(),
                        deserializationMessage.ErrorMessage));
            }

            return ctx.getStopAction();
        } finally {
            unpacker.close();
        }
    }

    public NextAction handleWrite(final FilterChainContext ctx) throws IOException {

        Object rawMsg = ctx.getMessage();
        if(rawMsg instanceof byte[]) {
            byte[] bytes = (byte[]) rawMsg;

            // Retrieve the memory manager
            final MemoryManager memoryManager = ctx.getConnection().getTransport().getMemoryManager();
            final Buffer bb = memoryManager.allocate(bytes.length);
            bb.put(bytes);

            // Set the Buffer as a context message
            ctx.setMessage(bb.flip());
            // Instruct the FilterChain to call the next filter
            return ctx.getInvokeAction();
        }

        // Get the source message to be written
        RexProMessage msg = (RexProMessage) rawMsg;

        final ByteArrayOutputStream rexProMessageStream = new ByteArrayOutputStream();
        //TODO: create RexProMessageMeta template
        final Packer packer = msgpack.createPacker(rexProMessageStream);
        byte[] rexProMessageAsBytes = new byte[0];

        try {
            packer.write(msg);
            rexProMessageAsBytes = rexProMessageStream.toByteArray();
        } catch (Exception ex) {
            // if there's an error during serialization with msgpack this could tank.  the script will already
            // have executed and likely committed with success.  just means the response won't get back cleanly
            // to the client.
            final ByteArrayOutputStream rpms = new ByteArrayOutputStream();
            final Packer p = msgpack.createPacker(rpms);
            ErrorResponseMessage errorMsg = MessageUtil.createErrorResponse(msg.Request, msg.Session,
                    ErrorResponseMessage.RESULT_SERIALIZATION_ERROR,
                    MessageTokens.ERROR_RESULT_SERIALIZATION);
            p.write(errorMsg);
            rexProMessageAsBytes = rpms.toByteArray();

            msg = errorMsg;

        } finally {
            packer.close();
        }

        // Retrieve the memory manager
        final MemoryManager memoryManager =
                ctx.getConnection().getTransport().getMemoryManager();
        final Buffer bb = memoryManager.allocate(RexProMessage.MESSAGE_HEADER_SIZE + rexProMessageAsBytes.length);

        //add version
        bb.put((byte) 0);

        if (msg instanceof SessionResponseMessage) {
            bb.put(MessageType.SESSION_RESPONSE);
        } else if (msg instanceof ConsoleScriptResponseMessage) {
            bb.put(MessageType.CONSOLE_SCRIPT_RESPONSE);
        } else if (msg instanceof ErrorResponseMessage) {
            bb.put(MessageType.ERROR);
        } else if (msg instanceof ScriptRequestMessage) {
            bb.put(MessageType.SCRIPT_REQUEST);
        } else if (msg instanceof SessionRequestMessage) {
            bb.put(MessageType.SESSION_REQUEST);
        } else if (msg instanceof MsgPackScriptResponseMessage) {
            bb.put(MessageType.MSGPACK_SCRIPT_RESPONSE);
        }  else if (msg instanceof GraphSONScriptResponseMessage) {
            bb.put(MessageType.GRAPHSON_SCRIPT_RESPONSE);
        }

        bb.putInt(rexProMessageAsBytes.length);
        bb.put(rexProMessageAsBytes);

        // Allow Grizzly core to dispose the buffer, once it's written
        bb.allowBufferDispose(true);

        // Set the Buffer as a context message
        ctx.setMessage(bb.flip());

        // Instruct the FilterChain to call the next filter
        return ctx.getInvokeAction();
    }
}