package com.tinkerpop.rexster;

/**
 * author: Marko A. Rodriguez (http://markorodriguez.com)
 */
public class StatisticsHelper {

    private long time = -1l;

    public long stopWatch() {
        if (time == -1l) {
            time = System.currentTimeMillis();
            return time;
        } else {
            long temp = System.currentTimeMillis() - time;
            time = -1l;
            return temp;
        }
    }
}
