package com.tinkerpop.rexster.protocol;

import com.tinkerpop.rexster.Tokens;
import com.tinkerpop.rexster.client.RexsterClient;
import org.msgpack.type.Value;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.msgpack.template.Templates.TString;
import static org.msgpack.template.Templates.TValue;
import static org.msgpack.template.Templates.tMap;

/**
 * A bit of an experiment.
 */
public class TryRexProSessionless implements Runnable {

    private int cycle = 0;
    private final String[] hosts;
    private final int exerciseTime;

    public static void main(final String[] args) throws Exception {
        int c = Integer.parseInt(args[1]);
        final int exerciseTime = Integer.parseInt(args[2]) * 60 * 1000;

        for (int ix = 0; ix < c; ix++) {
            new Thread(new TryRexProSessionless(args[0].split(","), exerciseTime)).start();
        }

        Thread.currentThread().join();
        System.exit(0);
    }

    public TryRexProSessionless(final String[] hosts, final int exerciseTime) {
        this.exerciseTime = exerciseTime;
        this.hosts = hosts;
    }

    @Override
    public void run() {
        this.lotsOfCalls();
    }

    private void lotsOfCalls(){

        final long start = System.currentTimeMillis();
        long checkpoint = System.currentTimeMillis();
        final Random random = new Random();

        while ((System.currentTimeMillis() - start) < exerciseTime) {
            cycle++;
            System.out.println("Exercise cycle: " + cycle);

            try {

                final RexsterClient client = new RexsterClient(hosts, 5);
                int counter = 1;
                final int vRequestCount = random.nextInt(500);
                for (int iv = 1; iv < vRequestCount; iv++) {
                    final Map<String,Object> scriptArgs = new HashMap<String, Object>();
                    scriptArgs.put("id", random.nextInt(800));
                    final List<Map<String, Value>> innerResults = client.gremlin("g=rexster.getGraph('gratefulgraph');g.v(id)", scriptArgs, tMap(TString, TValue));
                    System.out.println(innerResults.get(0));
                    counter++;
                }

                final int eRequestCount = random.nextInt(500);
                for (int ie = 1; ie < eRequestCount; ie++) {
                    final Map<String,Object> scriptArgs = new HashMap<String, Object>();
                    scriptArgs.put("id", random.nextInt(8000));
                    final List<Map<String, Value>> innerResults = client.gremlin("g=rexster.getGraph('gratefulgraph');g.e(id)", scriptArgs, tMap(TString, TValue));
                    System.out.println(innerResults.get(0));
                    counter++;
                }

                final long end = System.currentTimeMillis() - checkpoint;
                System.out.println((checkpoint - start) + ":" + end);
                System.out.println(counter / (end / 1000));
            } catch (Exception ex) {
                System.out.println("Error during TEST CYCLE (stack trace follows)");
                ex.printStackTrace();
                if (ex.getCause() != null) {
                    System.out.println("There is an inner exception (stack trace follows)");
                    ex.getCause().printStackTrace();
                }
            }
        }
    }
}