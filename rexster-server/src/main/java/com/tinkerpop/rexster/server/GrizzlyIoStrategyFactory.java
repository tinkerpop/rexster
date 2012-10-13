package com.tinkerpop.rexster.server;

import org.glassfish.grizzly.IOStrategy;
import org.glassfish.grizzly.strategies.LeaderFollowerNIOStrategy;
import org.glassfish.grizzly.strategies.SameThreadIOStrategy;
import org.glassfish.grizzly.strategies.SimpleDynamicNIOStrategy;
import org.glassfish.grizzly.strategies.WorkerThreadIOStrategy;

/**
 * Creates various IOStrategy implementations for Grizzly.
 *
 * http://grizzly.java.net/nonav/docs/docbkx2.0/html/iostrategies.html
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class GrizzlyIoStrategyFactory {

    private static final String WORKER = "worker";
    private static final String SAME = "same";
    private static final String DYNAMIC = "dynamic";
    private static final String LEADER_FOLLOWER = "leader-follower";

    public static IOStrategy createIoStrategy(final String strategy) {
        final IOStrategy ioStrategy;
        if (strategy.equals(WORKER)) {
            ioStrategy = WorkerThreadIOStrategy.getInstance();
        } else if (strategy.equals(SAME)) {
            ioStrategy = SameThreadIOStrategy.getInstance();
        } else if (strategy.equals(DYNAMIC))  {
            ioStrategy = SimpleDynamicNIOStrategy.getInstance();
        } else if (strategy.equals(LEADER_FOLLOWER)) {
            ioStrategy = LeaderFollowerNIOStrategy.getInstance();
        } else {
            ioStrategy = WorkerThreadIOStrategy.getInstance();
        }

        return ioStrategy;
    }
}
