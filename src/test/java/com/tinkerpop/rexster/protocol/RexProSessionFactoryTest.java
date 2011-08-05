package com.tinkerpop.rexster.protocol;

import com.tinkerpop.blueprints.pgm.impls.tg.TinkerGraph;
import com.tinkerpop.rexster.RexsterApplication;
import junit.framework.Assert;
import org.junit.Test;

import java.util.UUID;

public class RexProSessionFactoryTest {
    @Test
    public void createInstanceConsoleSession() {
        UUID sessionKey = UUID.randomUUID();
        AbstractRexProSession session = RexProSessionFactory.createInstance(sessionKey, new RexsterApplication("graph", new TinkerGraph()), RexProSessionFactory.CHANNEL_CONSOLE);

        Assert.assertNotNull(session);
        Assert.assertTrue(session instanceof ConsoleRexProSession);
        Assert.assertEquals(sessionKey, session.getSessionKey());
    }

    @Test
    public void createInstanceInvalidChannel() {
        Assert.assertNull(RexProSessionFactory.createInstance(UUID.randomUUID(), new RexsterApplication("graph", new TinkerGraph()), Byte.MAX_VALUE));
    }
}
