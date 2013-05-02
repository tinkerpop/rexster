package com.tinkerpop.rexster;

import com.carrotsearch.junitbenchmarks.BenchmarkOptions;
import com.carrotsearch.junitbenchmarks.BenchmarkRule;
import com.carrotsearch.junitbenchmarks.annotation.AxisRange;
import com.carrotsearch.junitbenchmarks.annotation.BenchmarkHistoryChart;
import com.carrotsearch.junitbenchmarks.annotation.BenchmarkMethodChart;
import com.carrotsearch.junitbenchmarks.annotation.LabelType;
import com.sun.jersey.api.client.ClientRequest;
import com.sun.jersey.api.client.ClientResponse;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import java.net.URI;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
@AxisRange(min = 0, max = 1)
@BenchmarkMethodChart(filePrefix = "gremlin-traverse")
@BenchmarkHistoryChart(labelWith = LabelType.CUSTOM_KEY, maxRuns = 20, filePrefix = "hx-gremlin-traverse")
public class ScenarioGremlinTraverseTest extends AbstractRexsterPerformanceTest {

    public final static int DEFAULT_BENCHMARK_ROUNDS = 50;
    public final static int DEFAULT_WARMUP_ROUNDS = 1;

    public final static int DEFAULT_CONCURRENT_BENCHMARK_ROUNDS = 500;
    public final static int DEFAULT_CONCURRENT_WARMUP_ROUNDS = 1;

    private static final String[] traversals = {
        "g.v(x).out.out.groupCount.cap.next()"
    };

    @Rule
    public TestRule benchmarkRun = new BenchmarkRule();

    @BenchmarkOptions(benchmarkRounds = DEFAULT_BENCHMARK_ROUNDS, warmupRounds = DEFAULT_WARMUP_ROUNDS, concurrency = BenchmarkOptions.CONCURRENCY_SEQUENTIAL)
    @Test
    public void restGremlin() throws Exception {
        tryRestGremlin();
    }

    @BenchmarkOptions(benchmarkRounds = DEFAULT_CONCURRENT_BENCHMARK_ROUNDS, warmupRounds = DEFAULT_CONCURRENT_WARMUP_ROUNDS, concurrency = BenchmarkOptions.CONCURRENCY_AVAILABLE_CORES)
    @Test
    public void restGremlinConcurrent() throws Exception {
        tryRestGremlin();
    }

    @BenchmarkOptions(benchmarkRounds = DEFAULT_BENCHMARK_ROUNDS, warmupRounds = DEFAULT_WARMUP_ROUNDS, concurrency = BenchmarkOptions.CONCURRENCY_SEQUENTIAL)
    @Test
    public void rexproSessionless() throws Exception {
        tryRexproSessionless();
    }

    @BenchmarkOptions(benchmarkRounds = DEFAULT_CONCURRENT_BENCHMARK_ROUNDS, warmupRounds = DEFAULT_CONCURRENT_WARMUP_ROUNDS, concurrency = BenchmarkOptions.CONCURRENCY_AVAILABLE_CORES)
    @Test
    public void rexproSessionlessConcurrent() throws Exception {
        tryRexproSessionless();
    }

    private void tryRexproSessionless() throws Exception {
        final String traversal = traversals[0];
        for (int iy = 1; iy < 26; iy++) {
            final Map<String, Object> m = new HashMap<String, Object>();
            m.put("x", iy * 8);

            final List<Map<String, Object>> results = rexproClientGrateful.execute(traversal, m);
            Assert.assertNotNull(results);
        }
    }


    private void tryRestGremlin() throws Exception {
        final String traversal = traversals[0];
        for (int iy = 1; iy < 26; iy++) {
            final String url = getHttpBaseUri() + "graphs/gratefulgraph/tp/gremlin?script=" + URLEncoder.encode(traversal) + "&params.x=";
            final ClientRequest request = ClientRequest.create().build(URI.create(url + String.valueOf(iy * 8)), "GET");
            final ClientResponse response = httpClient.handle(request);
            final JSONObject json = response.getEntity(JSONObject.class);
            //Assert.assertEquals(2, json.optJSONArray("results").optInt(0));
        }
    }
}