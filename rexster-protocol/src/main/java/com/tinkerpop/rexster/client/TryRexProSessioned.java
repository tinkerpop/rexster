package com.tinkerpop.rexster.client;

import com.tinkerpop.rexster.Tokens;
import com.tinkerpop.rexster.protocol.msg.ScriptRequestMessage;
import com.tinkerpop.rexster.protocol.msg.ScriptResponseMessage;
import org.msgpack.MessagePack;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
/**
 * A bit of an experiment.
 */
public class TryRexProSessioned {

    public static void main(String[] args) {
        //bigCalls();
        lotsOfCalls(false);
    }

    private static void bigCalls() {

        RemoteRexsterSession session = new RemoteRexsterSession("localhost", 8184, 100, "", "");
        session.open();

        long start = System.currentTimeMillis();

        long checkpoint = System.currentTimeMillis();

        try {
            ScriptResponseMessage resultMessage = (ScriptResponseMessage) session.sendRequest(
                    createScriptRequestMessage(session, "g=rexster.getGraph('gratefulgraph');g.V;"), 100);
            resultMessage = (ScriptResponseMessage) session.sendRequest(
                    createScriptRequestMessage(session, "g.E;"), 100);
            System.out.println((checkpoint - start) + ":" + (System.currentTimeMillis() - checkpoint));

        } catch (Exception ex) {
            ex.printStackTrace();
        }


    }

    private static void lotsOfCalls(boolean doJson){

        RemoteRexsterSession session = new RemoteRexsterSession("localhost", 8184, 100, "", "");
        session.open();

        MessagePack msgpack = new MessagePack();

        long start = System.currentTimeMillis();


        long checkpoint = System.currentTimeMillis();

        try {

            ScriptResponseMessage resultMessage = (ScriptResponseMessage) session.sendRequest(
                    createScriptRequestMessage(session, "g=rexster.getGraph('gratefulgraph');g.V;"), 100);

            int counter = 1;
            Iterator itty = ((Iterable) resultMessage.Results).iterator();
            while (itty.hasNext()){
                final Map<String,Object> map = (Map<String, Object>) itty.next();
                final String vId = (String) map.get(Tokens._ID);

                ScriptResponseMessage vertexResultMessage = (ScriptResponseMessage) session.sendRequest(
                        createScriptRequestMessage(session, "g.v(" + vId + ")"), 100);

                System.out.println(vertexResultMessage.Results);
                counter++;
            }

            /*
            resultMessage = (ScriptResponseMessage) session.sendRequest(
                    createScriptRequestMessage(session, "g.E;"), 100);

            unpacker = msgpack.createBufferUnpacker(resultMessage.Results);

            itty = unpacker.iterator();
            while (itty.hasNext()){
                final Map<String,Value> map = new Converter(msgpack, itty.next()).read(tMap(TString, TValue));
                final String eId = map.get(Tokens._ID).asRawValue().getString();

                ScriptResponseMessage edgeResultMessage = (ScriptResponseMessage) session.sendRequest(
                        createScriptRequestMessage(session, "g.e(" + eId + ")"), 100);

                unpacker = msgpack.createBufferUnpacker(edgeResultMessage.Results);
                System.out.println(unpacker.read(tMap(TString, TValue)));
            }
            */

            long end = System.currentTimeMillis() - checkpoint;
            System.out.println((checkpoint - start) + ":" + end);
            System.out.println(counter / (end / 1000));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static ScriptRequestMessage createScriptRequestMessage(RemoteRexsterSession session, String script) throws IOException {
        ScriptRequestMessage scriptMessage = new ScriptRequestMessage();
        scriptMessage.setSessionAsUUID(session.getSessionKey());
        scriptMessage.Script = script;

        scriptMessage.LanguageName = "groovy";
        scriptMessage.metaSetInSession(true);
        scriptMessage.setRequestAsUUID(UUID.randomUUID());
        return scriptMessage;
    }
}