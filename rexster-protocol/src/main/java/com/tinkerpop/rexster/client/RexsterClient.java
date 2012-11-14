package com.tinkerpop.rexster.client;

import com.tinkerpop.rexster.protocol.BitWorks;
import com.tinkerpop.rexster.protocol.RexPro;
import com.tinkerpop.rexster.protocol.RexsterBindings;
import com.tinkerpop.rexster.protocol.filter.RexProMessageFilter;
import com.tinkerpop.rexster.protocol.msg.ErrorResponseMessage;
import com.tinkerpop.rexster.protocol.msg.MessageFlag;
import com.tinkerpop.rexster.protocol.msg.MsgPackScriptResponseMessage;
import com.tinkerpop.rexster.protocol.msg.RexProMessage;
import com.tinkerpop.rexster.protocol.msg.ScriptRequestMessage;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.GrizzlyFuture;
import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;
import org.glassfish.grizzly.filterchain.TransportFilter;
import org.glassfish.grizzly.impl.FutureImpl;
import org.glassfish.grizzly.impl.SafeFutureImpl;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.nio.transport.TCPNIOTransportBuilder;
import org.msgpack.MessagePack;
import org.msgpack.template.Template;
import org.msgpack.type.Value;
import org.msgpack.unpacker.BufferUnpacker;
import org.msgpack.unpacker.Converter;
import org.msgpack.unpacker.UnpackerIterator;

import javax.script.Bindings;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

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
    private int timeout;

    private final TCPNIOTransport transport = TCPNIOTransportBuilder.newInstance().build();
    private final TransportFilter transportFilter = new TransportFilter();
    private final RexProMessageFilter rpFilter = new RexProMessageFilter();

    public RexsterClient(final String[] hostsAndPorts) {
        this(hostsAndPorts, RexPro.DEFAULT_TIMEOUT_SECONDS);
    }

    public RexsterClient(final String[] hostsAndPorts, final int timeout) {
        this.timeout = timeout;
        for (String hostAndPort : hostsAndPorts) {
            rexProInfos.add(new RexProInfo(hostAndPort));
        }
    }

    public List<Map<String,Value>> gremlin(final String script) throws IOException {
        return gremlin(script, tMap(TString,TValue));
    }

    public <T> List<T> gremlin(final String script, final Template template) throws IOException {
        return gremlin(script, null, template);
    }

    public <T> List<T> gremlin(final String script, final Map<String, Object> scriptArgs, final Template template) throws IOException {
        final RexProInfo server = nextServer();

        final RexProMessage resultMessage = this.sendMessage(server.getHost(), server.getPort(),
                createScriptRequestMessage(script, scriptArgs));
        if (resultMessage instanceof MsgPackScriptResponseMessage) {
            final MsgPackScriptResponseMessage msg = (MsgPackScriptResponseMessage) resultMessage;
            final BufferUnpacker unpacker = msgpack.createBufferUnpacker(msg.Results);
            unpacker.setArraySizeLimit(Integer.MAX_VALUE);
            unpacker.setMapSizeLimit(Integer.MAX_VALUE);
            unpacker.setRawSizeLimit(Integer.MAX_VALUE);

            final List<T> results = new ArrayList<T>();
            final UnpackerIterator itty = unpacker.iterator();
            while (itty.hasNext()){
                final Converter converter = new Converter(msgpack, itty.next());
                final T t = (T) converter.read(template);
                converter.close();
                results.add(t);
            }

            unpacker.close();

            return results;
        } else if (resultMessage instanceof ErrorResponseMessage) {
            throw new IOException(((ErrorResponseMessage) resultMessage).ErrorMessage);
        } else {
            throw new RuntimeException("RexsterClient doesn't support the message type returned.");
        }
    }

    private RexProMessage sendMessage(final String host, final int port, final RexProMessage toSend) throws IOException {

        final FutureImpl<RexProMessage> sessionMessageFuture = SafeFutureImpl.create();

        // Create a FilterChain using FilterChainBuilder
        final FilterChainBuilder filterChainBuilder = FilterChainBuilder.stateless();

        // Add TransportFilter, which is responsible
        // for reading and writing data to the connection
        filterChainBuilder.add(transportFilter);
        filterChainBuilder.add(rpFilter);
        filterChainBuilder.add(new CustomClientFilter(toSend, sessionMessageFuture));

        transport.setProcessor(filterChainBuilder.build());

        Connection connection = null;

        try {
            this.transport.start();

            // Connect client to the server
            final GrizzlyFuture<Connection> futureConnect = transport.connect(host, port);
            connection = futureConnect.get(this.timeout, TimeUnit.SECONDS);
            return sessionMessageFuture.get(this.timeout, TimeUnit.SECONDS);

        } catch (Exception e) {
            if (connection == null) {
                throw new RuntimeException("Can not connect via RexPro - " + e.getMessage(), e);
            } else {
                throw new RuntimeException("Request [" + toSend.getClass().getName() + "] to Rexster failed [" + host + ":" + port + "] - " + e.getMessage(), e);
            }
        } finally {
            if (connection != null) {
                connection.close();
            }

            transport.stop();
        }
    }

    private RexProInfo nextServer() {
        synchronized(rexProInfos) {
            if (currentServer == Integer.MAX_VALUE) { currentServer = 0; }
            currentServer = (currentServer + 1) % rexProInfos.size();
            return rexProInfos.get(currentServer);
        }
    }

    private static ScriptRequestMessage createScriptRequestMessage(final String script,
                                                                   final Map<String, Object> scriptArguments) throws IOException{
        final Bindings bindings = new RexsterBindings();
        if (scriptArguments != null) {
            bindings.putAll(scriptArguments);
        }

        final ScriptRequestMessage scriptMessage = new ScriptRequestMessage();
        scriptMessage.Script = script;
        scriptMessage.Bindings = BitWorks.convertSerializableBindingsToByteArray(bindings);
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

    private final class CustomClientFilter extends BaseFilter {
        private final FutureImpl<RexProMessage> resultFuture;
        private final RexProMessage toSend;

        public CustomClientFilter(final RexProMessage toSend, final FutureImpl<RexProMessage> resultFuture) {
            this.resultFuture = resultFuture;
            this.toSend = toSend;
        }

        @Override
        public NextAction handleConnect(FilterChainContext ctx) throws IOException {
            ctx.write(this.toSend);
            return ctx.getStopAction();
        }

        @Override
        public NextAction handleRead(FilterChainContext ctx) throws IOException {
            final RexProMessage message = ctx.getMessage();
            resultFuture.result(message);
            return ctx.getStopAction();
        }
    }
}
