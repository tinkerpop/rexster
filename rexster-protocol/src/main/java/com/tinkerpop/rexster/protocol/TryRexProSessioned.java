package com.tinkerpop.rexster.protocol;

import com.tinkerpop.rexster.Tokens;
import com.tinkerpop.rexster.protocol.msg.ConsoleScriptResponseMessage;
import com.tinkerpop.rexster.protocol.msg.MessageType;
import com.tinkerpop.rexster.protocol.msg.MsgPackScriptResponseMessage;
import com.tinkerpop.rexster.protocol.msg.ScriptRequestMessage;
import com.tinkerpop.rexster.protocol.msg.SessionRequestMessage;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.msgpack.MessagePack;
import org.msgpack.type.Value;
import org.msgpack.unpacker.BufferUnpacker;
import org.msgpack.unpacker.Converter;
import org.msgpack.unpacker.Unpacker;
import org.msgpack.unpacker.UnpackerIterator;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import static org.msgpack.template.Templates.tMap;
import static org.msgpack.template.Templates.TString;
import static org.msgpack.template.Templates.TValue;

/**
 * A bit of an experiment.
 */
public class TryRexProSessioned {

    private static final byte[] emptyBindings;

    static {{
        byte [] empty;
        try {
            empty = ConsoleScriptResponseMessage.convertBindingsToByteArray(new RexsterBindings());
        } catch (IOException ioe) {
            empty = new byte[0];
        }

        emptyBindings = empty;
    }};

    public static void main(String[] args) {
        //bigCalls();
        lotsOfCalls(false);
    }

    private static void bigCalls() {

        RemoteRexsterSession session = new RemoteRexsterSession("localhost", 8184, 100, "", "",
                SessionRequestMessage.CHANNEL_MSGPACK);
        session.open();

        long start = System.currentTimeMillis();

        long checkpoint = System.currentTimeMillis();

        try {
            MsgPackScriptResponseMessage resultMessage = (MsgPackScriptResponseMessage) session.sendRequest(
                    createScriptRequestMessage(session, "g=rexster.getGraph('gratefulgraph');g.V;"), 100);
            resultMessage = (MsgPackScriptResponseMessage) session.sendRequest(
                    createScriptRequestMessage(session, "g.E;"), 100);
            System.out.println((checkpoint - start) + ":" + (System.currentTimeMillis() - checkpoint));

        } catch (Exception ex) {
            ex.printStackTrace();
        }


    }

    private static void lotsOfCalls(boolean doJson){

        RemoteRexsterSession session = new RemoteRexsterSession("localhost", 8184, 100, "", "", SessionRequestMessage.CHANNEL_MSGPACK);
        session.open();

        MessagePack msgpack = new MessagePack();

        long start = System.currentTimeMillis();


        long checkpoint = System.currentTimeMillis();

        try {

            MsgPackScriptResponseMessage resultMessage = (MsgPackScriptResponseMessage) session.sendRequest(
                    createScriptRequestMessage(session, "g=rexster.getGraph('gratefulgraph');g.V;"), 100);

            BufferUnpacker unpacker = msgpack.createBufferUnpacker(resultMessage.Results);
            unpacker.setArraySizeLimit(Integer.MAX_VALUE);
            unpacker.setMapSizeLimit(Integer.MAX_VALUE);
            unpacker.setRawSizeLimit(Integer.MAX_VALUE);

            int counter = 1;
            UnpackerIterator itty = unpacker.iterator();
            while (itty.hasNext()){
                final Map<String,Value> map = new Converter(msgpack, itty.next()).read(tMap(TString, TValue));
                final String vId = map.get(Tokens._ID).asRawValue().getString();

                MsgPackScriptResponseMessage vertexResultMessage = (MsgPackScriptResponseMessage) session.sendRequest(
                        createScriptRequestMessage(session, "g.v(" + vId + ")"), 100);

                unpacker = msgpack.createBufferUnpacker(vertexResultMessage.Results);
                System.out.println(unpacker.read(tMap(TString, TValue)));
                counter++;
            }

            /*
            resultMessage = (MsgPackScriptResponseMessage) session.sendRequest(
                    createScriptRequestMessage(session, "g.E;"), 100);

            unpacker = msgpack.createBufferUnpacker(resultMessage.Results);

            itty = unpacker.iterator();
            while (itty.hasNext()){
                final Map<String,Value> map = new Converter(msgpack, itty.next()).read(tMap(TString, TValue));
                final String eId = map.get(Tokens._ID).asRawValue().getString();

                MsgPackScriptResponseMessage edgeResultMessage = (MsgPackScriptResponseMessage) session.sendRequest(
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
        scriptMessage.Bindings = emptyBindings;
        scriptMessage.LanguageName = "groovy";
        scriptMessage.Flag = MessageType.IN_SESSION_SCRIPT_REQUEST;
        scriptMessage.setRequestAsUUID(UUID.randomUUID());
        return scriptMessage;
    }
}