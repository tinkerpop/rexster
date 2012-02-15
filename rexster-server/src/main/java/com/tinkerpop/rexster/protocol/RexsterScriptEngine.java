package com.tinkerpop.rexster.protocol;

import com.tinkerpop.rexster.protocol.message.RexProMessage;
import com.tinkerpop.rexster.protocol.message.ScriptRequestMessage;
import com.tinkerpop.rexster.protocol.message.ScriptResponseMessage;

import javax.script.AbstractScriptEngine;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.util.ArrayList;

public class RexsterScriptEngine extends AbstractScriptEngine {

    public static final String CONFIG_SCOPE_HOST = "_host";
    public static final String CONFIG_SCOPE_PORT = "_port";
    public static final String CONFIG_SCOPE_LANGUAGE = "_language";

    private RemoteRexsterSession session = null;
    private String scriptEngineName;

    private int port = 8184;
    private String host = "localhost";

    private boolean isSessionChanged = true;

    public RemoteRexsterSession getSession() {
        return this.session;
    }

    public Bindings createBindings() {
        return new RexsterBindings();
    }

    public void setBindings(Bindings bindings, int scope) {
        if (!(bindings instanceof RexsterBindings)) {
            throw new IllegalArgumentException("Bindings must be of type RexsterBindings.");
        }

        super.setBindings(bindings, scope);
    }

    public void setContext(ScriptContext context) {
        if (!(context.getBindings(ScriptContext.ENGINE_SCOPE) instanceof RexsterBindings)) {
            throw new IllegalArgumentException("Engine Scope Bindings must be of type RexsterBindings.");
        }

        if (!(context.getBindings(ScriptContext.GLOBAL_SCOPE) instanceof RexsterBindings)) {
            throw new IllegalArgumentException("Global Scope Bindings must be of type RexsterBindings.");
        }

        super.setContext(context);
    }

    public ScriptEngineFactory getFactory() {
        return new RexsterScriptEngineFactory();
    }

    public void put(String key, Object value) {
        if (key.equals(CONFIG_SCOPE_HOST)) {
            this.host = (String) value;
            isSessionChanged = true;
        } else if (key.equals(CONFIG_SCOPE_PORT)) {
            this.port = Integer.parseInt(value.toString());
            isSessionChanged = true;
        } else if (key.equals(CONFIG_SCOPE_LANGUAGE)) {
            this.scriptEngineName = (String) value;
        } else {
            this.getBindings(ScriptContext.ENGINE_SCOPE).put(key, value);
        }
    }

    public Object get(String key) {
        if (key.equals(CONFIG_SCOPE_HOST)) {
            return this.host;
        } else if (key.equals(CONFIG_SCOPE_PORT)) {
            return this.port;
        } else if (key.equals(CONFIG_SCOPE_LANGUAGE)) {
            return this.scriptEngineName;
        } else {
            return this.getBindings(ScriptContext.ENGINE_SCOPE).get(key);
        }
    }

    public Object eval(final String script, final ScriptContext context) throws ScriptException {
        return this.eval(new StringReader(script), context);
    }

    public Object eval(final Reader reader, final ScriptContext context) throws ScriptException {

        if (!(context.getBindings(ScriptContext.ENGINE_SCOPE) instanceof RexsterBindings)) {
            throw new IllegalArgumentException("Engine Scope Bindings must be of type RexsterBindings.");
        }

        if (!(context.getBindings(ScriptContext.GLOBAL_SCOPE) instanceof RexsterBindings)) {
            throw new IllegalArgumentException("Global Scope Bindings must be of type RexsterBindings.");
        }

        String line;
        BufferedReader bReader = new BufferedReader(reader);
        Object finalValue = null;
        StringBuffer buffer = new StringBuffer();

        try {
            while ((line = bReader.readLine()) != null) {
                if (line.isEmpty()) {
                    continue;
                } else {
                    buffer.append(line).append("\n");
                }
            }

            if (buffer.length() > 0) {
                if (this.isSessionChanged) {
                    // session was changed by "put"
                    this.session = new RemoteRexsterSession(this.host, this.port, "", "");
                    this.isSessionChanged = false;
                }

                finalValue = eval(buffer.toString(), this.scriptEngineName, this.session,
                        (RexsterBindings) this.getBindings(ScriptContext.ENGINE_SCOPE));
            }

        } catch (Exception e) {
            throw new ScriptException(e.getMessage());
        }

        return finalValue;
    }

    private static Object eval(String script, String scriptEngineName, RemoteRexsterSession session, RexsterBindings bindings) {

        Object returnValue = null;

        try {
            session.open();
            final RexProMessage scriptMessage = new ScriptRequestMessage(
                    session.getSessionKey(), scriptEngineName, bindings, script);

            final RexProMessage resultMessage = RexPro.sendMessage(
                    session.getRexProHost(), session.getRexProPort(), scriptMessage);

            final ScriptResponseMessage responseMessage = new ScriptResponseMessage(resultMessage);
            RexsterBindings bindingsFromServer = responseMessage.getBindings();

            // apply bindings from server to local bindings so that they are in synch
            if (bindingsFromServer != null) {
                bindings.putAll(bindingsFromServer);
            }

            ArrayList listOfDeserializedObjects = new ArrayList();
            ByteBuffer bb = ByteBuffer.wrap(resultMessage.getBody());

            // navigate to the start of the results
            int lengthOfBindings = bb.getInt();
            bb.position(lengthOfBindings + 4);

            while (bb.hasRemaining()) {
                int segmentLength = bb.getInt();
                byte[] resultObjectBytes = new byte[segmentLength];
                bb.get(resultObjectBytes);
                ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(resultObjectBytes);
                ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);

                Object o = objectInputStream.readObject();

                listOfDeserializedObjects.add(o);
            }

            returnValue = listOfDeserializedObjects.iterator();

            if (listOfDeserializedObjects.size() == 1) {
                returnValue = listOfDeserializedObjects.get(0);
            }
        } catch (Exception e) {

        } finally {
        }

        return returnValue;
    }

    public void close() {
        if (this.session != null) {
            this.session.close();
            this.session = null;
        }
    }
}
