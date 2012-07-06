package com.tinkerpop.rexster.protocol.msg;

import com.tinkerpop.rexster.protocol.BitWorks;

import java.util.UUID;

public class RexProMessage {
    public static final UUID EMPTY_SESSION = new UUID(0, 0);

    public byte Version;

    public byte Flag;

    public byte[] Session;

    public byte[] Request;

    public boolean hasSession() {
        return this.Session != null && !this.sessionAsUUID().equals(EMPTY_SESSION);
    }

    public UUID sessionAsUUID() {
        return BitWorks.convertByteArrayToUUID(this.Session);
    }

    public void setSessionAsUUID(UUID session) {
        this.Session = BitWorks.convertUUIDToByteArray(session);
    }

    public UUID requestAsUUID() {
        return BitWorks.convertByteArrayToUUID(this.Request);
    }

    public void setRequestAsUUID(UUID request) {
        this.Request = BitWorks.convertUUIDToByteArray(request);
    }
}
