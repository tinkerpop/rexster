package com.tinkerpop.rexster.client;

import com.tinkerpop.rexster.protocol.BitWorks;
import com.tinkerpop.rexster.protocol.RexsterBindings;
import com.tinkerpop.rexster.protocol.msg.ErrorResponseMessage;
import com.tinkerpop.rexster.protocol.msg.MessageFlag;
import com.tinkerpop.rexster.protocol.msg.MsgPackScriptResponseMessage;
import com.tinkerpop.rexster.protocol.msg.RexProMessage;
import com.tinkerpop.rexster.protocol.msg.ScriptRequestMessage;
import org.apache.commons.configuration.Configuration;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.GrizzlyFuture;
import org.glassfish.grizzly.nio.NIOConnection;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.msgpack.MessagePack;
import org.msgpack.template.Template;
import org.msgpack.type.Value;
import org.msgpack.unpacker.BufferUnpacker;
import org.msgpack.unpacker.Converter;
import org.msgpack.unpacker.UnpackerIterator;
import org.omg.CORBA.PRIVATE_MEMBER;

import javax.script.Bindings;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
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

    private static final String DEFAULT_GREMLIN = "groovy";

    private final NIOConnection[] connections;
    private int currentConnection = 0;

    private final MessagePack msgpack = new MessagePack();
    private final int timeoutConnection;
    private final int timeoutWrite;
    private final int timeoutRead;
    private final int retries;
    private final int waitBetweenRetries;
    private final int asyncWriteQueueMaxBytes;
    private final int arraySizeLimit;
    private final int mapSizeLimit;
    private final int rawSizeLimit;

    private final TCPNIOTransport transport;
    private final String[] hosts;
    private final int port;

    protected static ConcurrentHashMap<UUID, ArrayBlockingQueue<Object>> responses = new ConcurrentHashMap<UUID, ArrayBlockingQueue<Object>>();

    protected RexsterClient(final Configuration configuration, final TCPNIOTransport transport) {
        this.timeoutConnection = configuration.getInt(RexsterClientTokens.CONFIG_TIMEOUT_CONNECTION_MS);
        this.timeoutRead = configuration.getInt(RexsterClientTokens.CONFIG_TIMEOUT_READ_MS);
        this.timeoutWrite = configuration.getInt(RexsterClientTokens.CONFIG_TIMEOUT_WRITE_MS);
        this.retries = configuration.getInt(RexsterClientTokens.CONFIG_MESSAGE_RETRY_COUNT);
        this.waitBetweenRetries = configuration.getInt(RexsterClientTokens.CONFIG_MESSAGE_RETRY_WAIT_MS);
        this.asyncWriteQueueMaxBytes = configuration.getInt(RexsterClientTokens.CONFIG_MAX_ASYNC_WRITE_QUEUE_BYTES);
        this.arraySizeLimit = configuration.getInt(RexsterClientTokens.CONFIG_DESERIALIZE_ARRAY_SIZE_LIMIT);
        this.mapSizeLimit = configuration.getInt(RexsterClientTokens.CONFIG_DESERIALIZE_MAP_SIZE_LIMIT);
        this.rawSizeLimit = configuration.getInt(RexsterClientTokens.CONFIG_DESERIALIZE_RAW_SIZE_LIMIT);

        this.transport = transport;
        this.port = configuration.getInt(RexsterClientTokens.CONFIG_PORT);
        final String hostname = configuration.getString(RexsterClientTokens.CONFIG_HOSTNAME);
        this.hosts = hostname.split(",");

        this.connections = new NIOConnection[this.hosts.length];
    }

    public List<Map<String,Value>> gremlin(final String script) throws Exception {
        return gremlin(script, tMap(TString,TValue));
    }

    public <T> List<T> gremlin(final String script, final Template template) throws Exception {
        return gremlin(script, null, template);
    }

    public <T> List<T> gremlin(final String script, final Map<String, Object> scriptArgs, final Template template) throws Exception {
        final ArrayBlockingQueue<Object> responseQueue = new ArrayBlockingQueue<Object>(1);
        final RexProMessage msgToSend = createNoSessionScriptRequest(script, scriptArgs);
        final UUID requestId = msgToSend.requestAsUUID();
        responses.put(requestId, responseQueue);

        this.sendRequest(msgToSend);

        Object resultMessage;
        try {
            final long beginTime = System.currentTimeMillis();
            resultMessage = responseQueue.poll(this.timeoutRead - (System.currentTimeMillis() - beginTime), TimeUnit.MILLISECONDS);
        } catch (Exception ex) {
            responses.remove(requestId);
            throw new IOException(ex);
        }

        responses.remove(requestId);

        if (resultMessage == null) {
            throw new IOException(String.format("Message received response timeoutConnection (%s s)", this.timeoutConnection));
        }

        if (resultMessage instanceof MsgPackScriptResponseMessage) {
            final MsgPackScriptResponseMessage msg = (MsgPackScriptResponseMessage) resultMessage;
            final BufferUnpacker unpacker = msgpack.createBufferUnpacker(msg.Results);
            unpacker.setArraySizeLimit(this.arraySizeLimit);
            unpacker.setMapSizeLimit(this.mapSizeLimit);
            unpacker.setRawSizeLimit(this.rawSizeLimit);

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
            throw new IOException("RexsterClient doesn't support the message type returned.");
        }
    }

    void putResponse(final RexProMessage response) throws Exception {
        final UUID requestId = response.requestAsUUID();
        if (!responses.containsKey(requestId)) {
            // probably a timeout if we get here... ???
            return;
        }

        try {
            final ArrayBlockingQueue<Object> queue = responses.get(requestId);
            if (queue != null) {
                queue.put(response);
            }
            else {
                // no queue for some reason....why ???
            }
        }
        catch (InterruptedException e) {
            // just trap this one ???
        }
    }

    private NIOConnection nextConnection() {
        synchronized(connections) {
            if (currentConnection == Integer.MAX_VALUE) { currentConnection = 0; }
            currentConnection = (currentConnection + 1) % hosts.length;

            final NIOConnection connection = connections[currentConnection];
            if (connection == null || !connection.isOpen()) {
                connections[currentConnection] = openConnection(this.hosts[currentConnection]);
            }

            return connections[currentConnection];
        }
    }

    private NIOConnection openConnection(final String host) {
        try {
            final Future<Connection> future = this.transport.connect(host, port);
            final NIOConnection connection = (NIOConnection) future.get(this.timeoutConnection, TimeUnit.MILLISECONDS);
            connection.setMaxAsyncWriteQueueSize(asyncWriteQueueMaxBytes);
            return connection;
        } catch (Exception e) {
            return null;
        }
    }

    private void sendRequest(final RexProMessage toSend) throws Exception {
        boolean sent = false;
        int tries = this.retries;
        while (tries > 0 && !sent) {
            try {
                final NIOConnection connection = nextConnection();
                if (connection != null && connection.isOpen()) {
                    if (toSend.estimateMessageSize() + connection.getAsyncWriteQueue().spaceInBytes() <= connection.getMaxAsyncWriteQueueSize()) {
                        final GrizzlyFuture future = connection.write(toSend);
                        future.get(this.timeoutWrite, TimeUnit.MILLISECONDS);
                        sent = true;
                    } else {
                        throw new Exception("internal");
                    }
                }
            } catch (Exception ex) {
                tries--;
                final UUID requestId = toSend.requestAsUUID();
                if (tries == 0) {
                    responses.remove(requestId);
                } else {
                    Thread.sleep(this.waitBetweenRetries);
                }
            }
        }

        if (!sent) {
            throw new Exception("Could not send message.");
        }

    }

    public void close() throws IOException {
        for (NIOConnection connection : this.connections) {
            connection.close();
        }

        this.transport.stop();
    }

    private static ScriptRequestMessage createNoSessionScriptRequest(final String script,
                                                                     final Map<String, Object> scriptArguments) throws IOException{
        final Bindings bindings = new RexsterBindings();
        if (scriptArguments != null) {
            bindings.putAll(scriptArguments);
        }

        final ScriptRequestMessage scriptMessage = new ScriptRequestMessage();
        scriptMessage.Script = script;
        scriptMessage.Bindings = BitWorks.convertSerializableBindingsToByteArray(bindings);
        scriptMessage.LanguageName = DEFAULT_GREMLIN;
        scriptMessage.Flag = MessageFlag.SCRIPT_REQUEST_NO_SESSION;
        scriptMessage.setRequestAsUUID(UUID.randomUUID());
        return scriptMessage;
    }
}
