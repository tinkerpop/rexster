package com.tinkerpop.rexster.protocol;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;

public class RemoteRexster {

    private RemoteRexsterSession session = null;

    public RemoteRexster() {

    }

    public RemoteRexster(RemoteRexsterSession session) {
        this.session = session;
    }

    public RemoteRexster(String rexProHost, int rexProPort) {
        this.session = new RemoteRexsterSession(rexProHost, rexProPort);
    }

    public RemoteRexsterSession getSession() {
        return this.session;
    }

    public Object eval(String script, String scriptEngineName) {
        return eval(script, scriptEngineName, this.session);
    }

    public static Object eval(String script, String scriptEngineName, RemoteRexsterSession session) {

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
