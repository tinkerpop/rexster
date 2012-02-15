package com.tinkerpop.rexster.protocol.message;

import com.tinkerpop.rexster.protocol.BitWorks;
import org.glassfish.grizzly.Buffer;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.UUID;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

public class RexProMessage {

    public static final int HEADER_SIZE = 51;

    public static final byte CURRENT_VERSION = (byte) 0;

    public static final UUID EMPTY_SESSION = new UUID(0, 0);

    private byte r;
    private byte e;
    private byte x;
    private byte p;

    protected byte[] checksum;

    protected byte version;

    protected byte type;

    protected byte flag;

    protected byte[] session;

    protected byte[] request;

    protected int bodyLength;

    protected byte[] body;


    public RexProMessage() {
        this.r = 'R';
        this.e = 'E';
        this.x = 'X';
        this.p = 'P';
    }


    public RexProMessage(byte version, byte type, byte flag, byte[] session, byte[] request, byte[] body) {
        this(version, type, flag, session, request, null, body);
    }

    public RexProMessage(byte version, byte type, byte flag, byte[] session, byte[] request, byte[] checksum, byte[] body) {

        this();

        this.version = version;
        this.type = type;
        this.flag = flag;
        this.session = session;
        this.request = request;

        this.bodyLength = body.length;
        this.body = body;
        this.checksum = checksum == null ? this.calculateChecksum() : checksum;
    }

    public byte[] getHeaderIdentification() {
        byte[] header = new byte[4];
        header[0] = r;
        header[1] = e;
        header[2] = x;
        header[3] = p;

        return header;
    }

    public void setHeaderIdentification(byte r, byte e, byte x, byte p) {
        this.r = r;
        this.e = e;
        this.x = x;
        this.p = p;
    }

    public byte[] getHeaderMessageInfo() {

        // length of full header less the checksum  + rexp
        ByteBuffer bb = ByteBuffer.allocate(HEADER_SIZE - 12);

        bb.put(this.version);
        bb.put(this.type);
        bb.put(this.flag);
        bb.put(this.session);
        bb.put(this.request);
        bb.putInt(this.bodyLength);

        return bb.array();
    }

    public byte[] getHeaderFull() {
        ByteBuffer bb = ByteBuffer.allocate(HEADER_SIZE);

        bb.put(this.getHeaderIdentification());
        bb.put(this.checksum);
        bb.put(this.getHeaderMessageInfo());

        return bb.array();
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

    public byte[] getChecksum() {
        return this.checksum;
    }

    public void setChecksum(byte[] checksum) {
        this.checksum = checksum;
    }

    public byte[] getBody() {
        return body;
    }

    public void setBody(byte[] body) {
        this.body = body;

        bodyLength = this.body.length;
        checksum = calculateChecksum();
    }

    public boolean hasSession() {
        return this.session != null && this.session.length == 16
                && !BitWorks.convertByteArrayToUUID(this.session).equals(EMPTY_SESSION);
    }

    public boolean isValid() {
        return this.r == 'R' && this.e == 'E' && this.x == 'X' && this.p == 'P'
                && Arrays.equals(calculateChecksum(), this.checksum);
    }

    private byte[] calculateChecksum() {
        Checksum checksum = new CRC32();

        // checksum calculated on body + header - rexp - checksum
        int length = this.bodyLength + HEADER_SIZE - 12;
        ByteBuffer bb = ByteBuffer.allocate(length);
        bb.put(this.getHeaderMessageInfo());

        if (this.bodyLength > 0) {
            bb.put(this.body);
        }

        checksum.update(bb.array(), 0, length);
        long lngChecksum = checksum.getValue();

        return ByteBuffer.allocate(8).putLong(lngChecksum).array();
    }

    public static RexProMessage read(Buffer sourceBuffer) {
        final RexProMessage message = new RexProMessage();

        message.setHeaderIdentification(sourceBuffer.get(), sourceBuffer.get(),
                sourceBuffer.get(), sourceBuffer.get());

        final byte[] checksumBytes = new byte[8];
        sourceBuffer.get(checksumBytes);
        message.setChecksum(checksumBytes);

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
        output.put(message.getHeaderFull());
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
        if (this.checksum != other.checksum && (this.checksum == null ||
                !Arrays.equals(this.checksum, other.checksum))) {
            return false;
        }
        if (this.version != other.version) {
            return false;
        }
        if (this.type != other.type) {
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
        hash = 97 * hash + (this.checksum != null ? this.checksum.hashCode() : 0);
        hash = 97 * hash + this.version;
        hash = 97 * hash + this.type;
        hash = 97 * hash + this.flag;
        hash = 97 * hash + (this.session != null ? this.session.hashCode() : 0);
        hash = 97 * hash + (this.request != null ? this.request.hashCode() : 0);
        hash = 97 * hash + this.bodyLength;
        hash = 97 * hash + (this.body != null ? this.body.hashCode() : 0);
        return hash;
    }
}
