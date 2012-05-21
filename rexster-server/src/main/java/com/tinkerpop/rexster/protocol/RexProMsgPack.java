package com.tinkerpop.rexster.protocol;

import com.tinkerpop.blueprints.impls.rexster.RestHelper;
import com.tinkerpop.blueprints.impls.rexster.RexsterAuthentication;
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

/**
 * A bit of an experiment.
 */
public class RexProMsgPack {
    
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
       
        long start = System.currentTimeMillis();

        RestHelper.Authentication = new RexsterAuthentication(null, null);

        JSONObject restResult = RestHelper.get("http://localhost:8182/graphs/gratefulgraph/tp/gremlin?script=g.V");
        JSONArray restVertices = restResult.optJSONArray(Tokens.RESULTS);
        for (int ix =0; ix < restVertices.length(); ix++)  {
            JSONObject restVertex = RestHelper.get("http://localhost:8182/graphs/gratefulgraph/tp/gremlin?script=g.v(" + restVertices.optJSONObject(ix).optString(Tokens._ID) + ")");
            System.out.println(restVertex);
        }

        restResult = RestHelper.get("http://localhost:8182/graphs/gratefulgraph/tp/gremlin?script=g.E");
        JSONArray restEdges = restResult.optJSONArray(Tokens.RESULTS);
        for (int ix =0; ix < restEdges.length(); ix++)  {
            JSONObject restEdge = RestHelper.get("http://localhost:8182/graphs/gratefulgraph/tp/gremlin?script=g.e(" + restEdges.optJSONObject(ix).optString(Tokens._ID) + ")");
            System.out.println(restEdge);
        }

        long checkpoint = System.currentTimeMillis();
        
        try {
            RemoteRexsterSession session = new RemoteRexsterSession("localhost", 8184, 100, "", "", SessionRequestMessage.CHANNEL_MSGPACK);
            session.open();

            MsgPackScriptResponseMessage resultMessage = (MsgPackScriptResponseMessage) session.sendRequest(
                    createScriptRequestMessage(session, "g=rexster.getGraph('gratefulgraph');g.V;"), 100);

            MessagePack msgpack = new MessagePack();

            Unpacker unpacker = msgpack.createUnpacker(new ByteArrayInputStream(resultMessage.Results));
            
            Iterator<Value> itty = unpacker.iterator();
            unpacker.close();
            while (itty.hasNext()){
                final Map<String,Value> map = new Converter(itty.next()).read(tMap(TString, TValue));
                final String vId = map.get("id").asRawValue().getString();

                MsgPackScriptResponseMessage vertexResultMessage = (MsgPackScriptResponseMessage) session.sendRequest(
                        createScriptRequestMessage(session, "g.v(" + vId + ")"), 100);

                unpacker = msgpack.createUnpacker(new ByteArrayInputStream(vertexResultMessage.Results));
                Value valueVertex = unpacker.iterator().next();
                unpacker.close();

                System.out.println(new Converter(valueVertex).read(tMap(TString, TValue)));
            }

            resultMessage = (MsgPackScriptResponseMessage) session.sendRequest(
                    createScriptRequestMessage(session, "g.E;"), 100);

            unpacker = msgpack.createUnpacker(new ByteArrayInputStream(resultMessage.Results));

            itty = unpacker.iterator();
            unpacker.close();
            while (itty.hasNext()){
                final Map<String,Value> map = new Converter(itty.next()).read(tMap(TString, TValue));
                final String eId = map.get("id").asRawValue().getString();

                MsgPackScriptResponseMessage edgeResultMessage = (MsgPackScriptResponseMessage) session.sendRequest(
                        createScriptRequestMessage(session, "g.e(" + eId + ")"), 100);

                unpacker = msgpack.createUnpacker(new ByteArrayInputStream(edgeResultMessage.Results));
                Value valueEdge = unpacker.iterator().next();
                unpacker.close();

                System.out.println(new Converter(valueEdge).read(tMap(TString, TValue)));
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
        scriptMessage.Bindings = emptyBindings;
        scriptMessage.LanguageName = "groovy";
        scriptMessage.Flag = (byte) 0;
        scriptMessage.setRequestAsUUID(UUID.randomUUID());
        return scriptMessage;
    }
}
