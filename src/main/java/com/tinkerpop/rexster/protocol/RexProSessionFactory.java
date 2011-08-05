package com.tinkerpop.rexster.protocol;

import com.tinkerpop.rexster.RexsterApplication;

import java.util.UUID;

public class RexProSessionFactory {
    public static final byte CHANNEL_CONSOLE = 0;

    public static AbstractRexProSession createInstance(UUID sessionKey, RexsterApplication rexsterApplication, byte sessionChannel) {
        if (sessionChannel == CHANNEL_CONSOLE) {
            return new ConsoleRexProSession(sessionKey, rexsterApplication);
        } else {
            return null;
        }
    }
}
