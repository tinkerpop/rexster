package com.tinkerpop.rexster.protocol.msg;

import com.tinkerpop.rexster.protocol.BitWorks;
import org.msgpack.annotation.Message;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Represents a request to process a script.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 * @author Blake Eggleston (bdeggleston.github.com)
 */
@Message
public class ScriptRequestMessage extends RexProMessage {

    protected static final String IN_SESSION_META_KEY = "inSession";
    protected static final String GRAPH_NAME_META_KEY = "graphName";
    protected static final String GRAPH_OBJECT_NAME_META_KEY = "graphObjName";
    protected static final String ISOLATE_REQUEST_META_KEY = "isolate";
    protected static final String TRANSACTION_META_KEY = "transaction";
    protected RexProMessageMetaField[] getMetaFields() {
        RexProMessageMetaField[] fields = {
            //indicates this requests should be executed in the supplied session
            RexProMessageMetaField.define(IN_SESSION_META_KEY, false, false, Boolean.class),

            //sets the graph and graph variable name for this session, optional
            RexProMessageMetaField.define(GRAPH_NAME_META_KEY, false, null, String.class),
            RexProMessageMetaField.define(GRAPH_OBJECT_NAME_META_KEY, false, "g", String.class),

            //indicates variables defined in this request will not be available in the next
            RexProMessageMetaField.define(ISOLATE_REQUEST_META_KEY, false, true, Boolean.class),

            //indicates this request should be wrapped in a transaction
            RexProMessageMetaField.define(TRANSACTION_META_KEY, false, true, Boolean.class),
        };
        return fields;
    }

    public String LanguageName;
    public String Script;
    public byte[] Bindings;

    @Override
    public int estimateMessageSize() {
        return BASE_MESSAGE_SIZE
                + (LanguageName == null ? 0 : LanguageName.length())
                + (Script == null ? 0 :Script.length())
                + (Bindings == null ? 0 : Bindings.length);
    }

    public javax.script.Bindings getBindings() throws IOException, ClassNotFoundException {
        final ByteBuffer buffer = ByteBuffer.wrap(this.Bindings);

        final byte[] theRest = new byte[buffer.remaining()];
        buffer.get(theRest);
        return BitWorks.convertBytesToBindings(theRest);
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
        if (!Meta.containsKey(IN_SESSION_META_KEY)) {
            return false;
        } else {
            return (Boolean) Meta.get(IN_SESSION_META_KEY);
        }
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

    public void metaSetIsolate(Boolean val) {
        Meta.put(ISOLATE_REQUEST_META_KEY, val);
    }

    public Boolean metaGetIsolate() {
        return (Boolean) Meta.get(ISOLATE_REQUEST_META_KEY);
    }

    public void metaSetTransaction(Boolean val) {
        Meta.put(TRANSACTION_META_KEY, val);
    }

    public Boolean metaGetTransaction() {
        return (Boolean) Meta.get(TRANSACTION_META_KEY);
    }
}
