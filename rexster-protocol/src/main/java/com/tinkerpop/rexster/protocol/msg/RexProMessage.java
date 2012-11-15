package com.tinkerpop.rexster.protocol.msg;

import com.tinkerpop.rexster.protocol.BitWorks;

import java.util.UUID;

/**
 * A basic RexProMessage containing the basic components of every message that Rexster can process.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class RexProMessage {

    /**
     * The standard value for an empty session.
     */
    public static final UUID EMPTY_SESSION = new UUID(0, 0);
    public static final byte[] EMPTY_SESSION_AS_BYTES = BitWorks.convertUUIDToByteArray(EMPTY_SESSION);

    public static final UUID EMPTY_REQUEST = new UUID(0, 0);
    public static final byte[] EMPTY_REQUEST_AS_BYTES = BitWorks.convertUUIDToByteArray(EMPTY_REQUEST);

    /**
     * Denotes the version of RexPro that is being used.
     */
    public byte Version = 0;

    /**
     * A value used to denote different states in different messages.  See specific message implementations
     * for how this field is used.
     */
    public byte Flag;

    /**
     * Denotes the session on which the message is sent. Reserved for 16 bytes and resolves to a UUID.
     */
    public byte[] Session;

    /**
     * A value that uniquely identifies a request. Reserved for 16 bytes and resolves to a UUID.
     */
    public byte[] Request;

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

    public int estimateSize() {
        return 16;
    }
}
