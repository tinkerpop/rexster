package com.tinkerpop.rexster.protocol.msg;

import com.tinkerpop.rexster.client.RexProException;
import com.tinkerpop.rexster.protocol.BitWorks;

import java.util.UUID;

/**
 * A basic RexProMessage containing the basic components of every message that Rexster can process.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 * @author Blake Eggleston (bdeggleston.github.com)
 */
public abstract class RexProMessage {

    public static final int MESSAGE_HEADER_SIZE = 6;

    /**
     * Constant that represents the size of a RexProMessage.
     */
    protected static final int BASE_MESSAGE_SIZE = 32;

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
     * @return the estimated size of the message in bytes.
     */
    public abstract int estimateMessageSize();

    /**
     * Validates the instance's Meta field
     */
    public void validateMetaData() throws RexProException{
        for (RexProMessageMetaField f : getMetaFields()) {
            f.validateMeta(Meta);
        }
    }

}
