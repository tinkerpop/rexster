package com.tinkerpop.rexster.protocol.msg;

import com.tinkerpop.rexster.protocol.MsgPackConverter;

import org.msgpack.packer.Packer;
import org.msgpack.template.AbstractTemplate;
import org.msgpack.unpacker.Unpacker;
import org.msgpack.template.Templates;

import java.io.IOException;

/**
 * Object with a msgpack template
 *
 * @author Blake Eggleston (bdeggleston.github.com)
 */
public class RexProScriptResult extends Object {
    public static class SerializationTemplate extends AbstractTemplate<RexProScriptResult> {

        @Override
        public void write(Packer pk, RexProScriptResult v) throws IOException {
            write(pk, v, false);
        }

        public void write(final Packer pk, final RexProScriptResult v, final boolean required) throws IOException {
            RexProScriptResult result = v;
            if (result == null) {
                result = new RexProScriptResult();
            }
            try{
                MsgPackConverter.serializeObject(result, pk);
            } catch (Exception ex) {
                throw new IOException(ex.toString());
            }
        }

        @Override
        public RexProScriptResult read(Unpacker u, RexProScriptResult to) throws IOException {
            return read(u, to, false);
        }

        public RexProScriptResult read(final Unpacker u, final RexProScriptResult to, final boolean required) throws IOException {
            if (!required && u.trySkipNil()) {
                return null;
            }
            return (RexProScriptResult) MsgPackConverter.deserializeObject(u.read(Templates.TValue));

        }

        public static SerializationTemplate instance = new SerializationTemplate();
        static public SerializationTemplate getInstance() {
            return instance;
        }
    }
}
