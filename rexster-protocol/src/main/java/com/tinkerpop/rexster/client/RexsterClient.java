package com.tinkerpop.rexster.client;

import com.tinkerpop.rexster.protocol.BitWorks;
import com.tinkerpop.rexster.protocol.RexPro;
import com.tinkerpop.rexster.protocol.RexsterBindings;
import com.tinkerpop.rexster.protocol.msg.MessageFlag;
import com.tinkerpop.rexster.protocol.msg.MsgPackScriptResponseMessage;
import com.tinkerpop.rexster.protocol.msg.ScriptRequestMessage;
import org.msgpack.MessagePack;
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

    public List<Map<String, Value>> gremlin(final String script) throws IOException {
        final RexProInfo server = nextServer();

        final MsgPackScriptResponseMessage resultMessage = (MsgPackScriptResponseMessage)
                RexPro.sendMessage(server.getHost(), server.getPort(),
                createScriptRequestMessage(script));

        final BufferUnpacker unpacker = msgpack.createBufferUnpacker(resultMessage.Results);
        unpacker.setArraySizeLimit(Integer.MAX_VALUE);
        unpacker.setMapSizeLimit(Integer.MAX_VALUE);
        unpacker.setRawSizeLimit(Integer.MAX_VALUE);

        final List<Map<String, Value>> results = new ArrayList<Map<String, Value>>();
        final UnpackerIterator itty = unpacker.iterator();
        while (itty.hasNext()){
            final Map<String,Value> map = new Converter(msgpack, itty.next()).read(tMap(TString, TValue));
            results.add(map);
        }

        return results;
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
