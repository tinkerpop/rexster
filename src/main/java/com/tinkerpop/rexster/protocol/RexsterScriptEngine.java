package com.tinkerpop.rexster.protocol;

import com.tinkerpop.rexster.Tokens;

import javax.script.*;
import javax.sound.sampled.Port;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;

public class RexsterScriptEngine extends AbstractScriptEngine {

    public static final String RESERVED_HOST = "host";
    public static final String RESERVED_PORT = "port";
    public static final String RESERVED_LANGUAGE = "language";

    private RemoteRexsterSession session = null;
    private String scriptEngineName;

    private int port = 8185;
    private String host = "localhost";

    private boolean isSessionChanged = true;


    public RexsterScriptEngine() {

    }

    public RexsterScriptEngine(String rexProHost, int rexProPort, String scriptEngineName) {
        this.session = new RemoteRexsterSession(rexProHost, rexProPort);
        this.scriptEngineName = scriptEngineName;
    }

    public RemoteRexsterSession getSession() {
        return this.session;
    }

    public Bindings createBindings() {
        return new SimpleBindings();
    }

    public ScriptEngineFactory getFactory() {
        return new RexsterScriptEngineFactory();
    }

    public void put(String key, Object value) {
        if (key.equals(RESERVED_HOST)) {
            this.host = (String) value;
            isSessionChanged = true;
        } else if (key.equals(RESERVED_PORT)) {
            this.port = Integer.parseInt(value.toString());
            isSessionChanged = true;
        } else if (key.equals(RESERVED_LANGUAGE)) {
            this.scriptEngineName = (String) value;
        } else {
            this.getBindings(ScriptContext.ENGINE_SCOPE).put(key, value);
        }
    }

    public Object get(String key) {
        if (key.equals(RESERVED_HOST)) {
            return this.host;
        } else if (key.equals(RESERVED_PORT)) {
            return this.port;
        } else if (key.equals(RESERVED_LANGUAGE)) {
            return this.scriptEngineName;
        } else {
            return this.getBindings(ScriptContext.ENGINE_SCOPE).get(key);
        }
    }

    public Object eval(final String script, final ScriptContext context) throws ScriptException {
        return this.eval(new StringReader(script), context);
    }

    public Object eval(final Reader reader, final ScriptContext context) throws ScriptException {
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
                    this.session = new RemoteRexsterSession(this.host, this.port);
                    this.isSessionChanged = false;
                }

                finalValue = eval(buffer.toString(), this.scriptEngineName, this.session);
            }

        } catch (Exception e) {
            throw new ScriptException(e.getMessage());
        }

        return finalValue;
    }

    private static Object eval(String script, String scriptEngineName, RemoteRexsterSession session) {

        Object returnValue = null;

        try {
            session.open();
            final RexProMessage scriptMessage = new ScriptRequestMessage(
                    session.getSessionKey(), scriptEngineName, script);

            final RexProMessage resultMessage = RexPro.sendMessage(
                    session.getRexProHost(), session.getRexProPort(), scriptMessage);

            ArrayList listOfDeserializedObjects = new ArrayList();
            ByteBuffer bb = ByteBuffer.wrap(resultMessage.getBody());
            while (bb.hasRemaining()) {
                int segmentLength = bb.getInt();
                byte[] resultObjectBytes = new byte[segmentLength];
                bb.get(resultObjectBytes);
                ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(resultObjectBytes);
                ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);

                Object o = objectInputStream.readObject();

                listOfDeserializedObjects.add(o);
            }

            returnValue = listOfDeserializedObjects;

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
