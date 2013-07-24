package com.tinkerpop.rexster.protocol.serializer.msgpack.templates;

import com.tinkerpop.rexster.protocol.msg.RexProBindings;
import org.msgpack.packer.Packer;
import org.msgpack.template.AbstractTemplate;
import org.msgpack.template.Templates;
import org.msgpack.unpacker.Unpacker;

import java.io.IOException;
import java.util.Map;

/**
 * @author Blake Eggleston (bdeggleston.github.com)
 */
public class BindingsTemplate extends AbstractTemplate<RexProBindings> {
    @Override
    public void write(Packer pk, RexProBindings v) throws IOException {
        write(pk, v, false);
    }

    public void write(final Packer pk, final RexProBindings v, final boolean required) throws IOException {
        RexProBindings bindings = v;
        if (bindings == null) {
            bindings = new RexProBindings();
        }
        pk.writeMapBegin(bindings.size());
        for (Map.Entry pair : bindings.entrySet()) {
            pk.write(pair.getKey());
            try {
                ResultsConverter.serializeObject(pair.getValue(), pk);
            } catch (Exception ex) {
                throw new IOException(ex.toString());
            }
        }
        pk.writeMapEnd();
    }

    @Override
    public RexProBindings read(Unpacker u, RexProBindings to) throws IOException {
        return read(u, to, false);
    }

    public RexProBindings read(final Unpacker u, final RexProBindings to, final boolean required) throws IOException {
        if (!required && u.trySkipNil()) {
            return null;
        }

        RexProBindings bindings;
        if (to != null) {
            bindings = to;
            bindings.clear();
        } else {
            bindings = new RexProBindings();
        }

        int n = u.readMapBegin();
        for (int i=0; i<n; i++) {
            final String key = u.read(Templates.TString);
            final Object val = ResultsConverter.deserializeObject(u.read(Templates.TValue));
            bindings.put(key, val);
        }
        u.readMapEnd();

        return bindings;
    }

    public static BindingsTemplate instance = new BindingsTemplate();
    static public BindingsTemplate getInstance() {
        return instance;
    }
}
