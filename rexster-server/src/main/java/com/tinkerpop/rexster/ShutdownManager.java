package com.tinkerpop.rexster;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manages the socket listening for incoming shutdown requests.
 * <p/>
 * Adapted from http://code.google.com/p/shutdown-listener/
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class ShutdownManager {
    protected final Logger logger = Logger.getLogger(this.getClass());

    private final CountDownLatch shutdownLatch = new CountDownLatch(1);
    private final AtomicBoolean shutdownRequested = new AtomicBoolean(false);
    private final AtomicBoolean shutdownComplete = new AtomicBoolean(false);

    protected final Collection<ShutdownListener> internalShutdownListeners = new ArrayList<ShutdownListener>();
    protected Collection<ShutdownListener> shutdownListeners = null;

    public static final String COMMAND_SHUTDOWN_WAIT = "sw";
    public static final String COMMAND_SHUTDOWN_NO_WAIT = "s";
    public static final String COMMAND_STATUS = "status";

    private int port = 8183;
    private String host = "127.0.0.1";

    static {
        PropertyConfigurator.configure(RexsterApplicationImpl.class.getResource("log4j.properties"));
    }

    public ShutdownManager(String host, int port) {
        this.port = port;
        this.host = host;
    }

    public void registerShutdownListener(ShutdownListener shutdownListener) {
        if (this.shutdownListeners == null) {
            this.shutdownListeners = new ArrayList<ShutdownListener>();
        }
        this.shutdownListeners.add(shutdownListener);
    }

    public final void start() throws Exception {

        final ShutdownSocketListener shutdownSocketListener = new ShutdownSocketListener(this.host, this.port);

        final Thread shutdownSocketThread = new Thread(shutdownSocketListener, "ShutdownListener-" + this.host + ":" + this.port);
        shutdownSocketThread.setDaemon(true);
        shutdownSocketThread.start();

        //Add the listener to the shutdown list
        this.internalShutdownListeners.add(shutdownSocketListener);

        //Register a shutdown handler
        final Thread shutdownHook = new Thread(new ShutdownHookHandler(), "JVM Shutdown Hook");
        Runtime.getRuntime().addShutdownHook(shutdownHook);
        this.logger.debug("Registered JVM shutdown hook");

        this.internalShutdownListeners.add(new ShutdownListener() {
            public void shutdown() {
                Runtime.getRuntime().removeShutdownHook(shutdownHook);
                logger.debug("Removed JVM shutdown hook");
            }

            @Override
            public String toString() {
                return "JVM Shutdown Hook Remover";
            }
        });
    }

    /**
     * If shutdown isn't complete will wait on the shutdown lock for shutdown to complete.
     * DOES NOT TRIGGER SHUTDOWN
     */
    public final void waitForShutdown() {
        if (this.shutdownComplete.get()) {
            return;
        }

        try {
            this.shutdownLatch.await();
        } catch (InterruptedException e) {
            this.logger.warn("Interrupted waiting for shutdown condition", e);
        }
    }

    /**
     * Calls shutdown hooks and cleans up shutdown listener code, notifies all waiting threads on completion
     */
    public final void shutdown() {
        final boolean shuttingDown = this.shutdownRequested.getAndSet(true);
        if (shuttingDown) {
            if (this.shutdownComplete.get()) {
                logger.info("Already shut down, ignoring duplicate request");
            } else {
                logger.info("Already shutting down, ignoring duplicate request");
            }
            return;
        }

        this.preShutdownListeners();

        //Run external shutdown tasks
        this.runShutdownHandlers(this.shutdownListeners);

        //Run internal shutdown tasks
        this.runShutdownHandlers(this.internalShutdownListeners);

        this.postShutdownListeners();

        this.shutdownComplete.set(true);
        this.shutdownLatch.countDown();
    }

    /**
     * Called before the shutdown listeners
     */
    protected void preShutdownListeners() {
    }

    /**
     * Called after the shutdown listeners, before threads waiting on {@link #waitForShutdown()} are released
     */
    protected void postShutdownListeners() {
    }

    /**
     * Sort a {@link List} of {@link com.tinkerpop.rexster.ShutdownManager.ShutdownSocketListener} before {@link #runShutdownHandlers(Collection)} iterates over them.
     * Default implementation does nothing
     */
    protected void sortShutdownListeners(List<ShutdownListener> shutdownListeners) {
    }

    protected final void runShutdownHandlers(Collection<ShutdownListener> shutdownListeners) {
        final List<ShutdownListener> shutdownListenersClone = new ArrayList<ShutdownListener>(shutdownListeners);
        this.sortShutdownListeners(shutdownListenersClone);
        for (final ShutdownListener shutdownListener : shutdownListenersClone) {
            try {
                this.logger.info("Calling ShutdownListener: " + shutdownListener);
                shutdownListener.shutdown();
                this.logger.info("ShutdownListener " + shutdownListener + " complete");
            } catch (Exception e) {
                this.logger.warn("ShutdownListener " + shutdownListener + " threw an exception, continuing with shutdown");
            }
        }
    }

    /**
     * Runnable for waiting on connections to the shutdown socket and handling them
     */
    private class ShutdownSocketListener implements Runnable, ShutdownListener {
        private final ServerSocket shutdownSocket;
        private final InetAddress bindHost;
        private final int port;

        private ShutdownSocketListener(String host, int port) {
            this.port = port;
            try {
                this.bindHost = InetAddress.getByName(host);
            } catch (UnknownHostException uhe) {
                throw new RuntimeException("Failed to create InetAddress for host '" + host + "'", uhe);
            }

            try {
                this.shutdownSocket = new ServerSocket(this.port, 10, this.bindHost);
            } catch (IOException ioe) {
                throw new RuntimeException("Failed to create shutdown socket on '" + this.bindHost + "' and " + this.port, ioe);
            }

            logger.info("Bound shutdown socket to " + this.bindHost + ":" + this.port + ". Starting listener thread for shutdown requests.");
        }

        /* (non-Javadoc)
         * @see java.lang.Runnable#run()
         */
        public void run() {
            try {
                while (!shutdownSocket.isClosed()) {
                    try {
                        final Socket connection = shutdownSocket.accept();
                        final ShutdownSocketHandler shutdownSocketHandler = new ShutdownSocketHandler(connection);
                        final Thread shutdownRequestThread = new Thread(shutdownSocketHandler, "ShutdownManager-" + connection.getInetAddress() + ":" + connection.getPort());
                        shutdownRequestThread.setDaemon(true);
                        shutdownRequestThread.start();
                    } catch (SocketException se) {
                        if (shutdownSocket.isClosed()) {
                            logger.info("Caught SocketException on shutdownSocket, assuming close() was called: " + se);
                        } else {
                            logger.warn("Exception while handling connection to shutdown socket, ignoring", se);
                        }
                    } catch (IOException ioe) {
                        logger.warn("Exception while handling connection to shutdown socket, ignoring", ioe);
                    }
                }
            } finally {
                this.shutdown();
            }
        }

        public void shutdown() {
            if (!shutdownSocket.isClosed()) {
                try {
                    shutdownSocket.close();
                    logger.debug("Closed shutdown socket " + this.bindHost + ":" + this.port);
                } catch (IOException ioe) {
                    //Ignore
                }
            }
        }

        @Override
        public String toString() {
            return "ShutdownListener [bindHost=" + bindHost + ", port=" + port + "]";
        }
    }

    /**
     * Runnable to handle connections to the shutdown socket
     */
    private class ShutdownSocketHandler implements Runnable {
        private final Socket shutdownConnection;

        public ShutdownSocketHandler(Socket shutdownConnection) {
            this.shutdownConnection = shutdownConnection;
        }

        public void run() {
            boolean shutdownNoWait = false;

            try {
                final BufferedReader reader = new BufferedReader(new InputStreamReader(shutdownConnection.getInputStream()));
                final PrintWriter writer = new PrintWriter(this.shutdownConnection.getOutputStream());
                try {
                    final String receivedCommand = reader.readLine();

                    if (ShutdownManager.COMMAND_SHUTDOWN_WAIT.equals(receivedCommand)) {
                        logger.info("Received request for shutdown");
                        writer.println("Rexster Server shutting down...");
                        writer.flush();
                        shutdown();
                        writer.println("Rexster Server shutdown complete");
                    } else if (ShutdownManager.COMMAND_SHUTDOWN_NO_WAIT.equals(receivedCommand)) {
                        logger.info("Received request for shutdown");
                        writer.println("Rexster Server is starting shutdown (check status of shutdown with --status option)");
                        shutdownNoWait = true;
                    } else if (ShutdownManager.COMMAND_STATUS.equals(receivedCommand)) {
                        logger.debug("Received request for status");
                        if (shutdownRequested.get()) {
                            writer.println("Rexster Server is shutting down");
                        } else {
                            writer.println("Rexster Server is running");
                        }
                    } else {
                        writer.println(new Date() + ": Unknown command '" + receivedCommand + "'");
                    }
                } finally {
                    writer.flush();
                    IOUtils.closeQuietly(reader);
                    IOUtils.closeQuietly(writer);
                }
            } catch (IOException e) {
                logger.warn("Exception while handling connection to shutdown socket, ignoring", e);
            } finally {
                if (this.shutdownConnection != null) {
                    try {
                        this.shutdownConnection.close();
                    } catch (IOException ioe) {
                        //Ignore
                    }
                }

                //To handle shutdown-no-wait calls
                if (shutdownNoWait) {
                    shutdown();
                }
            }
        }
    }


    public interface ShutdownListener {
        /**
         * Called when the application is shutting down, should block until the class is completely shut down
         */
        public void shutdown();
    }

    /**
     * Runnable that calls shutdown, used for JVM shutdown hook
     */
    private class ShutdownHookHandler implements Runnable {
        /* (non-Javadoc)
         * @see java.lang.Runnable#run()
         */
        public void run() {
            logger.info("JVM shutdown hook called");
            shutdown();
        }
    }

}