package com.tinkerpop.rexster.client;

import com.tinkerpop.rexster.protocol.msg.ErrorResponseMessage;
import com.tinkerpop.rexster.protocol.msg.MessageTokens;
import com.tinkerpop.rexster.protocol.msg.MessageType;
import com.tinkerpop.rexster.protocol.msg.MessageUtil;
import com.tinkerpop.rexster.protocol.msg.RexProMessage;
import com.tinkerpop.rexster.protocol.msg.ScriptRequestMessage;
import com.tinkerpop.rexster.protocol.msg.ScriptResponseMessage;
import com.tinkerpop.rexster.protocol.msg.SessionRequestMessage;
import com.tinkerpop.rexster.protocol.msg.SessionResponseMessage;
import com.tinkerpop.rexster.protocol.serializer.RexProSerializer;
import com.tinkerpop.rexster.protocol.serializer.json.JSONSerializer;
import com.tinkerpop.rexster.protocol.serializer.msgpack.MsgPackSerializer;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;
import org.glassfish.grizzly.memory.MemoryManager;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Handles incoming/outgoing RexProMessage instances.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class RexProClientFilter extends BaseFilter {
    private static final Logger logger = Logger.getLogger(RexProClientFilter.class);

    private static MsgPackSerializer msgPackSerializer = new MsgPackSerializer();
    private static JSONSerializer jsonSerializer = new JSONSerializer();

    //version byte
    //serializer byte
    //reserved byte (4x)
    //message type byte
    //body length int
    private static int ENVELOPE_LENGTH = 1 + 1 + 4 + 1 + 4;

    public NextAction handleRead(final FilterChainContext ctx) throws IOException {
        // Get the source buffer from the context
        final Buffer sourceBuffer = ctx.getMessage();
        final int sourceBufferLength = sourceBuffer.remaining();

        // If source buffer doesn't contain header
        if (sourceBufferLength < ENVELOPE_LENGTH) {
            // stop the filterchain processing and store sourceBuffer to be
            // used next time
            return ctx.getStopAction(sourceBuffer);
        }

        final byte messageVersion = sourceBuffer.get(0);
        final byte serializerType = sourceBuffer.get(1);
        //bytes 2,3,4,5 are reserved for future use
        final byte messageType = sourceBuffer.get(6);
        final int bodyLength = sourceBuffer.getInt(7);
        final int completeMessageLength = ENVELOPE_LENGTH + bodyLength;

        //check message version
        if (messageVersion != 1) {
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
        sourceBuffer.position(ENVELOPE_LENGTH);
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

        try {
            RexProSerializer serializer;
            if (serializerType == msgPackSerializer.getSerializerId()){
                serializer = msgPackSerializer;
            } else if (serializerType == jsonSerializer.getSerializerId()) {
                serializer = jsonSerializer;
            } else {
                throw new RexProException(String.format("unknown serializer type: %s", serializerType));
            }

            RexProMessage message = null;
            if (messageType == MessageType.SCRIPT_REQUEST) {
                message = serializer.deserialize(messageAsBytes, ScriptRequestMessage.class);
            } else if (messageType == MessageType.SESSION_REQUEST) {
                message = serializer.deserialize(messageAsBytes, SessionRequestMessage.class);
            } else if (messageType == MessageType.SESSION_RESPONSE) {
                message = serializer.deserialize(messageAsBytes, SessionResponseMessage.class);
            } else if (messageType == MessageType.ERROR) {
                message = serializer.deserialize(messageAsBytes, ErrorResponseMessage.class);
            } else if (messageType == MessageType.SCRIPT_RESPONSE) {
                message = serializer.deserialize(messageAsBytes, ScriptResponseMessage.class);
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
        }
    }

    public NextAction handleWrite(final FilterChainContext ctx) throws IOException {

        Object rawMsg = ctx.getMessage();

        RexsterClient.MessageContainer container = (RexsterClient.MessageContainer) rawMsg;
        // Get the source message to be written
        byte serializerType = container.getSerializer();
        RexProMessage msg = container.getMessage();

        byte[] rexProMessageAsBytes = new byte[0];

        try {
            RexProSerializer serializer;
            if (serializerType == msgPackSerializer.getSerializerId()){
                serializer = msgPackSerializer;
            } else if (serializerType == jsonSerializer.getSerializerId()) {
                serializer = jsonSerializer;
            } else {
                throw new RexProException(String.format("unknown serializer type: %s", serializerType));
            }

            if (msg instanceof SessionResponseMessage) {
                rexProMessageAsBytes = serializer.serialize((SessionResponseMessage) msg, SessionResponseMessage.class);
            } else if (msg instanceof ErrorResponseMessage) {
                rexProMessageAsBytes = serializer.serialize((ErrorResponseMessage) msg, ErrorResponseMessage.class);
            } else if (msg instanceof ScriptRequestMessage) {
                rexProMessageAsBytes = serializer.serialize((ScriptRequestMessage) msg, ScriptRequestMessage.class);
            } else if (msg instanceof SessionRequestMessage) {
                rexProMessageAsBytes = serializer.serialize((SessionRequestMessage) msg, SessionRequestMessage.class);
            } else if (msg instanceof ScriptResponseMessage) {
                rexProMessageAsBytes = serializer.serialize((ScriptResponseMessage) msg, ScriptResponseMessage.class);
            }

        } catch (Exception ex) {
            // if there's an error during serialization with msgpack this could tank.  the script will already
            // have executed and likely committed with success.  just means the response won't get back cleanly
            // to the client.
            final ByteArrayOutputStream rpms = new ByteArrayOutputStream();
            ErrorResponseMessage errorMsg = MessageUtil.createErrorResponse(msg.Request, msg.Session,
                    ErrorResponseMessage.RESULT_SERIALIZATION_ERROR,
                    MessageTokens.ERROR_RESULT_SERIALIZATION);
            rexProMessageAsBytes = rpms.toByteArray();

            ctx.setMessage(null);
            return ctx.getStopAction();

        }

        // Retrieve the memory manager
        final MemoryManager memoryManager =
                ctx.getConnection().getTransport().getMemoryManager();
        final Buffer bb = memoryManager.allocate(ENVELOPE_LENGTH + rexProMessageAsBytes.length);

        //add version
        bb.put((byte) 1);

        //add serializer
        bb.put(serializerType);

        //add reserved bytes
        bb.put((byte) 0);
        bb.put((byte) 0);
        bb.put((byte) 0);
        bb.put((byte) 0);

        if (msg instanceof SessionResponseMessage) {
            bb.put(MessageType.SESSION_RESPONSE);
        } else if (msg instanceof ErrorResponseMessage) {
            bb.put(MessageType.ERROR);
        } else if (msg instanceof ScriptRequestMessage) {
            bb.put(MessageType.SCRIPT_REQUEST);
        } else if (msg instanceof SessionRequestMessage) {
            bb.put(MessageType.SESSION_REQUEST);
        } else if (msg instanceof ScriptResponseMessage) {
            bb.put(MessageType.SCRIPT_RESPONSE);
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