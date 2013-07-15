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
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
@AxisRange(min = 0, max = 1)
@BenchmarkMethodChart(filePrefix = "gremlin-traverse")
@BenchmarkHistoryChart(labelWith = LabelType.CUSTOM_KEY, maxRuns = 20, filePrefix = "hx-gremlin-traverse")
public class ScenarioGremlinTraverseTest extends AbstractRexsterPerformanceTest {

    public final static int DEFAULT_BENCHMARK_ROUNDS = 20;
    public final static int DEFAULT_WARMUP_ROUNDS = 1;

    public final static int DEFAULT_CONCURRENT_BENCHMARK_ROUNDS = 100;
    public final static int DEFAULT_CONCURRENT_WARMUP_ROUNDS = 1;

    private static final String[] traversals = {
        "g.v(x).out.out.groupCount.cap.next()",
        "m=[:];g.v(x).in('sung_by').out('followed_by').groupCount(m).loop(2){it.loops<3}.iterate();m",
        "c=0;m=[:];g.v(x).out.groupCount(m){it.name}.loop(2){c++ < 300}.iterate();m"
    };

    private static final int[] artists = {339,748,530,745,534,533,749,532,539,537,536,743,741,342,344,345,347,540,734,737,542,544,739,340,546,548,549,730,732,729,511,510,726,724,721,518,516,514,716,523,718,712,520,714,525,528,527,570,574,704,572,701,573,579,708,577,706,581,583,585,587,559,556,557,554,552,553,551,569,566,567,561,562,564,799,798,796,793,790,791,781,783,785,787,789,505,773,771,507,775,501,500,763,765,760,769,755,753,751,758,757,594,391,596,598,394,591,592,383,384,381,380,388,372,373,375,377,376,362,368,367,350,351,352,354,356,359,806,807,804,803,600,609,605,602,408,405,407,401,416,415,623,622,627,629,412,628,426,428,611,614,615,618,422,434,430,444,446,447,442,449,453,451,457,458,456,459,461,460,462,466,467,469,686,688,683,681,677,674,673,671,679,670,694,699,691,646,649,642,644,645,640,637,635,632,668,669,662,666,659,657,658,652,650,654,486,484,488,483,480,481,474,473,476,475,478,479,471,495,498,497,491,493,490};
    private static final int[] songs = {338,332,333,330,331,336,337,334,335,531,747,746,535,740,538,744,742,349,341,343,346,348,3,735,2,1,541,736,7,6,543,738,5,4,545,547,9,8,731,733,318,319,316,317,314,315,312,313,310,311,513,512,728,727,725,723,722,519,720,517,515,327,328,329,323,324,325,326,709,320,321,322,522,717,521,524,719,713,715,529,711,710,526,571,700,703,575,702,578,707,576,705,300,302,580,301,304,582,303,306,305,308,307,309,584,586,588,589,558,555,550,560,565,568,563,797,795,794,792,782,784,786,788,780,772,506,770,503,504,776,509,777,774,508,778,779,502,764,766,761,762,767,768,754,752,750,759,756,595,597,390,393,599,392,395,396,397,398,399,590,593,382,389,387,386,385,370,371,379,378,374,361,360,366,365,364,363,369,353,355,357,358,805,802,800,801,159,158,157,156,155,154,152,601,153,150,151,608,607,606,604,603,202,203,204,205,200,201,169,166,206,165,207,208,168,167,209,161,409,162,163,164,404,160,406,400,403,402,211,212,210,215,216,213,214,219,179,178,177,217,176,218,170,418,171,417,174,175,172,621,173,419,620,410,626,625,624,414,413,411,220,221,222,223,224,225,226,227,228,188,229,187,189,427,180,181,429,182,183,610,184,185,612,186,808,613,421,616,420,423,617,425,424,619,116,117,114,115,112,113,110,111,118,119,435,436,433,431,432,439,437,438,125,126,127,128,121,122,123,124,129,445,440,441,443,120,448,134,135,132,133,138,139,136,137,450,454,452,455,131,130,143,144,145,146,147,148,149,463,464,465,468,140,142,141,687,689,682,685,684,680,678,676,675,672,696,695,693,698,697,692,690,647,648,195,194,643,197,196,191,190,193,192,641,198,199,639,638,636,633,634,631,630,660,661,663,664,665,667,651,655,656,653,35,36,33,34,39,37,38,43,42,41,40,22,23,24,25,26,27,28,29,30,32,31,19,17,18,15,16,13,14,11,12,21,20,10,79,78,77,82,83,80,81,86,87,84,85,67,66,69,68,70,71,72,73,74,75,76,59,58,57,56,55,64,65,62,63,60,61,49,48,45,44,47,46,51,52,53,54,50,281,487,280,485,285,284,283,489,282,288,289,286,287,482,270,272,271,477,274,273,275,276,277,278,279,470,472,109,108,107,106,105,104,103,102,99,101,100,98,97,96,95,94,93,92,91,294,90,293,499,296,295,290,496,292,291,492,494,297,298,299,88,89,240,241,245,244,243,242,249,248,247,246,230,239,232,231,234,233,236,235,238,237,262,263,260,261,269,268,267,266,265,264,250,251,252,258,257,259,254,253,256,255};

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
        tryRexproSessionless(getRexsterClientMsgPackGratefulGraph());
    }

    private void tryRexproJsonSessionless() throws Exception {
        tryRexproSessionless(getRexsterClientJsonGratefulGraph());
    }

    private void tryRexproSessionless(final RexsterClient client) throws Exception {
        String traversal = traversals[0];
        for (int iy = 1; iy < 26; iy++) {
            final Map<String, Object> m = new HashMap<String, Object>();
            m.put("x", iy * 8);

            final List<Map<String, Object>> results = client.execute(traversal, m);
            Assert.assertNotNull(results);
        }

        traversal = traversals[1];
        for (int ix = 0; ix < 25; ix++) {
            final Map<String, Object> m = new HashMap<String, Object>();
            m.put("x", artists[ix]);

            final List<Map<String, Object>> results = client.execute(traversal, m);
            Assert.assertNotNull(results);
        }

        traversal = traversals[2];
        for (int ix = 100; ix < 125; ix++) {
            final Map<String, Object> m = new HashMap<String, Object>();
            m.put("x", songs[ix]);

            final List<Map<String, Object>> results = client.execute(traversal, m);
            Assert.assertNotNull(results);
        }
    }

    private void tryRestGremlin() throws Exception {
        String traversal = traversals[0];
        for (int iy = 1; iy < 26; iy++) {
            final String url = getHttpBaseUri() + "graphs/gratefulgraph/tp/gremlin?script=" + URLEncoder.encode(traversal) + "&rexster.offset.end=" + Long.MAX_VALUE + "&params.x=";
            final ClientRequest request = ClientRequest.create().build(URI.create(url + String.valueOf(iy * 8)), "GET");
            final ClientResponse response = httpClient.handle(request);
            final JSONObject json = response.getEntity(JSONObject.class);
            Assert.assertTrue(json.optBoolean("success"));
        }

        traversal = traversals[1];
        for (int ix = 0; ix < 25; ix++) {
            final String url = getHttpBaseUri() + "graphs/gratefulgraph/tp/gremlin?script=" + URLEncoder.encode(traversal) + "&rexster.offset.end=" + Long.MAX_VALUE + "&params.x=";
            final ClientRequest request = ClientRequest.create().build(URI.create(url + String.valueOf(artists[ix])), "GET");
            final ClientResponse response = httpClient.handle(request);
            final JSONObject json = response.getEntity(JSONObject.class);
            Assert.assertTrue(json.optBoolean("success"));
        }

        traversal = traversals[2];
        for (int ix = 100; ix < 125; ix++) {
            final String url = getHttpBaseUri() + "graphs/gratefulgraph/tp/gremlin?script=" + URLEncoder.encode(traversal) + "&rexster.offset.end=" + Long.MAX_VALUE + "&params.x=";
            final ClientRequest request = ClientRequest.create().build(URI.create(url + String.valueOf(songs[ix])), "GET");
            final ClientResponse response = httpClient.handle(request);
            final JSONObject json = response.getEntity(JSONObject.class);
            Assert.assertTrue(json.optBoolean("success"));
        }
    }
}