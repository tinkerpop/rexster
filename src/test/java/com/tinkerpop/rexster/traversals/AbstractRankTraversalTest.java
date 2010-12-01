package com.tinkerpop.rexster.traversals;

import com.tinkerpop.blueprints.pgm.Graph;
import com.tinkerpop.blueprints.pgm.impls.tg.TinkerGraph;

import java.util.Map;

import org.codehaus.jettison.json.JSONException;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class AbstractRankTraversalTest {

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

    @Test
    public void testRankOffsets() throws JSONException{
        TestTraversal tt = new TestTraversal();
        buildTestRanks(tt.idToElement);
        tt.generateRankList();
        tt.sortRanks("rank");
        Assert.assertEquals(5, tt.ranks.size());
        tt.offsetRanks();
        Assert.assertEquals(5, tt.ranks.size());
        tt.ranks.clear();
        Assert.assertEquals(0, tt.ranks.size());

        buildTestRanks(tt.idToElement);
        tt.generateRankList();
        Assert.assertEquals(5, tt.ranks.size());
        tt.sort = AbstractRankTraversal.Sort.REVERSE;
        tt.sortRanks("rank");
        tt.startOffset = 0;
        tt.endOffset = 3;
        tt.offsetRanks();
        Assert.assertEquals(3, tt.ranks.size());
        Assert.assertEquals("e", tt.ranks.get(0).getId());
        Assert.assertEquals("d", tt.ranks.get(1).getId());
        Assert.assertEquals("c", tt.ranks.get(2).getId());
        tt.ranks.clear();
        Assert.assertEquals(0, tt.ranks.size());

        buildTestRanks(tt.idToElement);
        tt.generateRankList();
        Assert.assertEquals(5, tt.ranks.size());
        tt.sort = AbstractRankTraversal.Sort.REGULAR;
        tt.sortRanks("rank");
        tt.startOffset = 0;
        tt.endOffset = 3;
        tt.offsetRanks();
        Assert.assertEquals(3, tt.ranks.size());
        Assert.assertEquals("a", tt.ranks.get(0).getId());
        Assert.assertEquals("b", tt.ranks.get(1).getId());
        Assert.assertEquals("c", tt.ranks.get(2).getId());
        tt.ranks.clear();
        Assert.assertEquals(0, tt.ranks.size());

        buildTestRanks(tt.idToElement);
        tt.generateRankList();
        Assert.assertEquals(5, tt.ranks.size());
        tt.sort = AbstractRankTraversal.Sort.REGULAR;
        tt.sortRanks("rank");
        tt.startOffset = 0;
        tt.endOffset = 3;
        tt.offsetRanks();
        Assert.assertEquals(3, tt.ranks.size());
        Assert.assertEquals("a", tt.ranks.get(0).getId());
        Assert.assertEquals("b", tt.ranks.get(1).getId());
        Assert.assertEquals("c", tt.ranks.get(2).getId());
        tt.ranks.clear();
        Assert.assertEquals(0, tt.ranks.size());

        buildTestRanks(tt.idToElement);
        tt.generateRankList();
        Assert.assertEquals(5, tt.ranks.size());
        tt.sort = AbstractRankTraversal.Sort.REVERSE;
        tt.sortRanks("rank");
        tt.startOffset = 4;
        tt.endOffset = -1;
        tt.offsetRanks();
        Assert.assertEquals(1, tt.ranks.size());
        Assert.assertEquals("a", tt.ranks.get(0).getId());
        tt.ranks.clear();
        Assert.assertEquals(0, tt.ranks.size());


        buildTestRanks(tt.idToElement);
        tt.generateRankList();
        Assert.assertEquals(5, tt.ranks.size());
        tt.sort = AbstractRankTraversal.Sort.REVERSE;
        tt.sortRanks("rank");
        tt.startOffset = 3;
        tt.endOffset = 4;
        tt.offsetRanks();
        Assert.assertEquals(1, tt.ranks.size());
        Assert.assertEquals("b", tt.ranks.get(0).getId());
        tt.ranks.clear();
        Assert.assertEquals(0, tt.ranks.size());

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


		@Override
		protected void traverse() {

        }

		@Override
		protected void addApiToResultObject() {
			// TODO Auto-generated method stub
			
		}
    }

}
