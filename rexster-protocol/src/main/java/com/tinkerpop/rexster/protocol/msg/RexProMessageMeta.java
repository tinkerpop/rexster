package com.tinkerpop.rexster.protocol.msg;

import org.msgpack.MessageTypeException;
import org.msgpack.template.AbstractTemplate;
import org.msgpack.template.Template;
import org.msgpack.packer.Packer;
import org.msgpack.template.Templates;
import org.msgpack.type.Value;
import org.msgpack.unpacker.Unpacker;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * HashMap with a special template
 */
public class RexProMessageMeta extends HashMap<String, Object> {

    public RexProMessageMeta() { }
    public RexProMessageMeta(int i) { super(i); }
    public RexProMessageMeta(int i, float v) { super(i, v); }
    public RexProMessageMeta(Map<? extends String, ?> map) { super(map); }

    public static class SerializationTemplate extends AbstractTemplate<RexProMessageMeta>{

        @Override
        public void write(Packer pk, RexProMessageMeta v) throws IOException {
            write(pk, v, false);
        }

        public void write(Packer pk, RexProMessageMeta v, boolean required) throws IOException {
            RexProMessageMeta meta = v;
            if (meta == null) {
                meta = new RexProMessageMeta();
            }
            pk.writeMapBegin(meta.size());
            for (Map.Entry pair : meta.entrySet()) {
                pk.write(pair.getKey());
                pk.write(pair.getValue());
            }
            pk.writeMapEnd();
        }

        @Override
        public RexProMessageMeta read(Unpacker u, RexProMessageMeta to) throws IOException {
            return read(u, to, false);
        }

        protected Object deserializeObject(Value v) {
            if (v == null) {
                return null;
            } else if (v.isBooleanValue()) {
                return v.asBooleanValue().getBoolean();
            } else if (v.isFloatValue()) {
                return v.asFloatValue().getDouble();
            } else if (v.isIntegerValue()) {
                return v.asIntegerValue().getInt();
            } else {
                return v.asRawValue().getString();
            }
        }

        public RexProMessageMeta read(Unpacker u, RexProMessageMeta to, boolean required) throws IOException {
            if (!required && u.trySkipNil()) {
                return null;
            }

            RexProMessageMeta meta;
            if (to != null) {
                meta = to;
                meta.clear();
            } else {
                meta = new RexProMessageMeta();
            }

            int n = u.readMapBegin();
            for (int i=0; i<n; i++) {
                String key = u.read(Templates.TString);
                Object val = deserializeObject(u.read(Templates.TValue));
                meta.put(key, val);
            }
            u.readMapEnd();

            return meta;
        }


        public static SerializationTemplate instance = new SerializationTemplate();
        static public SerializationTemplate getInstance() {
            return instance;
        }

    }

}
