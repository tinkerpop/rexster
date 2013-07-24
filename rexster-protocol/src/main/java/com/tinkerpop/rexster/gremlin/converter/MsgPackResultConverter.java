package com.tinkerpop.rexster.gremlin.converter;

import com.tinkerpop.rexster.protocol.serializer.msgpack.templates.ResultsConverter;
import org.msgpack.MessagePack;
import org.msgpack.packer.BufferPacker;

/**
 * Converts a result from Gremlin to a byte array encoded by MsgPack.
 *
 * @author Blake Eggleston (bdeggleston.github.com)
 */
public class MsgPackResultConverter implements ResultConverter<byte[]> {
    private final MessagePack msgpack = new MessagePack();

    public byte[] convert(final Object result) throws Exception {
        final BufferPacker packer = msgpack.createBufferPacker(1024);
        try {
            ResultsConverter.serializeObject(result, packer);
            return packer.toByteArray();
        } catch (Exception e) {
            throw e;
        } finally {
            packer.close();
        }
    }
}
