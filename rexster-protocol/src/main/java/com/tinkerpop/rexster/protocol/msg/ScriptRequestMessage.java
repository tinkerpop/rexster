package com.tinkerpop.rexster.protocol.msg;

import org.msgpack.annotation.Message;

import java.io.IOException;

/**
 * Represents a request to process a script.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 * @author Blake Eggleston (bdeggleston.github.com)
 */
@Message
public class ScriptRequestMessage extends RexProMessage {

    protected static final String META_KEY_IN_SESSION = "inSession";
    protected static final String META_KEY_GRAPH_NAME = "graphName";
    protected static final String META_KEY_GRAPH_OBJECT_NAME = "graphObjName";
    protected static final String META_KEY_ISOLATE_REQUEST = "isolate";
    protected static final String META_KEY_TRANSACTION = "transaction";
    protected static final String META_KEY_CONSOLE = "console";

    protected RexProMessageMetaField[] getMetaFields() {
        RexProMessageMetaField[] fields = {
            //indicates this requests should be executed in the supplied session
            RexProMessageMetaField.define(META_KEY_IN_SESSION, false, false, Boolean.class),

            //sets the graph and graph variable name for this session, optional
            RexProMessageMetaField.define(META_KEY_GRAPH_NAME, false, null, String.class),
            RexProMessageMetaField.define(META_KEY_GRAPH_OBJECT_NAME, false, "g", String.class),

            //indicates variables defined in this request will not be available in the next
            RexProMessageMetaField.define(META_KEY_ISOLATE_REQUEST, false, true, Boolean.class),

            //indicates this request should be wrapped in a transaction
            RexProMessageMetaField.define(META_KEY_TRANSACTION, false, true, Boolean.class),

            // indicates the response should be toString'd
            RexProMessageMetaField.define(META_KEY_CONSOLE, false, false, Boolean.class)
        };
        return fields;
    }

    public String LanguageName;
    public String Script;
    public RexProBindings Bindings = new RexProBindings();

    public javax.script.Bindings getBindings() throws IOException, ClassNotFoundException {
        return this.Bindings;
    }

    /**
     * Sets the inSession meta val
     */
    public void metaSetInSession(Boolean val) {
        Meta.put("inSession", val);
    }

    /**
     * Gets the inSession meta val, or the default if not set
     */
    public Boolean metaGetInSession() {
        if (!Meta.containsKey(META_KEY_IN_SESSION)) {
            return false;
        } else {
            return (Boolean) Meta.get(META_KEY_IN_SESSION);
        }
    }

    public void metaSetGraphName(final String val) {
        Meta.put(META_KEY_GRAPH_NAME, val);
    }

    public String metaGetGraphName() {
        return (String) Meta.get(META_KEY_GRAPH_NAME);
    }

    public void metaSetGraphObjName(final String val) {
        Meta.put(META_KEY_GRAPH_OBJECT_NAME, val);
    }

    public String metaGetGraphObjName() {
        return (String) Meta.get(META_KEY_GRAPH_OBJECT_NAME);
    }

    public void metaSetIsolate(final boolean val) {
        Meta.put(META_KEY_ISOLATE_REQUEST, val);
    }

    public Boolean metaGetIsolate() {
        return (Boolean) Meta.get(META_KEY_ISOLATE_REQUEST);
    }

    public void metaSetTransaction(final boolean val) {
        Meta.put(META_KEY_TRANSACTION, val);
    }

    public Boolean metaGetTransaction() {
        return (Boolean) Meta.get(META_KEY_TRANSACTION);
    }

    public void metaSetConsole(final boolean v) {
        Meta.put(META_KEY_CONSOLE, v);
    }

    public Boolean metaGetConsole() {
        return (Boolean) Meta.get(META_KEY_CONSOLE);
    }
}
