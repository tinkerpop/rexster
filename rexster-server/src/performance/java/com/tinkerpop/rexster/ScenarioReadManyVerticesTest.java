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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Read 800 vertices from gratefulgraph.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
@AxisRange(min = 0, max = 1)
@BenchmarkMethodChart(filePrefix = "read-many-vertices")
@BenchmarkHistoryChart(labelWith = LabelType.CUSTOM_KEY, maxRuns = 20, filePrefix = "hx-read-many-vertices")
public class ScenarioReadManyVerticesTest extends AbstractRexsterPerformanceTest {

    public final static int DEFAULT_BENCHMARK_ROUNDS = 10;
    public final static int DEFAULT_WARMUP_ROUNDS = 1;

    public final static int DEFAULT_CONCURRENT_BENCHMARK_ROUNDS = 100;
    public final static int DEFAULT_CONCURRENT_WARMUP_ROUNDS = 1;

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
    public void restApi() throws Exception {
        tryRestApi();
    }

    @BenchmarkOptions(benchmarkRounds = DEFAULT_CONCURRENT_BENCHMARK_ROUNDS, warmupRounds = DEFAULT_CONCURRENT_WARMUP_ROUNDS, concurrency = BenchmarkOptions.CONCURRENCY_AVAILABLE_CORES)
    @Test
    public void restApiConcurrent() throws Exception {
        tryRestApi();
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
        tryRexproSessionless(getRexsterClientMsgPackGratefulGraph());
    }

    private void tryRexproJsonSessionless() throws Exception {
        tryRexproSessionless(getRexsterClientJsonGratefulGraph());
    }

    private void tryRexproSessionless(final RexsterClient client) throws Exception {
        for (int ix = 1; ix < 801; ix++) {
            final Map<String, Object> m = new HashMap<String, Object>();
            m.put("x", ix);

            final List<Map<String, Object>> results = client.execute("g.v(x)", m);
            Assert.assertEquals(String.valueOf(ix), results.get(0).get("_id"));
        }
    }

    private void tryRestGremlin() throws Exception {
        final String url = getHttpBaseUri() + "graphs/gratefulgraph/tp/gremlin?script=" + URLEncoder.encode("g.v(x)") + "&params.x=";
        for (int ix = 1; ix < 801; ix++) {
            final ClientRequest request = ClientRequest.create().build(URI.create(url + String.valueOf(ix)), "GET");
            final ClientResponse response = httpClient.handle(request);
            final JSONObject json = response.getEntity(JSONObject.class);
            Assert.assertEquals(ix, json.optJSONArray("results").optJSONObject(0).optInt("_id"));
        }
    }

    private void tryRestApi() throws Exception {
        final String url = getHttpBaseUri() + "graphs/gratefulgraph/vertices/";
        for (int ix = 1; ix < 801; ix++) {
            final ClientRequest request = ClientRequest.create().build(URI.create(url + String.valueOf(ix)), "GET");
            final ClientResponse response = httpClient.handle(request);
            final JSONObject json = response.getEntity(JSONObject.class);
            Assert.assertEquals(ix, json.optJSONObject("results").optInt("_id"));
        }
    }
}
