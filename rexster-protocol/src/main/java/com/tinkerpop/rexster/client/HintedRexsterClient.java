package com.tinkerpop.rexster.client;

import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.rexster.config.hinted.HintedGraphs;
import com.tinkerpop.rexster.config.hinted.ElementRange;
import com.tinkerpop.rexster.protocol.msg.ErrorResponseMessage;
import com.tinkerpop.rexster.protocol.msg.RexProMessage;
import com.tinkerpop.rexster.protocol.msg.ScriptRequestMessage;
import com.tinkerpop.rexster.protocol.msg.ScriptResponseMessage;
import com.tinkerpop.rexster.protocol.serializer.msgpack.MsgPackSerializer;
import org.apache.commons.configuration.Configuration;
import org.apache.log4j.Logger;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.GrizzlyFuture;
import org.glassfish.grizzly.nio.NIOConnection;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.jgroups.Address;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;
import org.jgroups.util.PayloadUUID;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Basic client for sending Gremlin scripts to Rexster and receiving results as Map objects with String
 * keys and MsgPack Value objects. This client is only for sessionless communication with Rexster and
 * therefore all Gremlin scripts sent as requests to Rexster should be careful to handle their own
 * transactional semantics.  In other words, do not count on sending a script that mutates some aspect of the
 * graph in one request and then a second request later to commit the transaction as there is no guarantee that
 * the transaction will be handled properly.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 * @author Blake Eggleston (bdeggleston.github.com)
 */
public class HintedRexsterClient {
    private static final Logger logger = Logger.getLogger(RexsterClient.class);

    //private final NIOConnection[] connections;
    private final RexsterConnections connections = new RexsterConnections();
    private int currentConnection = 0;

    private final int timeoutConnection;
    private final int timeoutWrite;
    private final int timeoutRead;
    private final int retries;
    private final int waitBetweenRetries;
    private final int asyncWriteQueueMaxBytes;
    private final String language;
    private final String graphName;
    private final String graphObjName;
    private final boolean transaction;

    private JChannel channel;

    private final TCPNIOTransport transport;
    private final String[] hosts;
    private final int port;
    private byte serializer;

    protected static ConcurrentHashMap<UUID, ArrayBlockingQueue<Object>> responses = new ConcurrentHashMap<UUID, ArrayBlockingQueue<Object>>();

    protected HintedRexsterClient(final Configuration configuration, final TCPNIOTransport transport) {
        this.timeoutConnection = configuration.getInt(RexsterClientTokens.CONFIG_TIMEOUT_CONNECTION_MS);
        this.timeoutRead = configuration.getInt(RexsterClientTokens.CONFIG_TIMEOUT_READ_MS);
        this.timeoutWrite = configuration.getInt(RexsterClientTokens.CONFIG_TIMEOUT_WRITE_MS);
        this.retries = configuration.getInt(RexsterClientTokens.CONFIG_MESSAGE_RETRY_COUNT);
        this.waitBetweenRetries = configuration.getInt(RexsterClientTokens.CONFIG_MESSAGE_RETRY_WAIT_MS);
        this.asyncWriteQueueMaxBytes = configuration.getInt(RexsterClientTokens.CONFIG_MAX_ASYNC_WRITE_QUEUE_BYTES);
        this.language = configuration.getString(RexsterClientTokens.CONFIG_LANGUAGE);
        this.graphName = configuration.getString(RexsterClientTokens.CONFIG_GRAPH_NAME);
        this.graphObjName = configuration.getString(RexsterClientTokens.CONFIG_GRAPH_OBJECT_NAME);
        this.transaction= configuration.getBoolean(RexsterClientTokens.CONFIG_TRANSACTION);

        this.transport = transport;
        this.port = configuration.getInt(RexsterClientTokens.CONFIG_PORT);
        this.hosts = configuration.getStringArray(RexsterClientTokens.CONFIG_HOSTNAME);
        this.serializer = configuration.getByte(RexsterClientTokens.CONFIG_SERIALIZER, MsgPackSerializer.SERIALIZER_ID);

        //this.connections = new NIOConnection[this.hosts.length];

        try {
            final String stack = System.getProperty("rexster.jgroups", JChannel.DEFAULT_PROTOCOL_STACK);
            this.channel = new JChannel(stack);
            this.channel.setReceiver(new RexsterClusterReceiver());
            this.channel.connect("rexster");
            logger.debug("JChannel self address: " + this.channel.getAddressAsString());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Sends a RexProMessage, and returns the received RexProMessage response.
     *
     * This method is for low-level operations with RexPro only.
     *
     * @param rawMessage message to send.
     */
    public RexProMessage execute(final RexProMessage rawMessage) throws RexProException, IOException {
        final ArrayBlockingQueue<Object> responseQueue = new ArrayBlockingQueue<Object>(1);
        final UUID requestId = rawMessage.requestAsUUID();
        responses.put(requestId, responseQueue);
        try {
            this.sendRequest(rawMessage, null);
        } catch (Throwable t) {
            throw new IOException(t);
        }

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
        } else if (!(resultMessage instanceof RexProMessage)) {
            logger.error(String.format("Rexster returned a message of type [%s]", resultMessage.getClass().getName()));
            throw new RexProException("RexsterClient doesn't support the message type returned.");
        }

        return (RexProMessage) resultMessage;
    }

    /**
     * Send a script to a RexPro Server for execution and return the result.  No bindings are specified.
     *
     * @param script the script to execute
     */
    public <T> List<T> execute(final String script) throws RexProException, IOException {
        return execute(script, null);
    }

    public <T> List<T> execute(final String script, final Map<String, Object> scriptArgs) throws RexProException, IOException {
        return execute(script, scriptArgs, null);
    }

    /**
     * Send a script to a RexPro Server for execution and return the result.
     *
     * Be sure that arguments sent are serializable by MsgPack or the object will not be bound properly on the
     * server.  For example a complex object like java.util.Date will simply be serialized via toString and
     * therefore will be referenced as such when accessed via the Gremlin script.
     *
     * @param script the script to execute
     * @param scriptArgs the map becomes bindings.
     */
    public <T> List<T> execute(final String script, final Map<String, Object> scriptArgs,
                               final Hint hint) throws RexProException, IOException {
        final ArrayBlockingQueue<Object> responseQueue = new ArrayBlockingQueue<Object>(1);
        final RexProMessage msgToSend = createNoSessionScriptRequest(script, scriptArgs);
        final UUID requestId = msgToSend.requestAsUUID();
        responses.put(requestId, responseQueue);

        try {

            this.sendRequest(msgToSend, hint);
        } catch (Throwable t) {
            throw new IOException(t);
        }

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

        if (resultMessage instanceof ScriptResponseMessage) {
            final ScriptResponseMessage msg = (ScriptResponseMessage) resultMessage;

            // when rexster returns an iterable it's read out of the unpacker as a single object much like a single
            // vertex coming back from rexster.  basically, this is the difference between g.v(1) and g.v(1).map.
            // the latter returns an iterable essentially putting a list inside of the results list here on the
            // client side. the idea here is to normalize all results to a list on the client side, and therefore,
            // iterables like those from g.v(1).map need to be unrolled into the results list.  Prefer this to
            // doing it on the server, because the server should return what is asked of it, in case other clients
            // want to process this differently.
            final List<T> results = new ArrayList<T>();
            if (null != msg && null != msg.Results && null != msg.Results.get()) {
                Object maybeIter = msg.Results.get();
                if (maybeIter instanceof Iterable) {
                    final Iterator<T> itty = ((Iterable) maybeIter).iterator();
                    while(itty.hasNext()) {
                        results.add(itty.next());
                    }
                } else {
                    results.add((T)maybeIter);
                }
            }

            return results;

        } else if (resultMessage instanceof ScriptResponseMessage) {
            final ScriptResponseMessage msg = (ScriptResponseMessage) resultMessage;
            final List<T> results = new ArrayList<T>();
            for (String line : (String[]) msg.Results.get()) {
                results.add((T) line);
            }
            return results;
        }else if (resultMessage instanceof ErrorResponseMessage) {
            logger.warn(String.format("Rexster returned an error response for [%s] with params [%s]",
                    script, scriptArgs));
            throw new RexProException(((ErrorResponseMessage) resultMessage).ErrorMessage);
        } else {
            logger.error(String.format("Rexster returned a message of type [%s]", resultMessage.getClass().getName()));
            throw new RexProException("RexsterClient doesn't support the message type returned.");
        }
    }

    static void putResponse(final RexProMessage response) throws Exception {
        final UUID requestId = response.requestAsUUID();
        if (!responses.containsKey(requestId)) {
            // probably a timeout if we get here... ???
            logger.warn(String.format("No queue found in the response map: %s", requestId));
            return;
        }

        try {
            final ArrayBlockingQueue<Object> queue = responses.get(requestId);
            if (queue != null) {
                queue.put(response);
            }
            else {
                // no queue for some reason....why ???
                logger.error(String.format("No queue found in the response map: %s", requestId));
            }
        }
        catch (InterruptedException e) {
            // just trap this one ???
            logger.error("Error reading the queue in the response map.", e);
        }
    }

    private NIOConnection nextRoundRobinConnection() {
        synchronized(connections) {
            if (currentConnection == Integer.MAX_VALUE) { currentConnection = 0; }
            currentConnection = (currentConnection + 1) % connections.size();
            return connections.getAt(currentConnection).getNioConnection();
        }
    }

    private NIOConnection openConnection(final String host) {
        try {
            final Future<Connection> future = this.transport.connect(host, port);
            final NIOConnection connection = (NIOConnection) future.get(this.timeoutConnection, TimeUnit.MILLISECONDS);
            connection.setMaxAsyncWriteQueueSize(asyncWriteQueueMaxBytes);
            return connection;
        } catch (Exception e) {
            logger.warn(String.format("Failed to open connection to %s:%d", host, port), e);
            return null;
        }
    }

    private void sendRequest(final RexProMessage toSend, final Hint hint) throws Exception {
        boolean sent = false;
        int tries = this.retries;
        while (tries > 0 && !sent) {
            try {
                final NIOConnection connection;
                if (hint == null)
                    connection = nextRoundRobinConnection();
                else
                    connection = connections.best(hint, this.retries - tries);

                if (connection != null && connection.isOpen()) {
                    final GrizzlyFuture future = connection.write(new RexsterClient.MessageContainer(serializer, toSend));
                    future.get(this.timeoutWrite, TimeUnit.MILLISECONDS);
                    sent = true;
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
        //RexsterClientFactory.removeClient(this);
        this.channel.close();
    }

    private ScriptRequestMessage createNoSessionScriptRequest(final String script,
                                                              final Map<String, Object> scriptArguments) throws IOException, RexProException {
        final ScriptRequestMessage scriptMessage = new ScriptRequestMessage();
        scriptMessage.Script = script;
        scriptMessage.LanguageName = this.language;
        scriptMessage.metaSetGraphName(this.graphName);
        scriptMessage.metaSetGraphObjName(this.graphObjName);
        scriptMessage.metaSetInSession(false);
        scriptMessage.metaSetTransaction(this.transaction);
        scriptMessage.setRequestAsUUID(UUID.randomUUID());

        scriptMessage.validateMetaData();

        //attach bindings
        if (scriptArguments != null) {
            scriptMessage.Bindings.putAll(scriptArguments);
        }

        return scriptMessage;
    }

    public byte getSerializer() {
        return serializer;
    }

    public void setSerializer(byte serializer) {
        this.serializer = serializer;
    }

    private class RexsterClusterReceiver extends ReceiverAdapter {
        @Override
        public void viewAccepted(final View view) {
            for(Address addy : view.getMembers()) {
                connections.tryToAdd(addy);
            }
        }

        @Override
        public void receive(final Message msg) {
            // send updates on ranges
            connections.update(msg.getSrc(), (HintedGraphs) msg.getObject());
        }

        @Override
        public void suspect(final Address mbr) {
            // maybe dead???
            System.out.println("DEAD:? " + mbr);
        }
    }

    class RexsterConnection {
        private final Address address;
        private final String host;
        private NIOConnection nioConnection;
        private HintedGraphs hintedGraphs;

        public RexsterConnection(final Address address,
                                 final String host) {
            this.address = address;
            this.host = host;
            this.nioConnection = openConnection(host);
        }

        public Address getAddress() {
            return address;
        }

        public NIOConnection getNioConnection() {
            return nioConnection;
        }

        public HintedGraphs getHintedGraphs() {
            return hintedGraphs;
        }

        public String getHost() {
            return host;
        }
    }

    class RexsterConnections {
        private final Map<Address, RexsterConnection> connections = new ConcurrentHashMap<Address, RexsterConnection>();

        public synchronized void tryToAdd(final Address address) {
            System.out.println("Attempting add of address " + address);
            if (address instanceof PayloadUUID) {
                final PayloadUUID payloadUUID = (PayloadUUID) address;
                final String payload = payloadUUID.getPayload();
                if (payload.startsWith("server")) {
                    // todo: invalid format????
                    final String hostPort = payload.substring(payload.indexOf("-") + 1);
                    final String[] pair = hostPort.split(":");
                    if (!connections.containsKey(address)) {
                        connections.put(address, new RexsterConnection(address, pair[0]));
                    }
                }
            }
        }

        public synchronized void update(final Address address, final HintedGraphs graphs) {
            if (connections.containsKey(address)) {
                final RexsterConnection connection = connections.get(address);
                connection.hintedGraphs = graphs;
            }
        }

        public RexsterConnection getAt(final int index) {
            return (RexsterConnection) connections.values().toArray()[index];
        }

        public int size() {
            return connections.size();
        }

        public NIOConnection best(final Hint hint, final int tries) {
            if (connections.size() == 0)
                return nextRoundRobinConnection();

            NIOConnection best;
            final List<PrioritizedRexsterConnection> candidates = new ArrayList<PrioritizedRexsterConnection>();
            for (RexsterConnection conn : connections.values()) {
                try {
                    final List<ElementRange> ranges;
                    if (hint.getElementType() == Vertex.class) {
                        ranges = conn.getHintedGraphs().graphs.get(hint.getGraphName()).getVertexRanges();
                    } else {
                        ranges = conn.getHintedGraphs().graphs.get(hint.getGraphName()).getEdgeRanges();
                    }

                    for (ElementRange range : ranges) {
                        if (range.contains(hint.getHintValue()))
                            candidates.add(new PrioritizedRexsterConnection(conn, range.getPriority()));
                    }
                } catch (Exception ex) {
                    // maybe the server hasn't broadcasted yet.  if so just let it run to select the
                    // connection based on round-robin
                }
            }

            // for each try select the next best connection.  if there are more tries than recommended connections
            // then simply revert to roundrobin
            if (candidates.size() > 0 && tries < candidates.size()) {
                Collections.sort(candidates, PrioritizedRexsterConnectionComparator.COMPARATOR);
                best = candidates.get(tries).connection.getNioConnection();
            } else
                best = nextRoundRobinConnection();

            return best;
        }
    }

    private static class PrioritizedRexsterConnection {
        private final RexsterConnection connection;
        private final int priority;

        private PrioritizedRexsterConnection(final RexsterConnection connection, final int priority) {
            this.connection = connection;
            this.priority = priority;
        }
    }

    private static class PrioritizedRexsterConnectionComparator implements Comparator<PrioritizedRexsterConnection> {
        public static final PrioritizedRexsterConnectionComparator COMPARATOR = new PrioritizedRexsterConnectionComparator();

        @Override
        public int compare(final PrioritizedRexsterConnection prioritizedRexsterConnection, final PrioritizedRexsterConnection prioritizedRexsterConnection2) {
            return new Integer(prioritizedRexsterConnection.priority).compareTo(prioritizedRexsterConnection2.priority);
        }
    }

    public static class Hint<U extends Comparable> {
        private Class elementType;
        private U hintValue;
        private String graphName;

        public Hint(final Class elementType, final U hintValue, final String graphName) {
            this.elementType = elementType;
            this.hintValue = hintValue;
            this.graphName = graphName;
        }

        public Class getElementType() {
            return elementType;
        }

        public U getHintValue() {
            return hintValue;
        }

        public String getGraphName() {
            return graphName;
        }
    }
}
