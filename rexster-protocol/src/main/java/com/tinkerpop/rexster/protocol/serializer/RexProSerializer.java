package com.tinkerpop.rexster.protocol.serializer;

import com.tinkerpop.rexster.protocol.msg.RexProMessage;

import java.io.IOException;

/**
 * RexPro serializer interface. Use this to implement new serializers
 *
 * @author Blake Eggleston (bdeggleston.github.com)
 */
public interface RexProSerializer {

    /**
     * Consumes a byte array and RexProMessage class and returns a RexProMessage instance
     *
     * @param bytes
     * @param messageClass
     * @param <Message>
     * @return
     */
    public <Message extends RexProMessage> Message deserialize(byte[] bytes, Class<Message> messageClass) throws IOException;

    /**
     * Consumes a RexProMessage and returns a byte array
     *
     * @param message
     * @param <Message>
     * @return
     */
    public <Message extends RexProMessage> byte[] serialize(Message message, Class<Message> messageClass) throws IOException;

    /**
     * Returns the byte uniquely identifying this serializer
     * @return
     */
    public byte getSerializerId();
}
