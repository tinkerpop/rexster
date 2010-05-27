package com.tinkerpop.rexster.traversals;

import junit.framework.TestCase;

/**
 * @author: Marko A. Rodriguez (http://markorodriguez.com)
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
    }

    public void testRankOffsets() {
        TestTraversal tt = new TestTraversal();
        buildTestRanks(tt.ranks);
        assertEquals(tt.ranks.size(), 5);
        tt.offsetRanks();
        assertEquals(tt.ranks.size(), 5);
        tt.ranks.clear();
        assertEquals(tt.ranks.size(), 0);

        buildTestRanks(tt.ranks);
        assertEquals(tt.ranks.size(), 5);
        tt.sortRanks();
        tt.startOffset = 0;
        tt.endOffset = 3;
        tt.offsetRanks();
        assertEquals(tt.ranks.size(), 3);
        assertTrue(tt.ranks.containsKey("a"));
        assertTrue(tt.ranks.containsKey("b"));
        assertTrue(tt.ranks.containsKey("c"));
        assertFalse(tt.ranks.containsKey("d"));
        assertFalse(tt.ranks.containsKey("e"));
        tt.ranks.clear();
        assertEquals(tt.ranks.size(), 0);

        buildTestRanks(tt.ranks);
        assertEquals(tt.ranks.size(), 5);
        tt.sortRanks();
        tt.startOffset = 1;
        tt.endOffset = 3;
        tt.offsetRanks();
        assertEquals(tt.ranks.size(), 2);
        assertFalse(tt.ranks.containsKey("a"));
        assertTrue(tt.ranks.containsKey("b"));
        assertTrue(tt.ranks.containsKey("c"));
        assertFalse(tt.ranks.containsKey("d"));
        assertFalse(tt.ranks.containsKey("e"));
        tt.ranks.clear();
        assertEquals(tt.ranks.size(), 0);

        buildTestRanks(tt.ranks);
        assertEquals(tt.ranks.size(), 5);
        tt.sortRanks();
        tt.startOffset = 4;
        tt.endOffset = -1;
        tt.offsetRanks();
        assertEquals(tt.ranks.size(), 1);
        assertFalse(tt.ranks.containsKey("a"));
        assertFalse(tt.ranks.containsKey("b"));
        assertFalse(tt.ranks.containsKey("c"));
        assertFalse(tt.ranks.containsKey("d"));
        assertTrue(tt.ranks.containsKey("e"));
        tt.ranks.clear();
        assertEquals(tt.ranks.size(), 0);


        buildTestRanks(tt.ranks);
        assertEquals(tt.ranks.size(), 5);
        tt.sortRanks();
        tt.startOffset = 3;
        tt.endOffset = 4;
        tt.offsetRanks();
        assertEquals(tt.ranks.size(), 1);
        assertFalse(tt.ranks.containsKey("a"));
        assertFalse(tt.ranks.containsKey("b"));
        assertFalse(tt.ranks.containsKey("c"));
        assertTrue(tt.ranks.containsKey("d"));
        assertFalse(tt.ranks.containsKey("e"));
        tt.ranks.clear();
        assertEquals(tt.ranks.size(), 0);

        buildTestRanks(tt.ranks);
        assertEquals(tt.ranks.size(), 5);
        tt.sortRanks();
        tt.startOffset = 4;
        tt.endOffset = 3;
        tt.offsetRanks();
        assertEquals(tt.ranks.size(), 0);
        assertFalse(tt.ranks.containsKey("a"));
        assertFalse(tt.ranks.containsKey("b"));
        assertFalse(tt.ranks.containsKey("c"));
        assertFalse(tt.ranks.containsKey("d"));
        assertFalse(tt.ranks.containsKey("e"));
        tt.ranks.clear();
        assertEquals(tt.ranks.size(), 0);

    }

    private static void buildTestRanks(Map<Object, Float> testRanks) {
        testRanks.put("a", 1.0f);
        testRanks.put("b", 2.0f);
        testRanks.put("c", 3.0f);
        testRanks.put("d", 4.0f);
        testRanks.put("e", 5.0f);
    }

    private class TestTraversal extends AbstractRankTraversal {
        public String getTraversalName() {
            return "test-traversal";
        }

        public void traverse() {

        }
    }*/

}
