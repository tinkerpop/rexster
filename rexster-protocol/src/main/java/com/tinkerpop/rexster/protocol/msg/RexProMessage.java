package com.tinkerpop.rexster.protocol.msg;

import com.tinkerpop.rexster.client.RexProException;
import com.tinkerpop.rexster.protocol.BitWorks;
import com.tinkerpop.rexster.protocol.serializer.msgpack.templates.MetaTemplate;
import com.tinkerpop.rexster.protocol.serializer.msgpack.templates.ResultsTemplate;
import org.msgpack.MessagePack;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * A basic RexProMessage containing the basic components of every message that Rexster can process.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 * @author Blake Eggleston (bdeggleston.github.com)
 */
public abstract class RexProMessage {

    public static final int MESSAGE_HEADER_SIZE = 6;

    private static final MessagePack msgpack = new MessagePack();
    static {
        //todo: replace with msgpack.templates.* template instances
        msgpack.register(RexProMessageMeta.class, MetaTemplate.getInstance());
        msgpack.register(RexProBindings.class, RexProBindings.SerializationTemplate.getInstance());
        msgpack.register(RexProScriptResult.class, ResultsTemplate.getInstance());
    }

    /**
     * List of meta fields accepted for this message type
     */
    protected RexProMessageMetaField[] getMetaFields() {
        RexProMessageMetaField[] fields = {};
        return fields;
    }

    /**
     * The standard value for an empty session.
     */
    public static final UUID EMPTY_SESSION = new UUID(0, 0);
    public static final byte[] EMPTY_SESSION_AS_BYTES = BitWorks.convertUUIDToByteArray(EMPTY_SESSION);

    public static final UUID EMPTY_REQUEST = new UUID(0, 0);
    public static final byte[] EMPTY_REQUEST_AS_BYTES = BitWorks.convertUUIDToByteArray(EMPTY_REQUEST);

    /**
     * Denotes the session on which the message is sent. Reserved for 16 bytes and resolves to a UUID.
     */
    public byte[] Session;

    /**
     * A value that uniquely identifies a request. Reserved for 16 bytes and resolves to a UUID.
     */
    public byte[] Request;

    /**
     * Map of message type specific meta data, supported keys and values vary by message type
     */
    public RexProMessageMeta Meta = new RexProMessageMeta();

    public boolean hasSession() {
        return this.Session != null && !this.sessionAsUUID().equals(EMPTY_SESSION);
    }

    public UUID sessionAsUUID() {
        return BitWorks.convertByteArrayToUUID(this.Session);
    }

    public void setSessionAsUUID(final UUID session) {
        this.Session = BitWorks.convertUUIDToByteArray(session);
    }

    public UUID requestAsUUID() {
        return BitWorks.convertByteArrayToUUID(this.Request);
    }

    public void setRequestAsUUID(final UUID request) {
        this.Request = BitWorks.convertUUIDToByteArray(request);
    }

    /**
     * Validates the instance's Meta field
     */
    public void validateMetaData() throws RexProException{
        for (RexProMessageMetaField f : getMetaFields()) {
            f.validateMeta(Meta);
        }
    }

    /**
     * Serializes the message into a byte array
     * @return
     */
    public static byte[] serialize(RexProMessage msg) throws IOException {
        byte[] message = msgpack.write(msg);
        ByteBuffer bb = ByteBuffer.allocate(MESSAGE_HEADER_SIZE + message.length);

        //version
        bb.put((byte)0);

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

        bb.putInt(message.length);
        bb.put(message);
        return bb.array();
    }
}
