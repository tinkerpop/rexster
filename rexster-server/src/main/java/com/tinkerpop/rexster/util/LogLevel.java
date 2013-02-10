package com.tinkerpop.rexster.util;

import java.util.logging.Level;

/**
 * Logging level enum, used for switch statements.
 * <br/>
 * https://github.com/joshuadavis/yajul/blob/master/core/src/main/java/org/yajul/log/LogLevel.java
 */
public enum LogLevel {
    TRACE(Level.FINER.intValue()),
    DEBUG(Level.FINE.intValue()),
    INFO(Level.INFO.intValue()),
    WARN(Level.WARNING.intValue()),
    ERROR(Level.SEVERE.intValue()),
    OFF(Level.OFF.intValue()),
    ;
    private int juliPriority;

    LogLevel(int juliPriority) {
        this.juliPriority = juliPriority;
    }

    public static LogLevel toLogLevel(Level level) {
        int juliPriority = level.intValue();
        LogLevel[] levels = LogLevel.values();
        for (LogLevel logLevel : levels) {
            if (logLevel.juliPriority >= juliPriority)
                return logLevel;
        }
        return OFF;
    }
}
