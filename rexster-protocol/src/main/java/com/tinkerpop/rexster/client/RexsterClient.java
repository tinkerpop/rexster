package com.tinkerpop.rexster.client;

import com.tinkerpop.rexster.protocol.BitWorks;
import com.tinkerpop.rexster.protocol.RexsterBindings;
import com.tinkerpop.rexster.protocol.msg.ErrorResponseMessage;
import com.tinkerpop.rexster.protocol.msg.MessageFlag;
import com.tinkerpop.rexster.protocol.msg.MsgPackScriptResponseMessage;
import com.tinkerpop.rexster.protocol.msg.RexProMessage;
import com.tinkerpop.rexster.protocol.msg.ScriptRequestMessage;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.GrizzlyFuture;
import org.glassfish.grizzly.Transport;
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
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
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
    private String host;
    private int port;
    private Connection<Object> connection;
    private int timeout;
    private Transport transport;

    protected static ConcurrentHashMap<UUID, ArrayBlockingQueue<Object>> responses =
            new ConcurrentHashMap<UUID, ArrayBlockingQueue<Object>>();

    public RexsterClient(final String host, final int port, final int timeout,
                         final Connection<Object> connection, final Transport transport) {
        this.timeout = timeout;
        this.host = host;
        this.port = port;
        this.connection = connection;
        this.transport = transport;
    }

    private void sendRequest(final RexProMessage toSend) throws Exception {
        final GrizzlyFuture future = connection.write(toSend);
        future.get(this.timeout, TimeUnit.SECONDS);
    }
    public List<Map<String,Value>> gremlin(final String script) throws Exception {
        return gremlin(script, tMap(TString,TValue));
    }

    public <T> List<T> gremlin(final String script, final Template template) throws Exception {
        return gremlin(script, null, template);
    }

    public <T> List<T> gremlin(final String script, final Map<String, Object> scriptArgs, final Template template) throws Exception {
        final long beginTime = System.currentTimeMillis();
        final ArrayBlockingQueue<Object> responseQueue = new ArrayBlockingQueue<Object>(1);
        final RexProMessage msgToSend = createScriptRequestMessage(script, scriptArgs);
        final UUID requestId = msgToSend.requestAsUUID();
        responses.put(requestId, responseQueue);

        try {
            this.sendRequest(msgToSend);
        } catch (Exception e) {
            responses.remove(requestId);
            throw e;
        }

        Object resultMessage = null;
        try {
            resultMessage = responseQueue.poll((this.timeout * 1000) - (System.currentTimeMillis() - beginTime), TimeUnit.MILLISECONDS);
        } catch (Exception ex) {
            responses.remove(requestId);
            throw ex;
        }

        responses.remove(requestId);

        if (resultMessage == null) {
            throw new RuntimeException("NULL GUY");
        }

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

    public void putResponse(RexProMessage response) throws Exception {
        final UUID requestId = response.requestAsUUID();
        if (!responses.containsKey(requestId)) {
            // LOGGER.warn("give up the response,request id is:" + wrapper.getRequestId() + ",maybe because timeout!");
            System.out.println("WTF");
            return;
        }
        try {
            final ArrayBlockingQueue<Object> queue = responses.get(requestId);
            if (queue != null) {
                queue.put(response);
            }
            else {
                //LOGGER.warn("give up the response,request id is:" + requestId + ",because queue is null");
            }
        }
        catch (InterruptedException e) {
            //LOGGER.error("put response error,request id is:" + wrapper.getRequestId(), e);
        }
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public int getTimeout() {
        return timeout;
    }

    public void close() throws IOException {
        this.connection.close();
        this.transport.stop();
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
}
