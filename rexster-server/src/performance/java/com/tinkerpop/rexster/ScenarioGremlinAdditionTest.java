package com.tinkerpop.rexster;

import com.carrotsearch.junitbenchmarks.BenchmarkOptions;
import com.carrotsearch.junitbenchmarks.BenchmarkRule;
import com.carrotsearch.junitbenchmarks.annotation.AxisRange;
import com.carrotsearch.junitbenchmarks.annotation.BenchmarkHistoryChart;
import com.carrotsearch.junitbenchmarks.annotation.BenchmarkMethodChart;
import com.carrotsearch.junitbenchmarks.annotation.LabelType;
import com.sun.jersey.api.client.ClientRequest;
import com.sun.jersey.api.client.ClientResponse;
import com.tinkerpop.rexster.client.RexsterClient;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import java.net.URI;
import java.net.URLEncoder;
import java.util.List;

/**
 * Execute a simple script (1+1).
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
@AxisRange(min = 0, max = 1)
@BenchmarkMethodChart(filePrefix = "gremlin-addition")
@BenchmarkHistoryChart(labelWith = LabelType.CUSTOM_KEY, maxRuns = 20, filePrefix = "hx-gremlin-addition")
public class ScenarioGremlinAdditionTest extends AbstractRexsterPerformanceTest {

    public final static int DEFAULT_BENCHMARK_ROUNDS = 50;
    public final static int DEFAULT_WARMUP_ROUNDS = 5;

    public final static int DEFAULT_CONCURRENT_BENCHMARK_ROUNDS = 500;
    public final static int DEFAULT_CONCURRENT_WARMUP_ROUNDS = 10;

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
    public void rexproMsgPackSessionless() throws Exception {
        tryRexproMsgPackSessionless();
    }

    @BenchmarkOptions(benchmarkRounds = DEFAULT_CONCURRENT_BENCHMARK_ROUNDS, warmupRounds = DEFAULT_CONCURRENT_WARMUP_ROUNDS, concurrency = BenchmarkOptions.CONCURRENCY_AVAILABLE_CORES)
    @Test
    public void rexproMsgPackSessionlessConcurrent() throws Exception {
        tryRexproMsgPackSessionless();
    }

    @BenchmarkOptions(benchmarkRounds = DEFAULT_BENCHMARK_ROUNDS, warmupRounds = DEFAULT_WARMUP_ROUNDS, concurrency = BenchmarkOptions.CONCURRENCY_SEQUENTIAL)
    @Test
    public void rexproJsonSessionless() throws Exception {
        tryRexproJsonSessionless();
    }

    @BenchmarkOptions(benchmarkRounds = DEFAULT_CONCURRENT_BENCHMARK_ROUNDS, warmupRounds = DEFAULT_CONCURRENT_WARMUP_ROUNDS, concurrency = BenchmarkOptions.CONCURRENCY_AVAILABLE_CORES)
    @Test
    public void rexproJsonSessionlessConcurrent() throws Exception {
        tryRexproJsonSessionless();
    }

    private void tryRexproMsgPackSessionless() throws Exception {
        tryRexproSessionless(getRexsterClientMsgPackEmptyGraph());
    }

    private void tryRexproJsonSessionless() throws Exception {
        tryRexproSessionless(getRexsterClientJsonEmptyGraph());
    }

    private void tryRexproSessionless(final RexsterClient client) throws Exception {
        final List<Long> results = client.execute("1+1");
        Assert.assertEquals(2, results.get(0).intValue());
    }

    private void tryRestGremlin() throws Exception {
        final String url = getHttpBaseUri() + "graphs/emptygraph/tp/gremlin?script=" + URLEncoder.encode("1+1");
        final ClientRequest request = ClientRequest.create().build(URI.create(url), "GET");
        final ClientResponse response = httpClient.handle(request);
        final JSONObject json = response.getEntity(JSONObject.class);
        Assert.assertEquals(2, json.optJSONArray("results").optInt(0));
    }
}
