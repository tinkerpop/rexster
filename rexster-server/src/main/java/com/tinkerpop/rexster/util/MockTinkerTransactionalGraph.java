package com.tinkerpop.rexster.util;

import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.blueprints.impls.tg.TinkerGraph;

/**
 * Mocked transactional graph for testing purposes.
 * <p/>
 * This class doesn't really do anything Transactional.
 *
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class MockTinkerTransactionalGraph extends TinkerGraph implements TransactionalGraph {

    public MockTinkerTransactionalGraph(final String directory, final FileType fileType) {
        super(directory, fileType);
    }

    public MockTinkerTransactionalGraph() {
        super();
    }

    public void stopTransaction(final Conclusion conclusion) {

    }

    public void commit() {
    }

    public void rollback() {
    }
}
