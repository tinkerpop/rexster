package com.tinkerpop.rexster.protocol.serializer.msgpack.templates;

import com.tinkerpop.rexster.protocol.msg.RexProScriptResult;
import org.msgpack.packer.Packer;
import org.msgpack.template.AbstractTemplate;
import org.msgpack.template.Templates;
import org.msgpack.unpacker.Unpacker;

import java.io.IOException;

/**
 * Template for interpreter output
 *
 * @author Blake Eggleston (bdeggleston.github.com)
 */
public class ResultsTemplate extends AbstractTemplate<RexProScriptResult> {
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
            ResultsConverter.serializeObject(result.get(), pk);
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

        result.set(ResultsConverter.deserializeObject(u.read(Templates.TValue)));
        return result;
    }

    public static ResultsTemplate instance = new ResultsTemplate();
    static public ResultsTemplate getInstance() {
        return instance;
    }
}
