package com.tinkerpop.rexster.protocol;

import org.glassfish.grizzly.Buffer;

import java.util.Arrays;
import java.util.UUID;

public class RexProMessage {

    public static final int HEADER_SIZE = 43;

    public static final byte CURRENT_VERSION = (byte) 0;

    protected static final UUID EMPTY_SESSION = new UUID(0, 0);

    private byte r;
    private byte e;
    private byte x;
    private byte p;

    private byte version;

    private byte type;

    private byte flag;

    private byte[] session;

    private byte[] request;

    private int bodyLength;

    private byte[] body;


    public RexProMessage() {
    }

    public RexProMessage(byte version, byte type, byte flag, byte[] session, byte[] request, byte[] body) {
        this.r = 'R';
        this.e = 'E';
        this.x = 'X';
        this.p = 'P';

        this.version = version;
        this.type = type;
        this.flag = flag;
        this.session = session;
        this.request = request;

        bodyLength = body.length;
        this.body = body;
    }

    public byte[] getHeader() {
        byte[] header = new byte[4];
        header[0] = r;
        header[1] = e;
        header[2] = x;
        header[3] = p;

        return header;
    }

    public void setHeader(byte r, byte e, byte x, byte p) {
        this.r = r;
        this.e = e;
        this.x = x;
        this.p = p;
    }

    public byte getVersion() {
        return version;
    }

    public void setVersion(byte version) {
        this.version = version;
    }

    public byte getType() {
        return type;
    }

    public void setType(byte type) {
        this.type = type;
    }

    public byte getFlag() {
        return flag;
    }

    public void setFlag(byte flag) {
        this.flag = flag;
    }

    public UUID getSessionAsUUID() {
        return BitWorks.convertByteArrayToUUID(this.session);
    }

    public byte[] getSession() {
        return this.session;
    }

    public void setSession(byte[] session) {
        this.session = session;
    }

    public UUID getRequestAsUUID() {
        return BitWorks.convertByteArrayToUUID(this.request);
    }

    public byte[] getRequest() {
            return this.request;
    }

    public void setRequest(byte[] request) {
        this.request = request;
    }

    public int getBodyLength() {
        return bodyLength;
    }

    public void setBodyLength(int bodyLength) {
        this.bodyLength = bodyLength;
    }

    public byte[] getBody() {
        return body;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }

    public boolean hasSession() {
        return this.session != null && this.session.length == 16
                && !BitWorks.convertByteArrayToUUID(this.session).equals(EMPTY_SESSION);
    }

    public static RexProMessage read(Buffer sourceBuffer) {
        final RexProMessage message = new RexProMessage();

        message.setHeader(sourceBuffer.get(), sourceBuffer.get(),
                sourceBuffer.get(), sourceBuffer.get());

        message.setVersion(sourceBuffer.get());
        message.setType(sourceBuffer.get());
        message.setFlag(sourceBuffer.get());

        final byte[] session = new byte[16];
        sourceBuffer.get(session);
        message.setSession(session);

        final byte[] request = new byte[16];
        sourceBuffer.get(request);
        message.setRequest(request);

        message.setBodyLength(sourceBuffer.getInt());

        final byte[] body = new byte[message.getBodyLength()];
        sourceBuffer.get(body);
        message.setBody(body);

        return message;
    }

    public static void write(Buffer output, RexProMessage message) {
        output.put(message.getHeader());
        output.put(message.getVersion());
        output.put(message.getType());
        output.put(message.getFlag());
        output.put(message.getSession());
        output.put(message.getRequest());
        output.putInt(message.getBodyLength());
        output.put(message.getBody());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final RexProMessage other = (RexProMessage) obj;
        if (this.r != other.r) {
            return false;
        }
        if (this.e != other.e) {
            return false;
        }
        if (this.x != other.x) {
            return false;
        }
        if (this.p != other.p) {
            return false;
        }
        if (this.version != other.version) {
            return false;
        }
        if (this.flag != other.flag) {
            return false;
        }
        if (this.session != other.session && (this.session == null ||
                !Arrays.equals(this.session, other.session))) {
            return false;
        }
        if (this.request != other.request && (this.request == null ||
                !Arrays.equals(this.request, other.request))) {
            return false;
        }
        if (this.bodyLength != other.bodyLength) {
            return false;
        }
        if (this.body != other.body && (this.body == null ||
                !Arrays.equals(this.body, other.body))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + this.r;
        hash = 97 * hash + this.e;
        hash = 97 * hash + this.x;
        hash = 97 * hash + this.p;
        hash = 97 * hash + this.version;
        hash = 97 * hash + this.flag;
        hash = 97 * hash + (this.session != null ? this.session.hashCode() : 0);
        hash = 97 * hash + (this.request != null ? this.request.hashCode() : 0);
        hash = 97 * hash + this.bodyLength;
        hash = 97 * hash + (this.body != null ? this.body.hashCode() : 0);
        return hash;
    }
}
