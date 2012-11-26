package com.tinkerpop.rexster.protocol;

import junit.framework.Assert;
import org.junit.Ignore;
import org.junit.Test;

import javax.script.ScriptEngine;

public class EngineHolderTest {
    @Test
    @Ignore
    public void getEngineReset() throws Exception {
        EngineController controller = EngineController.getInstance();
        EngineHolder holder = controller.getEngineByLanguageName("groovy");

        // counter at 0
        ScriptEngine engine = holder.getEngine();

        // counter at 1
        for (int iy = 0; iy < 10; iy++) {
            for (int ix = 1; ix < 100; ix++) {
                ScriptEngine sameEngine = holder.getEngine();
                Assert.assertSame(engine, sameEngine);
            }

            ScriptEngine differentEngine = holder.getEngine();
            Assert.assertNotSame(engine, differentEngine);

            engine = differentEngine;
        }

    }
}
