package com.tinkerpop.rexster.protocol.serializer.msgpack.templates;

import com.tinkerpop.rexster.protocol.msg.RexProMessageMeta;
import org.msgpack.packer.Packer;
import org.msgpack.template.AbstractTemplate;
import org.msgpack.template.Templates;
import org.msgpack.type.Value;
import org.msgpack.unpacker.Unpacker;

import java.io.IOException;
import java.util.Map;

/**
 * Serializer for message meta fields
 *
 * @author Blake Eggleston (bdeggleston.github.com)
 */
public class MetaTemplate extends AbstractTemplate<RexProMessageMeta> {

    @Override
    public void write(Packer pk, RexProMessageMeta v) throws IOException {
        write(pk, v, false);
    }

    public void write(final Packer pk, final RexProMessageMeta v, final boolean required) throws IOException {
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

    public RexProMessageMeta read(final Unpacker u, final RexProMessageMeta to, final boolean required) throws IOException {
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
            final String key = u.read(Templates.TString);
            final Object val = deserializeObject(u.read(Templates.TValue));
            meta.put(key, val);
        }
        u.readMapEnd();

        return meta;
    }


    public static MetaTemplate instance = new MetaTemplate();
    static public MetaTemplate getInstance() {
        return instance;
    }
}
