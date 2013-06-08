package com.tinkerpop.rexster.protocol.msg;

import com.tinkerpop.rexster.protocol.MsgPackConverter;
import org.msgpack.packer.Packer;
import org.msgpack.template.AbstractTemplate;
import org.msgpack.template.Templates;
import org.msgpack.unpacker.Unpacker;

import java.io.IOException;

/**
 * Object with a msgpack template
 *
 * @author Blake Eggleston (bdeggleston.github.com)
 */
public class RexProScriptResult {

    private Object value;

    public RexProScriptResult() {
        this.value = null;
    }

    public RexProScriptResult(Object value) {
        this.value = value;
    }

    public Object get() {
        return value;
    }

    public void set(Object val) {
        value = val;
    }

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
                MsgPackConverter.serializeObject(result.get(), pk);
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

            RexProScriptResult result;
            if(to != null) {
                result = to;
            } else {
                result = new RexProScriptResult();
            }

            result.set(MsgPackConverter.deserializeObject(u.read(Templates.TValue)));
            return result;
        }

        public static SerializationTemplate instance = new SerializationTemplate();
        static public SerializationTemplate getInstance() {
            return instance;
        }
    }
}
