package com.tinkerpop.rexster.protocol.msg;

import java.util.HashMap;
import java.util.Map;

/**
 * Meta data container for RexPro messages
 *
 * @author Blake Eggleston (bdeggleston.github.com)
 */
public class RexProMessageMeta extends HashMap<String, Object> {
    public RexProMessageMeta(int i, float v) { super(i, v); }

    public RexProMessageMeta(int i) { super(i); }

    public RexProMessageMeta() { }

    public RexProMessageMeta(Map<? extends String, ?> map) { super(map); }

}
