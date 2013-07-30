package com.tinkerpop.rexster.protocol.server;

import com.tinkerpop.rexster.gremlin.converter.ConsoleResultConverter;
import com.tinkerpop.rexster.protocol.msg.ErrorResponseMessage;
import com.tinkerpop.rexster.protocol.msg.MessageTokens;
import com.tinkerpop.rexster.protocol.msg.MessageType;
import com.tinkerpop.rexster.protocol.msg.MessageUtil;
import com.tinkerpop.rexster.protocol.msg.RexProBindings;
import com.tinkerpop.rexster.protocol.msg.RexProMessage;
import com.tinkerpop.rexster.protocol.msg.RexProMessageMeta;
import com.tinkerpop.rexster.protocol.msg.RexProScriptResult;
import com.tinkerpop.rexster.protocol.msg.ScriptRequestMessage;
import com.tinkerpop.rexster.protocol.msg.ScriptResponseMessage;
import com.tinkerpop.rexster.protocol.msg.SessionRequestMessage;
import com.tinkerpop.rexster.protocol.msg.SessionResponseMessage;
import com.tinkerpop.rexster.protocol.serializer.RexProSerializer;
import com.tinkerpop.rexster.protocol.serializer.json.JSONSerializer;
import com.tinkerpop.rexster.protocol.serializer.msgpack.MsgPackSerializer;
import com.tinkerpop.rexster.protocol.serializer.msgpack.templates.MetaTemplate;
import com.tinkerpop.rexster.protocol.serializer.msgpack.templates.ResultsTemplate;
import com.tinkerpop.rexster.protocol.session.RexProSession;
import com.tinkerpop.rexster.server.RexsterApplication;
import org.apache.log4j.Logger;
import org.glassfish.grizzly.Buffer;
import org.msgpack.MessagePack;

import java.io.IOException;
import java.io.StringWriter;
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
        msgpack.register(RexProMessageMeta.class, MetaTemplate.getInstance());
        msgpack.register(RexProBindings.class, RexProBindings.SerializationTemplate.getInstance());
        msgpack.register(RexProScriptResult.class, ResultsTemplate.getInstance());
    }

    private ByteBuffer requestBuffer;
    private RexsterApplication rexsterApplication;

    private final byte serializerType;
    private final byte messageType;
    private final int bodyLength;
    private final int completeMessageLength;

    //the raw bytes of the request message
    //does not include message envelope
    private final byte[] requestBytes;
    private RexProMessage requestMessage = null;

    //version byte
    //serializer byte
    //reserved byte (4x)
    //message type byte
    //body length int
    private static int ENVELOPE_LENGTH = 1 + 1 + 4 + 1 + 4;

    private RexProSession session = null;

    private RexProMessage responseMessage = null;

    //the raw bytes of the response message
    //does not include message envelope
    private byte[] responseBytes = null;

    private static byte BYTE_VERSION = 1;
    private static byte[] BYTES_RESERVED = new byte[] {0, 0, 0, 0};


    public RexProRequest(ByteBuffer buffer, int bufferSize, RexsterApplication application) throws IncompleteRexProRequestException {
        requestBuffer = buffer;
        rexsterApplication = application;

        if (bufferSize < ENVELOPE_LENGTH) {
            throw new IncompleteRexProRequestException();
        }

        serializerType = requestBuffer.get(1);

        //bytes 2,3,4,5 are reserved for future use

        messageType = requestBuffer.get(6);
        bodyLength = requestBuffer.getInt(7);
        completeMessageLength = ENVELOPE_LENGTH + bodyLength;

        // If the source message doesn't contain entire body
        if (bufferSize < completeMessageLength) {
            throw new IncompleteRexProRequestException(String.format("Message envelope is incomplete. Message length set to %s but the buffer only contains %s", completeMessageLength, bufferSize));
        }

        if (bodyLength < 0) {
            throw new IncompleteRexProRequestException(String.format("Message body length in the envelope is negative: %s.", completeMessageLength));
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

    public RexProMessage getRequestMessage() {
        return requestMessage;
    }

    private static MsgPackSerializer msgPackSerializer = new MsgPackSerializer();
    private static JSONSerializer jsonSerializer = new JSONSerializer();
    protected RexProSerializer getSerializer() {
        if (serializerType == jsonSerializer.getSerializerId()){
            return jsonSerializer;
        } else {
            return msgPackSerializer;
        }
    }

    private void deserializeMessage() throws IOException{
        try {
            if (messageType == MessageType.SCRIPT_REQUEST) {
                requestMessage = getSerializer().deserialize(requestBytes, ScriptRequestMessage.class);
            } else if (messageType == MessageType.SESSION_REQUEST) {
                requestMessage = getSerializer().deserialize(requestBytes, SessionRequestMessage.class);
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

        }
    }

    public void process() {
        // may have been processed by the security filter already.
        if (requestMessage != null)  return;

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

        ScriptRequestMessage message = ((ScriptRequestMessage) requestMessage);

        if (message.metaGetConsole()) {
            writeResponseMessage(formatForConsoleChannel(message, session, result));
        } else {
            writeResponseMessage(formatForMsgPackChannel(message, session, result));
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
        try {
            RexProSerializer serializer = getSerializer();
            if (responseMessage instanceof SessionResponseMessage) {
                responseBytes = serializer.serialize((SessionResponseMessage) responseMessage, SessionResponseMessage.class);
            } else if (responseMessage instanceof ErrorResponseMessage) {
                responseBytes = serializer.serialize((ErrorResponseMessage) responseMessage, ErrorResponseMessage.class);
            } else if (responseMessage instanceof ScriptResponseMessage) {
                responseBytes = serializer.serialize((ScriptResponseMessage) responseMessage, ScriptResponseMessage.class);
            } else {
                throw new Exception();
            }

        } catch (Exception ex) {
            // if there's an error during serialization with msgpack this could tank.  the script will already
            // have executed and likely committed with success.  just means the response won't get back cleanly
            // to the client.
            ErrorResponseMessage errorMsg = MessageUtil.createErrorResponse(
                responseMessage.Request,
                responseMessage.Session,
                ErrorResponseMessage.RESULT_SERIALIZATION_ERROR,
                MessageTokens.ERROR_RESULT_SERIALIZATION
            );

            try {
                responseBytes = getSerializer().serialize(errorMsg, ErrorResponseMessage.class);
                responseMessage = errorMsg;
            } catch (IOException ex2) {
                logger.error(String.format(
                    "Could not serialize error message for request [%s] session [%s].  Should have reported flag [%s] message [%s] to client",
                    errorMsg.requestAsUUID(),
                    errorMsg.sessionAsUUID(),
                    errorMsg.metaGetFlag(),
                    errorMsg.ErrorMessage
                ));
            }

        }
    }

    public int getResponseSize() {
        return ENVELOPE_LENGTH + responseBytes.length;
    }

    public void writeToBuffer(final Buffer bb) {
        //add version
        bb.put(BYTE_VERSION);

        //add serializer
        bb.put(serializerType);

        //add reserved bytes
        bb.put(BYTES_RESERVED);

        if (responseMessage instanceof SessionResponseMessage) {
            bb.put(MessageType.SESSION_RESPONSE);
        } else if (responseMessage instanceof ErrorResponseMessage) {
            bb.put(MessageType.ERROR);
        } else if (responseMessage instanceof ScriptResponseMessage) {
            bb.put(MessageType.SCRIPT_RESPONSE);
        }

        bb.putInt(responseBytes.length);
        bb.put(responseBytes);
    }

    private static ScriptResponseMessage formatForMsgPackChannel(final ScriptRequestMessage specificMessage, final RexProSession session, final Object result) throws Exception {
        final ScriptResponseMessage msgPackScriptResponseMessage = new ScriptResponseMessage();

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

    public static List<String> convertResultToConsoleLines(final Object result) throws Exception {
        final ConsoleResultConverter converter = new ConsoleResultConverter(new StringWriter());
        return converter.convert(result);
    }

    private static ScriptResponseMessage formatForConsoleChannel(final ScriptRequestMessage specificMessage, final RexProSession session, final Object result) throws Exception {
        final ScriptResponseMessage consoleScriptResponseMessage = new ScriptResponseMessage();

        if (specificMessage.metaGetInSession()){
            consoleScriptResponseMessage.Session = specificMessage.Session;
        } else {
            consoleScriptResponseMessage.setSessionAsUUID(RexProMessage.EMPTY_SESSION);
        }

        consoleScriptResponseMessage.Request = specificMessage.Request;

        final List<String> consoleLines = convertResultToConsoleLines(result);
        consoleScriptResponseMessage.Results.set(consoleLines);
        if (session != null) {
            consoleScriptResponseMessage.Bindings.putAll(session.getBindings());
        }
        consoleScriptResponseMessage.validateMetaData();
        return consoleScriptResponseMessage;
    }

}
