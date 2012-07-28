package com.tinkerpop.rexster.util;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class StatisticsHelper {

    private double time = -1.0d;

    public double stopWatch() {
        if (time == -1.0d) {
            time = System.nanoTime();
            return time;
        } else {
            double temp = (System.nanoTime() - time) / 1000000d;
            time = -1.0d;
            return temp;
        }
    }
}
