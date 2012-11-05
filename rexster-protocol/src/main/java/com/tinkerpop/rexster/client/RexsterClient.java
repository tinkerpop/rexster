package com.tinkerpop.rexster.client;

import com.tinkerpop.rexster.protocol.BitWorks;
import com.tinkerpop.rexster.protocol.RexPro;
import com.tinkerpop.rexster.protocol.RexsterBindings;
import com.tinkerpop.rexster.protocol.msg.ErrorResponseMessage;
import com.tinkerpop.rexster.protocol.msg.MessageFlag;
import com.tinkerpop.rexster.protocol.msg.MsgPackScriptResponseMessage;
import com.tinkerpop.rexster.protocol.msg.RexProMessage;
import com.tinkerpop.rexster.protocol.msg.ScriptRequestMessage;
import org.msgpack.MessagePack;
import org.msgpack.template.Template;
import org.msgpack.type.Value;
import org.msgpack.unpacker.BufferUnpacker;
import org.msgpack.unpacker.Converter;
import org.msgpack.unpacker.UnpackerIterator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.msgpack.template.Templates.TString;
import static org.msgpack.template.Templates.TValue;
import static org.msgpack.template.Templates.tMap;

/**
 * Basic client for sending Gremlin scripts to Rexster and receiving results as Map objects with String
 * keys and MsgPack Value objects.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class RexsterClient {
    private final MessagePack msgpack = new MessagePack();
    private final List<RexProInfo> rexProInfos = new ArrayList<RexProInfo>();
    private int currentServer = 0;

    private static final byte[] emptyBindings;

    static {{
        byte [] empty;
        try {
            empty = BitWorks.convertSerializableBindingsToByteArray(new RexsterBindings());
        } catch (IOException ioe) {
            empty = new byte[0];
        }

        emptyBindings = empty;
    }}

    public RexsterClient(final String[] hostsAndPorts) {
        for (String hostAndPort : hostsAndPorts) {
            rexProInfos.add(new RexProInfo(hostAndPort));
        }
    }

    public List<Map<String,Value>> gremlin(final String script) throws IOException {
        return gremlin(script, tMap(TString,TValue));
    }

    public <T> List<T> gremlin(final String script, final Template template) throws IOException {
        final RexProInfo server = nextServer();

        final RexProMessage resultMessage = RexPro.sendMessage(server.getHost(), server.getPort(),
                createScriptRequestMessage(script));
        if (resultMessage instanceof MsgPackScriptResponseMessage) {
            final MsgPackScriptResponseMessage msg = (MsgPackScriptResponseMessage) resultMessage;
            final BufferUnpacker unpacker = msgpack.createBufferUnpacker(msg.Results);
            unpacker.setArraySizeLimit(Integer.MAX_VALUE);
            unpacker.setMapSizeLimit(Integer.MAX_VALUE);
            unpacker.setRawSizeLimit(Integer.MAX_VALUE);

            final List<T> results = new ArrayList<T>();
            final UnpackerIterator itty = unpacker.iterator();
            while (itty.hasNext()){
                final T t = (T) new Converter(msgpack, itty.next()).read(template);
                //final Map<String,Value> map = new Converter(msgpack, itty.next()).read(tMap(TString, TValue));
                results.add(t);
            }

            return results;
        } else if (resultMessage instanceof ErrorResponseMessage) {
            throw new IOException(((ErrorResponseMessage) resultMessage).ErrorMessage);
        } else {
            throw new RuntimeException("RexsterClient doesn't support the message type returned.");
        }
    }

    private RexProInfo nextServer() {
        final RexProInfo server = this.rexProInfos.get(this.currentServer);
        this.currentServer++;
        if (this.currentServer >= this.rexProInfos.size()) {
            this.currentServer = 0;
        }

        return server;
    }

    private static ScriptRequestMessage createScriptRequestMessage(final String script) {
        ScriptRequestMessage scriptMessage = new ScriptRequestMessage();
        scriptMessage.Script = script;
        scriptMessage.Bindings = emptyBindings;
        scriptMessage.LanguageName = "groovy";
        scriptMessage.Flag = MessageFlag.SCRIPT_REQUEST_NO_SESSION;
        scriptMessage.setRequestAsUUID(UUID.randomUUID());
        return scriptMessage;
    }

    /*
    public static void main(final String [] args) throws Exception {
        RexsterClient client = new RexsterClient(new String[] {"localhost:8184"});
        List<Map<String, Value>> maps = client.gremlin("g = rexster.getGraph('tinkergraph');g.V", tMap(TString, TValue));
        System.out.println(maps);
        List<String> names = client.gremlin("g = rexster.getGraph('tinkergraph');g.v(1).name", TString);
        System.out.println(names);
    }
    */

    private class RexProInfo {
        private final String host;
        private final int port;

        public RexProInfo(final String host, final int port) {
            this.host = host;
            this.port = port;
        }

        public RexProInfo(final String hostAndPort) {
            final String[] hostPortPair = hostAndPort.split(":");
            this.host = hostPortPair[0];
            this.port = Integer.parseInt(hostPortPair[1]);
        }

        public int getPort() {
            return port;
        }

        public String getHost() {
            return host;
        }
    }
}
