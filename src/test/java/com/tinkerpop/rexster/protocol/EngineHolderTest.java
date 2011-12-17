package com.tinkerpop.rexster.protocol;

import junit.framework.Assert;
import org.junit.Test;

import javax.script.ScriptEngine;

public class EngineHolderTest {
    @Test
    public void getEngineReset() throws Exception {
        EngineController controller = EngineController.getInstance();
        EngineHolder holder = controller.getEngineByLanguageName("groovy");
        ScriptEngine engine = holder.getEngine();
        for (int ix = 0; ix < EngineHolder.ENGINE_RESET_THRESHOLD; ix++) {
            Assert.assertSame(engine, holder.getEngine());
        }

        Assert.assertNotSame(engine, holder.getEngine());
    }
}
