package com.tinkerpop.rexster.traversals;

import com.tinkerpop.blueprints.pgm.Graph;
import com.tinkerpop.blueprints.pgm.impls.tg.TinkerGraph;
import junit.framework.TestCase;

import java.util.Map;

import org.codehaus.jettison.json.JSONException;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class AbstractRankTraversalTest extends TestCase {

    public void testTrue() {
        assertTrue(true);
    }

    /*public void testRankSorting() {
        TestTraversal tt = new TestTraversal();
        tt.ranks.put("a", 1.0f);
        tt.ranks.put("b", 2.0f);
        tt.ranks.put("c", 3.0f);
        tt.ranks.put("d", 4.0f);
        assertEquals(tt.ranks.size(), 4);
        tt.sortRanks();
        float counter = 1.0f;
        for (Map.Entry<Object, Float> entry : tt.ranks.entrySet()) {
            assertEquals(entry.getValue(), counter);
            if (counter == 1.0f) {
                assertEquals(entry.getKey(), "a");
            } else if (counter == 2.0f) {
                assertEquals(entry.getKey(), "b");
            } else if (counter == 3.0f) {
                assertEquals(entry.getKey(), "c");
            } else if (counter == 4.0f) {
                assertEquals(entry.getKey(), "d");
            } else {
                assertTrue(false);
            }
            counter++;
        }
        tt.sort = AbstractRankTraversal.Sort.REVERSE;
        tt.sortRanks();
        counter = 4.0f;
        for (Map.Entry<Object, Float> entry : tt.ranks.entrySet()) {
            assertEquals(entry.getValue(), counter);
            if (counter == 1.0f) {
                assertEquals(entry.getKey(), "a");
            } else if (counter == 2.0f) {
                assertEquals(entry.getKey(), "b");
            } else if (counter == 3.0f) {
                assertEquals(entry.getKey(), "c");
            } else if (counter == 4.0f) {
                assertEquals(entry.getKey(), "d");
            } else {
                assertTrue(false);
            }
            counter--;
        }
    }*/

    public void testRankOffsets() throws JSONException{
        TestTraversal tt = new TestTraversal();
        buildTestRanks(tt.idToElement);
        tt.generateRankList();
        tt.sortRanks("rank");
        assertEquals(tt.ranks.size(), 5);
        tt.offsetRanks();
        assertEquals(tt.ranks.size(), 5);
        tt.ranks.clear();
        assertEquals(tt.ranks.size(), 0);

        buildTestRanks(tt.idToElement);
        tt.generateRankList();
        assertEquals(tt.ranks.size(), 5);
        tt.sort = AbstractRankTraversal.Sort.REVERSE;
        tt.sortRanks("rank");
        tt.startOffset = 0;
        tt.endOffset = 3;
        tt.offsetRanks();
        assertEquals(tt.ranks.size(), 3);
        assertEquals(tt.ranks.get(0).getId(), "e");
        assertEquals(tt.ranks.get(1).getId(), "d");
        assertEquals(tt.ranks.get(2).getId(), "c");
        tt.ranks.clear();
        assertEquals(tt.ranks.size(), 0);

        buildTestRanks(tt.idToElement);
        tt.generateRankList();
        assertEquals(tt.ranks.size(), 5);
        tt.sort = AbstractRankTraversal.Sort.REGULAR;
        tt.sortRanks("rank");
        tt.startOffset = 0;
        tt.endOffset = 3;
        tt.offsetRanks();
        assertEquals(tt.ranks.size(), 3);
        assertEquals(tt.ranks.get(0).getId(), "a");
        assertEquals(tt.ranks.get(1).getId(), "b");
        assertEquals(tt.ranks.get(2).getId(), "c");
        tt.ranks.clear();
        assertEquals(tt.ranks.size(), 0);

        buildTestRanks(tt.idToElement);
        tt.generateRankList();
        assertEquals(tt.ranks.size(), 5);
        tt.sort = AbstractRankTraversal.Sort.REGULAR;
        tt.sortRanks("rank");
        tt.startOffset = 0;
        tt.endOffset = 3;
        tt.offsetRanks();
        assertEquals(tt.ranks.size(), 3);
        assertEquals(tt.ranks.get(0).getId(), "a");
        assertEquals(tt.ranks.get(1).getId(), "b");
        assertEquals(tt.ranks.get(2).getId(), "c");
        tt.ranks.clear();
        assertEquals(tt.ranks.size(), 0);

        buildTestRanks(tt.idToElement);
        tt.generateRankList();
        assertEquals(tt.ranks.size(), 5);
        tt.sort = AbstractRankTraversal.Sort.REVERSE;
        tt.sortRanks("rank");
        tt.startOffset = 4;
        tt.endOffset = -1;
        tt.offsetRanks();
        assertEquals(tt.ranks.size(), 1);
        assertEquals(tt.ranks.get(0).getId(), "a");
        tt.ranks.clear();
        assertEquals(tt.ranks.size(), 0);


        buildTestRanks(tt.idToElement);
        tt.generateRankList();
        assertEquals(tt.ranks.size(), 5);
        tt.sort = AbstractRankTraversal.Sort.REVERSE;
        tt.sortRanks("rank");
        tt.startOffset = 3;
        tt.endOffset = 4;
        tt.offsetRanks();
        assertEquals(tt.ranks.size(), 1);
        assertEquals(tt.ranks.get(0).getId(), "b");
        tt.ranks.clear();
        assertEquals(tt.ranks.size(), 0);

    }

    private static void buildTestRanks(Map<Object, ElementJSONObject> testRanks) throws JSONException{
        Graph graph = new TinkerGraph();
        ElementJSONObject temp = new ElementJSONObject(graph.addVertex("a"));
        temp.put("rank", 1.0f);
        testRanks.put("a", temp);
        temp = new ElementJSONObject(graph.addVertex("b"));
        temp.put("rank", 2.0f);
        testRanks.put("b", temp);
        temp = new ElementJSONObject(graph.addVertex("c"));
        temp.put("rank", 3.0f);
        testRanks.put("c", temp);
        temp = new ElementJSONObject(graph.addVertex("d"));
        temp.put("rank", 4.0f);
        testRanks.put("d", temp);
        temp = new ElementJSONObject(graph.addVertex("e"));
        temp.put("rank", 5.0f);
        testRanks.put("e", temp);
    }

    private class TestTraversal extends AbstractRankTraversal {
        public String getTraversalName() {
            return "test-traversal";
        }

        public void traverse() {

        }
    }

}
