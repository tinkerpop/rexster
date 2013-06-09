package com.tinkerpop.rexster.protocol.msg;

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
}
