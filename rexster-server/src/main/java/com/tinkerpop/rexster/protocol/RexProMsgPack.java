package com.tinkerpop.rexster.protocol;

import com.tinkerpop.blueprints.pgm.impls.rexster.util.RestHelper;
import com.tinkerpop.blueprints.pgm.impls.rexster.util.RexsterAuthentication;
import com.tinkerpop.rexster.Tokens;
import com.tinkerpop.rexster.protocol.msg.ConsoleScriptResponseMessage;
import com.tinkerpop.rexster.protocol.msg.MsgPackScriptResponseMessage;
import com.tinkerpop.rexster.protocol.msg.ScriptRequestMessage;
import com.tinkerpop.rexster.protocol.msg.SessionRequestMessage;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.msgpack.MessagePack;
import org.msgpack.type.Value;
import org.msgpack.unpacker.Converter;
import org.msgpack.unpacker.Unpacker;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import static org.msgpack.template.Templates.tMap;
import static org.msgpack.template.Templates.TString;
import static org.msgpack.template.Templates.TValue;

public class RexProMsgPack {
    
    public static void main(String[] args) {
       
        long start = System.currentTimeMillis();
        
        RestHelper.Authentication = new RexsterAuthentication(null, null);
        /*
        JSONObject restResult = RestHelper.get("http://localhost:8182/graphs/gratefulgraph/vertices");
        JSONArray restVertices = restResult.optJSONArray(Tokens.RESULTS);
        for (int ix =0; ix < restVertices.length(); ix++)  {
            JSONObject restVertex = RestHelper.get("http://localhost:8182/graphs/gratefulgraph/vertices/" + restVertices.optJSONObject(ix).optString(Tokens._ID));
            System.out.println(restVertex);
        }

        restResult = RestHelper.get("http://localhost:8182/graphs/gratefulgraph/edges");
        JSONArray restEdges = restResult.optJSONArray(Tokens.RESULTS);
        for (int ix =0; ix < restEdges.length(); ix++)  {
            JSONObject restEdge = RestHelper.get("http://localhost:8182/graphs/gratefulgraph/edges/" + restEdges.optJSONObject(ix).optString(Tokens._ID));
            System.out.println(restEdge);
        }
        */
        long checkpoint = System.currentTimeMillis();
        
        try {
            RemoteRexsterSession session = new RemoteRexsterSession("localhost", 8184, 100, "", "", SessionRequestMessage.CHANNEL_MSGPACK);
            session.open();

            ScriptRequestMessage scriptMessage = createScriptRequestMessage(session, "g=rexster.getGraph('gratefulgraph');g.V;");
            MsgPackScriptResponseMessage resultMessage = (MsgPackScriptResponseMessage) session.sendRequest(scriptMessage, 100);

            MessagePack msgpack = new MessagePack();

            ByteArrayInputStream in = new ByteArrayInputStream(resultMessage.Results);
            Unpacker unpacker = msgpack.createUnpacker(in);
            
            Iterator<Value> itty = unpacker.iterator();
            while (itty.hasNext()){
                Value value = msgpack.read(itty.next().asRawValue().getByteArray());
                Map<String,Value> map = new Converter(value).read(tMap(TString, TValue));
                
                //System.out.println(map);
                
                ScriptRequestMessage vertexScriptMessage = createScriptRequestMessage(session, "g.v(" + map.get("id") + ")");
                MsgPackScriptResponseMessage vertexResultMessage = (MsgPackScriptResponseMessage) session.sendRequest(vertexScriptMessage, 100);

                ByteArrayInputStream vertexIn = new ByteArrayInputStream(vertexResultMessage.Results);
                Unpacker unpackerVertex = msgpack.createUnpacker(vertexIn);

                Value valueVertex = msgpack.read(unpackerVertex.iterator().next().asRawValue().getByteArray());
                Map<String,Value> mapVertex = new Converter(valueVertex).read(tMap(TString, TValue));

                System.out.println(mapVertex);
            }

            scriptMessage = createScriptRequestMessage(session, "g.E;");
            resultMessage = (MsgPackScriptResponseMessage) session.sendRequest(scriptMessage, 100);

            in = new ByteArrayInputStream(resultMessage.Results);
            unpacker = msgpack.createUnpacker(in);

            itty = unpacker.iterator();
            while (itty.hasNext()){
                Value value = msgpack.read(itty.next().asRawValue().getByteArray());
                Map<String,Value> map = new Converter(value).read(tMap(TString, TValue));

                //System.out.println(map);

                ScriptRequestMessage edgeScriptMessage = createScriptRequestMessage(session, "g.e(" + map.get("id") + ")");
                MsgPackScriptResponseMessage edgeResultMessage = (MsgPackScriptResponseMessage) session.sendRequest(edgeScriptMessage, 100);

                ByteArrayInputStream edgeIn = new ByteArrayInputStream(edgeResultMessage.Results);
                Unpacker unpackerEdge = msgpack.createUnpacker(edgeIn);

                Value valueEdge = msgpack.read(unpackerEdge.iterator().next().asRawValue().getByteArray());
                Map<String,Value> mapEdge = new Converter(valueEdge).read(tMap(TString, TValue));

                System.out.println(mapEdge);
            }
            
            System.out.println((checkpoint - start) + ":" + (System.currentTimeMillis() - checkpoint));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static ScriptRequestMessage createScriptRequestMessage(RemoteRexsterSession session, String script) throws IOException {
        ScriptRequestMessage scriptMessage = new ScriptRequestMessage();
        scriptMessage.setSessionAsUUID(session.getSessionKey());
        scriptMessage.Script = script;
        scriptMessage.Bindings = ConsoleScriptResponseMessage.convertBindingsToByteArray(new RexsterBindings());
        scriptMessage.LanguageName = "groovy";
        scriptMessage.Flag = (byte) 0;
        scriptMessage.setRequestAsUUID(UUID.randomUUID());
        return scriptMessage;
    }
}
