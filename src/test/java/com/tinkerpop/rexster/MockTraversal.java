package com.tinkerpop.rexster;

import org.codehaus.jettison.json.JSONObject;

import com.tinkerpop.rexster.traversals.Traversal;
import com.tinkerpop.rexster.traversals.TraversalException;

public class MockTraversal implements Traversal {

	@Override
	public JSONObject evaluate(RexsterResourceContext ctx)
			throws TraversalException {
		return null;
	}

	@Override
	public String getTraversalName() {
		return "mock";
	}

	public class MockEvilTraversal extends MockTraversal {

		@Override
		public JSONObject evaluate(RexsterResourceContext ctx)
				throws TraversalException {
			throw new TraversalException("didn't work");
		}

	}
}


