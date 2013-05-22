package com.tinkerpop.rexster.protocol.server;

import com.tinkerpop.rexster.protocol.RexProSession;
import com.tinkerpop.rexster.protocol.msg.*;
import com.tinkerpop.rexster.server.RexsterApplication;
import org.apache.log4j.Logger;

import org.glassfish.grizzly.Buffer;
import org.msgpack.MessagePack;
import org.msgpack.packer.Packer;
import org.msgpack.unpacker.Unpacker;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

/**
 * Contains logic for deserializing and executing
 * rexpro requests, and serializing the results
 *
 * @author Blake Eggleston (bdeggleston.github.com)
 */
public class RexProRequest {
    private static final Logger logger = Logger.getLogger(RexProRequest.class);
    private static final MessagePack msgpack = new MessagePack();
    static {
        msgpack.register(RexProMessageMeta.class, RexProMessageMeta.SerializationTemplate.getInstance());
        msgpack.register(RexProBindings.class, RexProBindings.SerializationTemplate.getInstance());
        msgpack.register(RexProScriptResult.class, RexProScriptResult.SerializationTemplate.getInstance());
    }

    private ByteBuffer requestBuffer;
    private RexsterApplication rexsterApplication;

    private final byte messageType;
    private final int bodyLength;
    private final int completeMessageLength;

    //the raw bytes of the request message
    //does not include message envelope
    private final byte[] requestBytes;
    private RexProMessage requestMessage = null;

    //version byte
    //message type byte
    //body length int
    private static int ENVELOPE_LENGTH = 1 + 1 + 4;

    private RexProSession session = null;

    private RexProMessage responseMessage = null;

    //the raw bytes of the response message
    //does not include message envelope
    private byte[] responseBytes = null;


    public RexProRequest(ByteBuffer buffer, int bufferSize, RexsterApplication application) throws IncompleteRexProRequest {
        requestBuffer = buffer;
        rexsterApplication = application;

        if (bufferSize < ENVELOPE_LENGTH) {
            throw new IncompleteRexProRequest();
        }

        messageType = requestBuffer.get(1);
        bodyLength = requestBuffer.getInt(2);
        completeMessageLength = ENVELOPE_LENGTH + bodyLength;

        // If the source message doesn't contain entire body
        if (bufferSize < completeMessageLength) {
            throw new IncompleteRexProRequest(String.format("Message envelope is incomplete. Message length set to %s but the buffer only contains %s", completeMessageLength, bufferSize));
        }

        if (bodyLength < 0) {
            throw new IncompleteRexProRequest(String.format("Message body length in the envelope is negative: %s.", completeMessageLength));
        }

        //get the actual message requestBytes
        requestBytes = new byte[bodyLength];
        buffer.position(ENVELOPE_LENGTH);
        buffer.get(requestBytes);
    }

    public int getCompleteRequestMessageLength() {
        return completeMessageLength;
    }

    public byte getRequestMessageType() {
        return messageType;
    }

    public byte[] getRequestMessageBytes() {
        return requestBytes;
    }

    public int getRequestBodyLength() {
        return bodyLength;
    }

    public void setSession(RexProSession session) {
        this.session = session;
    }

    private void deserializeMessage() throws IOException{
        final ByteArrayInputStream in = new ByteArrayInputStream(requestBytes);
        final Unpacker unpacker = msgpack.createUnpacker(in);

        try {
            unpacker.setArraySizeLimit(Integer.MAX_VALUE);
            unpacker.setMapSizeLimit(Integer.MAX_VALUE);
            unpacker.setRawSizeLimit(Integer.MAX_VALUE);

            if (messageType == MessageType.SCRIPT_REQUEST) {
                requestMessage = unpacker.read(ScriptRequestMessage.class);
            } else if (messageType == MessageType.SESSION_REQUEST) {
                requestMessage = unpacker.read(SessionRequestMessage.class);
            }

            if (requestMessage == null) {
                logger.warn(String.format("Message did not match the specified type [%s]", messageType));
                writeResponseMessage(
                    MessageUtil.createErrorResponse(
                        RexProMessage.EMPTY_REQUEST_AS_BYTES,
                        RexProMessage.EMPTY_SESSION_AS_BYTES,
                        ErrorResponseMessage.INVALID_MESSAGE_ERROR,
                        MessageTokens.ERROR_UNEXPECTED_MESSAGE_TYPE
                    )
                );
            }

        } catch (Exception ex) {
            logger.error(String.format("Error during message deserialization of a message of type [%s].", messageType), ex);
            writeResponseMessage(
                MessageUtil.createErrorResponse(
                    RexProMessage.EMPTY_REQUEST_AS_BYTES,
                    RexProMessage.EMPTY_SESSION_AS_BYTES,
                    ErrorResponseMessage.INVALID_MESSAGE_ERROR,
                    ex.toString()
                )
            );

        } finally {
            unpacker.close();
        }
    }

    public void process() {
        try {
            deserializeMessage();
        } catch (Exception ex) {
            logger.warn("error deserializing message: " + ex.toString());
        }

        //did the deserializer set the response message?
        //(probably an error response), if so, return early
        if (responseMessage != null) return;

        try {
            if (requestMessage instanceof SessionRequestMessage) {
                SessionServer sessionServer = new SessionServer(rexsterApplication);
                sessionServer.handleRequest((SessionRequestMessage) requestMessage, this);

            } else if (requestMessage instanceof ScriptRequestMessage) {
                ScriptServer scriptServer = new ScriptServer(rexsterApplication);
                scriptServer.handleRequest((ScriptRequestMessage) requestMessage, this);
            }

        } catch (Exception ex) {
            logger.warn("error processing rexpro request: " + ex.toString());
            try {
                //try to send an error message
                if (responseMessage == null) {
                    writeResponseMessage(
                        MessageUtil.createErrorResponse(
                            requestMessage.Request,
                            requestMessage.Session,
                            ErrorResponseMessage.UNKNOWN_ERROR,
                            ex.toString()
                        )
                    );
                }
            } catch (IOException ex2) {
                //don't do anything
            }
        }

        //TODO: do something if the response message hasn't been set yet
    }

    /**
     * Writes the result of a script request to it's message
     * type and serializes the result immediately
     *
     * @param result
     */
    public void writeScriptResults(Object result) throws Exception {

        int channel;
        ScriptRequestMessage message = ((ScriptRequestMessage) requestMessage);
        if (session != null) {
            channel = session.getChannel();
        } else {
            channel = message.metaGetChannel();
        }

        if (session.getChannel() == RexProChannel.CHANNEL_CONSOLE) {
            responseMessage = formatForConsoleChannel(message, session, result);

        } else if (session.getChannel() == RexProChannel.CHANNEL_MSGPACK) {
            responseMessage = formatForMsgPackChannel(message, session, result);

        } else if (session.getChannel() == RexProChannel.CHANNEL_GRAPHSON) {
            responseMessage = formatForGraphSONChannel(message, session, result);
        } else {
            // malformed channel???!!!
            logger.warn(String.format("Session is configured for a channel that does not exist: [%s]", session.getChannel()));
        }

    }

    /**
     * Writes a response message
     *
     * @param response
     */
    public void writeResponseMessage(RexProMessage response) throws IOException {
        responseMessage = response;
        serializeMessage();
    }

    private void serializeMessage() throws IOException {
        final ByteArrayOutputStream rexProMessageStream = new ByteArrayOutputStream();
        //TODO: create RexProMessageMeta template
        final Packer packer = msgpack.createPacker(rexProMessageStream);

        try {
            packer.write(responseMessage);
            responseBytes = rexProMessageStream.toByteArray();
        } catch (Exception ex) {
            // if there's an error during serialization with msgpack this could tank.  the script will already
            // have executed and likely committed with success.  just means the response won't get back cleanly
            // to the client.
            final ByteArrayOutputStream rpms = new ByteArrayOutputStream();
            final Packer p = msgpack.createPacker(rpms);

            ErrorResponseMessage errorMsg = MessageUtil.createErrorResponse(
                responseMessage.Request,
                responseMessage.Session,
                ErrorResponseMessage.RESULT_SERIALIZATION_ERROR,
                MessageTokens.ERROR_RESULT_SERIALIZATION
            );

            try {
                p.write(errorMsg);
            } catch (IOException ex2) {
                logger.error(String.format(
                    "Could not serialize error message for request [%s] session [%s].  Should have reported flag [%s] message [%s] to client",
                    errorMsg.requestAsUUID(),
                    errorMsg.sessionAsUUID(),
                    errorMsg.metaGetFlag(),
                    errorMsg.ErrorMessage
                ));
            }
            responseBytes = rpms.toByteArray();
            responseMessage = errorMsg;

        } finally {
            packer.close();
        }

    }

    public int getResponseSize() {
        return ENVELOPE_LENGTH + responseBytes.length;
    }

    public void writeToBuffer(Buffer bb) {
        //add version
        bb.put((byte) 0);

        if (responseMessage instanceof SessionResponseMessage) {
            bb.put(MessageType.SESSION_RESPONSE);
        } else if (responseMessage instanceof ConsoleScriptResponseMessage) {
            bb.put(MessageType.CONSOLE_SCRIPT_RESPONSE);
        } else if (responseMessage instanceof ErrorResponseMessage) {
            bb.put(MessageType.ERROR);
        } else if (responseMessage instanceof MsgPackScriptResponseMessage) {
            bb.put(MessageType.MSGPACK_SCRIPT_RESPONSE);
        }  else if (responseMessage instanceof GraphSONScriptResponseMessage) {
            bb.put(MessageType.GRAPHSON_SCRIPT_RESPONSE);
        }

        bb.putInt(responseBytes.length);
        bb.put(responseBytes);
    }

    private static GraphSONScriptResponseMessage formatForGraphSONChannel(final ScriptRequestMessage specificMessage, final RexProSession session, final Object result) throws Exception {
        final GraphSONScriptResponseMessage graphSONScriptResponseMessage = new GraphSONScriptResponseMessage();

        if (specificMessage.metaGetInSession()){
            graphSONScriptResponseMessage.Session = specificMessage.Session;
        } else {
            graphSONScriptResponseMessage.setSessionAsUUID(RexProMessage.EMPTY_SESSION);
        }

        graphSONScriptResponseMessage.Request = specificMessage.Request;
        graphSONScriptResponseMessage.Results = GraphSONScriptResponseMessage.convertResultToBytes(result);
        if (session != null){
            graphSONScriptResponseMessage.Bindings.putAll(session.getBindings());
        }
        graphSONScriptResponseMessage.validateMetaData();
        return graphSONScriptResponseMessage;
    }

    private static MsgPackScriptResponseMessage formatForMsgPackChannel(final ScriptRequestMessage specificMessage, final RexProSession session, final Object result) throws Exception {
        final MsgPackScriptResponseMessage msgPackScriptResponseMessage = new MsgPackScriptResponseMessage();

        if (specificMessage.metaGetInSession()){
            msgPackScriptResponseMessage.Session = specificMessage.Session;
        } else {
            msgPackScriptResponseMessage.setSessionAsUUID(RexProMessage.EMPTY_SESSION);
        }

        msgPackScriptResponseMessage.Request = specificMessage.Request;
        msgPackScriptResponseMessage.Results.set(result);
        if (session != null){
            msgPackScriptResponseMessage.Bindings.putAll(session.getBindings());
        }
        msgPackScriptResponseMessage.validateMetaData();
        return msgPackScriptResponseMessage;
    }

    private static ConsoleScriptResponseMessage formatForConsoleChannel(final ScriptRequestMessage specificMessage, final RexProSession session, final Object result) throws Exception {
        final ConsoleScriptResponseMessage consoleScriptResponseMessage = new ConsoleScriptResponseMessage();

        if (specificMessage.metaGetInSession()){
            consoleScriptResponseMessage.Session = specificMessage.Session;
        } else {
            consoleScriptResponseMessage.setSessionAsUUID(RexProMessage.EMPTY_SESSION);
        }

        consoleScriptResponseMessage.Request = specificMessage.Request;

        final List<String> consoleLines = ConsoleScriptResponseMessage.convertResultToConsoleLines(result);
        consoleScriptResponseMessage.ConsoleLines = new String[consoleLines.size()];
        consoleLines.toArray(consoleScriptResponseMessage.ConsoleLines);
        if (session != null) {
            consoleScriptResponseMessage.Bindings.putAll(session.getBindings());
        }
        consoleScriptResponseMessage.validateMetaData();
        return consoleScriptResponseMessage;
    }

}
