package com.tinkerpop.rexster.protocol;

import com.tinkerpop.rexster.protocol.msg.ConsoleScriptResponseMessage;
import com.tinkerpop.rexster.protocol.msg.MsgPackScriptResponseMessage;
import com.tinkerpop.rexster.protocol.msg.RexProMessage;
import com.tinkerpop.rexster.protocol.msg.ScriptRequestMessage;
import com.tinkerpop.rexster.protocol.msg.SessionRequestMessage;
import org.msgpack.MessagePack;
import org.msgpack.type.Value;
import org.msgpack.unpacker.Converter;
import org.msgpack.unpacker.Unpacker;

import java.io.ByteArrayInputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.msgpack.template.Templates.tList;
import static org.msgpack.template.Templates.tMap;
import static org.msgpack.template.Templates.TString;
import static org.msgpack.template.Templates.TValue;

public class RexProMsgPack {
    
    public static void main(String[] args) {
        try {
            RemoteRexsterSession session = new RemoteRexsterSession("localhost", 8184, 100, "", "", SessionRequestMessage.CHANNEL_MSGPACK);
            session.open();

            ScriptRequestMessage scriptMessage = new ScriptRequestMessage();
            scriptMessage.setSessionAsUUID(session.getSessionKey());
            scriptMessage.Script = "g=rexster.getGraph('tinkergraph');g.V;";
            scriptMessage.Bindings = ConsoleScriptResponseMessage.convertBindingsToByteArray(new RexsterBindings());
            scriptMessage.LanguageName = "groovy";
            scriptMessage.Flag = (byte) 0;
            scriptMessage.setRequestAsUUID(UUID.randomUUID());

            MsgPackScriptResponseMessage resultMessage = (MsgPackScriptResponseMessage) session.sendRequest(scriptMessage, 100);

            MessagePack msgpack = new MessagePack();

            ByteArrayInputStream in = new ByteArrayInputStream(resultMessage.Results);
            Unpacker unpacker = msgpack.createUnpacker(in);
            
            Iterator<Value> itty = unpacker.iterator();
            while (itty.hasNext()){
                Value value = msgpack.read(itty.next().asRawValue().getByteArray());
                Map<String,Value> map = new Converter(value).read(tMap(TString, TValue));
                System.out.println(map);
            }
            
            
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
