package com.tinkerpop.rexster.server;

import com.tinkerpop.rexster.config.hinted.HintedGraphs;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.Logger;
import org.jgroups.Address;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.stack.AddressGenerator;
import org.jgroups.util.PayloadUUID;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class RexsterCluster implements RexsterServer {
    private static final Logger logger = Logger.getLogger(RexsterCluster.class);
    private static final String DEFAULT_CLUSTER_NAME = "rexster";
    private static final long DEFAULT_BROADCAST_RETRY = 10000;

    private JChannel channel;
    private Thread broadcast;

    private Integer lastRexsterServerPort;
    private String lastRexsterServerHost;
    private final AtomicInteger rexsterServerPort = new AtomicInteger();
    private final AtomicReference<String> rexsterServerHost = new AtomicReference<String>();
    private final AtomicBoolean killed = new AtomicBoolean(false);

    public RexsterCluster(final RexsterProperties properties) throws Exception {
        // initialize channel first as it need sto be present for updateSettings
        final String stack = System.getProperty("rexster.jgroups", JChannel.DEFAULT_PROTOCOL_STACK);
        this.channel = new JChannel(stack);
        this.channel.setAddressGenerator(new AddressGenerator() {
            @Override
            public Address generateAddress() {
                return PayloadUUID.randomUUID(String.format("server-%s:%s", rexsterServerHost.get(), rexsterServerPort.get()));
            }
        });

        properties.addListener(new RexsterProperties.RexsterPropertiesListener() {
            @Override
            public void propertiesChanged(final XMLConfiguration configuration) {
                // maintain history of previous settings
                lastRexsterServerHost = rexsterServerHost.get();
                lastRexsterServerPort = rexsterServerPort.get();
                updateSettings(configuration);
            }
        });
        this.updateSettings(properties.getConfiguration());

        logger.info(String.format("Joined [%s] cluster.  Broadcasting server information every %s ms",
                DEFAULT_CLUSTER_NAME, DEFAULT_BROADCAST_RETRY));
    }

    public void start(final RexsterApplication rexsterApplication) {
        if (broadcast == null || this.broadcast.isInterrupted()) {
            this.broadcast = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (!killed.get()) {
                        final Set<String> graphNames = rexsterApplication.getGraphNames();
                        final HintedGraphs graphs = new HintedGraphs();

                        for (String graphName : graphNames) {
                            graphs.graphs.put(graphName, rexsterApplication.getApplicationGraph(graphName).getHintedGraph());
                        }

                        final Message msg = new Message(null, graphs);
                        try {
                            channel.send(msg);
                            logger.debug(String.format("Broadcast %s", msg));
                        } catch (Exception ex) {
                            logger.warn(String.format(
                                    "Could not broadcast existence to the [%s] cluster.  Will retry in %s",
                                    DEFAULT_CLUSTER_NAME, DEFAULT_BROADCAST_RETRY), ex);
                        } finally {
                            try { Thread.sleep(DEFAULT_BROADCAST_RETRY); } catch (InterruptedException ie) { }
                        }
                    }
                }
            });
            broadcast.start();
        }
    }

    public void stop() {
        if (broadcast != null) {
            int checks = 0;
            this.killed.set(true);
            while (this.broadcast.isAlive()) {
                try { Thread.sleep(1000); } catch (InterruptedException ie) {}
                checks++;
                if (checks > (DEFAULT_BROADCAST_RETRY / 1000))
                    this.broadcast.interrupt();
            }

            this.channel.close();

            logger.info(String.format("Rexster is no longer broadcasting to the [%s] cluster.", DEFAULT_CLUSTER_NAME));
        }
    }

    private void updateSettings(final XMLConfiguration configuration) {
        this.rexsterServerPort.set(configuration.getInteger("rexpro.server-port", new Integer(RexsterSettings.DEFAULT_HTTP_PORT)));
        this.rexsterServerHost.set(configuration.getString("rexpro.server-host", "0.0.0.0"));

        // reset the cluster connection if changed
        if (!this.rexsterServerHost.get().equals(lastRexsterServerHost)
                || this.rexsterServerPort.get() != lastRexsterServerPort) {
            this.channel.disconnect();
            try {
                this.channel.connect(DEFAULT_CLUSTER_NAME);
                logger.info(String.format("Initialize/Reset connection to [%s] cluster after reconfiguration of host/port", DEFAULT_CLUSTER_NAME));
            }
            catch (Exception ex) {
                logger.warn(String.format("Failed to disconnect from [%s] cluster after reconfiguration of host/port.", DEFAULT_CLUSTER_NAME), ex);
            }
        }
    }
}
