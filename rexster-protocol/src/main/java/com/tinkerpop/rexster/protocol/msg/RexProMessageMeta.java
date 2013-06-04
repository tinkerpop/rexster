package com.tinkerpop.rexster.protocol.msg;

import java.util.HashMap;
import java.util.Map;

/**
 * HashMap with a msgpack template
 *
 * @author Blake Eggleston (bdeggleston.github.com)
 */
public class RexProMessageMeta extends HashMap<String, Object> {

    public RexProMessageMeta() { }
    public RexProMessageMeta(int i) { super(i); }
    public RexProMessageMeta(int i, float v) { super(i, v); }
    public RexProMessageMeta(Map<? extends String, ?> map) { super(map); }

}
