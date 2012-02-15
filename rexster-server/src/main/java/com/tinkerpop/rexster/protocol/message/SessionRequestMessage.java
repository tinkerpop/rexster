package com.tinkerpop.rexster.protocol.message;

import com.tinkerpop.rexster.protocol.BitWorks;

import java.nio.ByteBuffer;
import java.util.UUID;

public class SessionRequestMessage extends RexProMessage {

    public static final int DEFAULT_CHUNK_SIZE = 8192;

    public static final byte CHANNEL_NONE = 0;

    public static final byte CHANNEL_CONSOLE = 1;

    /**
     * Starts a session.
     */
    public static final byte FLAG_NEW_SESSION = 0;

    /**
     * Destroy a session.
     */
    public static final byte FLAG_KILL_SESSION = 1;

    public SessionRequestMessage(RexProMessage message) {
        super(message.getVersion(), message.getType(), message.getFlag(),
                message.getSession(), message.getRequest(), message.getBody());

        if (this.getType() != MessageType.SESSION_REQUEST) {
            throw new IllegalArgumentException("The message is not of type SESSION_REQUEST");
        }
    }

    protected SessionRequestMessage(byte flag) {
        super(RexProMessage.CURRENT_VERSION, MessageType.SESSION_REQUEST, flag,
                BitWorks.convertUUIDToByteArray(EMPTY_SESSION),
                BitWorks.convertUUIDToByteArray(UUID.randomUUID()),
                new byte[0]);

        if (this.getFlag() != FLAG_KILL_SESSION) {
            throw new IllegalArgumentException("This type of message cannot be constructed without a body.");
        }
    }

    public SessionRequestMessage(byte flag, byte channel, String username, String password) {
        this(flag, channel, DEFAULT_CHUNK_SIZE, username, password);
    }

    public SessionRequestMessage(byte flag, byte channel, int chunkSize, String username, String password) {
        super(RexProMessage.CURRENT_VERSION, MessageType.SESSION_REQUEST, flag,
                BitWorks.convertUUIDToByteArray(EMPTY_SESSION),
                BitWorks.convertUUIDToByteArray(UUID.randomUUID()),
                buildBody(channel, chunkSize, username, password));

        if (this.getFlag() == FLAG_KILL_SESSION) {
            throw new IllegalArgumentException("This type of message must be constructed without a body.");
        }
    }

    public byte getChannel() {

        byte channel = CHANNEL_NONE;
        if (this.body.length > 0) {
            channel = this.body[0];

            if (!isValidChannel(channel)) {
                channel = CHANNEL_NONE;
            }
        }

        return channel;

    }

    public String[] getUsernamePassword() {
        final String[] usernamePassword = new String [] { "", "" };

        if (this.body.length > 0) {
            ByteBuffer buffer = ByteBuffer.wrap(this.body);
            buffer.position(5);
            int usernameLength = buffer.getInt();

            byte[] usernameBytes = new byte[usernameLength];
            buffer.get(usernameBytes);
            usernamePassword[0] = new String(usernameBytes);

            int passwordLength = buffer.getInt();

            byte[] passwordBytes = new byte[passwordLength];
            buffer.get(passwordBytes);
            usernamePassword[1] = new String(passwordBytes);
        }

        return usernamePassword;
    }

    public int getChunkSize() {
        int chunkSize = DEFAULT_CHUNK_SIZE;
        if (this.body.length > 1) {
            ByteBuffer bb = ByteBuffer.wrap(this.body);
            bb.position(1);
            chunkSize = bb.getInt();
        }

        return chunkSize;
    }

    public static boolean isValidChannel(byte channel) {
        return channel == CHANNEL_NONE || channel == CHANNEL_CONSOLE;
    }

    private static byte[] buildBody(byte channel, int chunkSize, String username, String password) {
        ByteBuffer bb = ByteBuffer.allocate(13 + username.length() + password.length());

        bb.put(channel);
        bb.putInt(chunkSize);
        bb.putInt(username.length());
        bb.put(username.getBytes());
        bb.putInt(password.length());
        bb.put(password.getBytes());

        return bb.array();
    }
}
