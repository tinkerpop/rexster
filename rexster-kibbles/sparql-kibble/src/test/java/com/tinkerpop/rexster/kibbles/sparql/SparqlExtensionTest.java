package com.tinkerpop.rexster.kibbles.sparql;

import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.impls.sail.SailGraph;
import com.tinkerpop.blueprints.impls.sail.SailGraphFactory;
import com.tinkerpop.blueprints.impls.sail.impls.MemoryStoreSailGraph;
import com.tinkerpop.rexster.RexsterResourceContext;
import com.tinkerpop.rexster.Tokens;
import com.tinkerpop.rexster.extension.ExtensionResponse;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response;

public class SparqlExtensionTest {
    private Graph graph;
    private RexsterResourceContext ctx;

    @Before
    public void beforeTest() {

        SailGraph sailGraph = new MemoryStoreSailGraph();
        SailGraphFactory.createTinkerGraph(sailGraph);

        this.graph = sailGraph;

    }

    @Test
    public void evaluateSparqlNoReturnKeysNoShowTypes() {
        String sparqlQuery = "SELECT ?x ?y WHERE { ?x <http://tinkerpop.com#knows> ?y }";

        this.ctx = new RexsterResourceContext(null, null, null, new JSONObject(), null, null, null);

        SparqlExtension extension = new SparqlExtension();
        ExtensionResponse extensionResponse = extension.evaluateSparql(this.ctx, this.graph, sparqlQuery);

        Assert.assertNotNull(extensionResponse);
        Assert.assertFalse(extensionResponse.isErrorResponse());

        Response jerseyResponse = extensionResponse.getJerseyResponse();
        Assert.assertNotNull(jerseyResponse);
        Assert.assertEquals(Response.Status.OK.getStatusCode(), jerseyResponse.getStatus());

        JSONObject jsonResponseEntity = (JSONObject) jerseyResponse.getEntity();
        Assert.assertNotNull(jsonResponseEntity);

        Assert.assertTrue(jsonResponseEntity.has(Tokens.RESULTS));

        JSONArray results = jsonResponseEntity.optJSONArray(Tokens.RESULTS);
        Assert.assertNotNull(results);
        Assert.assertEquals(2, results.length());

    }
}
