package com.tinkerpop.rexster.protocol.msg;

import org.msgpack.MessageTypeException;
import org.msgpack.template.AbstractTemplate;
import org.msgpack.template.Template;
import org.msgpack.packer.Packer;
import org.msgpack.template.Templates;
import org.msgpack.unpacker.Unpacker;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * HashMap with a special template
 */
public class RexProMessageMeta extends HashMap<String, Object> {

    public RexProMessageMeta(int i, float v) { super(i, v); }
    public RexProMessageMeta(int i) { super(i); }
    public RexProMessageMeta() { }
    public RexProMessageMeta(Map<? extends String, ?> map) { super(map); }

}
