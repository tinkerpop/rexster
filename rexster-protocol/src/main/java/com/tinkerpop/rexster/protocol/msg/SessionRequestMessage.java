package com.tinkerpop.rexster.protocol.msg;

import org.msgpack.annotation.Message;

/**
 * Represents a request to open or close a session.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 * @author Blake Eggleston (bdeggleston.github.com)
 */
@Message
public class SessionRequestMessage extends RexProMessage {

    protected static final String KILL_SESSION_META_KEY = "killSession";
    protected static final String GRAPH_NAME_META_KEY = "graphName";
    protected static final String GRAPH_OBJECT_NAME_META_KEY = "graphObjName";
    protected RexProMessageMetaField[] getMetaFields() {
        RexProMessageMetaField[] fields = {
            //indicates this session should be destroyed
            RexProMessageMetaField.define(KILL_SESSION_META_KEY, false, false, Boolean.class),

            //sets the graph and graph variable name for this session, optional
            RexProMessageMetaField.define(GRAPH_NAME_META_KEY, false, null, String.class),
            RexProMessageMetaField.define(GRAPH_OBJECT_NAME_META_KEY, false, "g", String.class)
        };
        return fields;
    }

    public String Username;
    public String Password;

    public void metaSetKillSession(Boolean val) {
        Meta.put(KILL_SESSION_META_KEY, val);
    }

    public Boolean metaGetKillSession() {
        return (Boolean) Meta.get(KILL_SESSION_META_KEY);
    }

    public void metaSetGraphName(String val) {
        Meta.put(GRAPH_NAME_META_KEY, val);
    }

    public String metaGetGraphName() {
        return (String) Meta.get(GRAPH_NAME_META_KEY);
    }

    public void metaSetGraphObjName(String val) {
        Meta.put(GRAPH_OBJECT_NAME_META_KEY, val);
    }

    public String metaGetGraphObjName() {
        return (String) Meta.get(GRAPH_OBJECT_NAME_META_KEY);
    }
}
