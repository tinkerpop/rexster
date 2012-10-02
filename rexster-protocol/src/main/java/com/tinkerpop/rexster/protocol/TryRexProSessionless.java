package com.tinkerpop.rexster.protocol;

import com.tinkerpop.rexster.Tokens;
import com.tinkerpop.rexster.client.RexsterClient;
import com.tinkerpop.rexster.protocol.msg.MessageFlag;
import com.tinkerpop.rexster.protocol.msg.MsgPackScriptResponseMessage;
import com.tinkerpop.rexster.protocol.msg.ScriptRequestMessage;
import org.msgpack.MessagePack;
import org.msgpack.type.Value;
import org.msgpack.unpacker.BufferUnpacker;
import org.msgpack.unpacker.Converter;
import org.msgpack.unpacker.UnpackerIterator;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import static org.msgpack.template.Templates.tMap;
import static org.msgpack.template.Templates.TString;
import static org.msgpack.template.Templates.TValue;

/**
 * A bit of an experiment.
 */
public class TryRexProSessionless {

    public static void main(final String[] args) throws Exception {
        int c = Integer.parseInt(args[1]);
        final CountDownLatch latch = new CountDownLatch(c);

        for (int ix = 0; ix < c; ix++) {
            new Thread(new Runnable() {

                @Override
                public void run() {
                    lotsOfCalls(args[0].split(","));
                    latch.countDown();
                }
            }).start();
        }

        latch.await();
    }

    private static void lotsOfCalls(final String[] hosts){

        long start = System.currentTimeMillis();
        long checkpoint = System.currentTimeMillis();

        try {

            final RexsterClient client = new RexsterClient(hosts);
            final List<Map<String, Value>> results = client.gremlin("g=rexster.getGraph('gratefulgraph');g.V;");
            int counter = 1;
            for (Map<String, Value> result : results) {
                final String vId = result.get(Tokens._ID).asRawValue().getString();
                final List<Map<String, Value>> innerResults = client.gremlin(String.format("g=rexster.getGraph('gratefulgraph');g.v(%s)", vId));
                System.out.println(innerResults.get(0));
                counter++;
            }

            long end = System.currentTimeMillis() - checkpoint;
            System.out.println((checkpoint - start) + ":" + end);
            System.out.println(counter / (end / 1000));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}