package com.tinkerpop.rexster.protocol.msg;

import org.msgpack.packer.Packer;
import org.msgpack.template.AbstractTemplate;
import org.msgpack.template.Templates;
import org.msgpack.type.Value;
import org.msgpack.unpacker.Unpacker;

import java.io.IOException;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: bdeggleston
 * Date: 2/6/13
 * Time: 12:33 PM
 * To change this template use File | Settings | File Templates.
 */
public class RexProMessageMetaTemplate extends AbstractTemplate<RexProMessageMeta> {
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
            // includes raw value
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

    static final RexProMessageMetaTemplate instance = new RexProMessageMetaTemplate();
    static public RexProMessageMetaTemplate getInstance() {
        return instance;
    }
}
