package com.tinkerpop.rexster;

import com.sun.jersey.core.util.MultivaluedMapImpl;
import com.tinkerpop.blueprints.pgm.impls.sail.SailGraph;
import com.tinkerpop.blueprints.pgm.impls.sail.SailGraphFactory;
import com.tinkerpop.blueprints.pgm.impls.sail.impls.MemoryStoreSailGraph;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class PrefixResourceTest {

    protected Mockery mockery = new JUnit4Mockery();
    protected final String baseUri = "http://localhost/mock";

    @Before
    public void init() {
        this.mockery = new JUnit4Mockery();
    }

    @Test
    public void getPrefixesValid() {

        SailGraph sg = new MemoryStoreSailGraph();
        SailGraphFactory.createTinkerGraph(sg);
        final RexsterApplicationGraph rag = new RexsterApplicationGraph("graph", sg);
        final RexsterApplicationProvider rap = this.mockery.mock(RexsterApplicationProvider.class);

        final UriInfo uri = this.mockery.mock(UriInfo.class);
        final HttpServletRequest httpServletRequest = this.mockery.mock(HttpServletRequest.class);

        this.mockery.checking(new Expectations() {{
            allowing(httpServletRequest).getParameterMap();
            will(returnValue(new HashMap<String, String>()));
            allowing(rap).getApplicationGraph(with(any(String.class)));
            will(returnValue(rag));
        }});

        PrefixResource resource = new PrefixResource(uri, httpServletRequest, rap);
        Response response = resource.getPrefixes("graph");

        Assert.assertNotNull(response);
        Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        JSONObject jsonObject = (JSONObject) response.getEntity();
        Assert.assertNotNull(jsonObject);

        Assert.assertTrue(jsonObject.has(Tokens.RESULTS));
        JSONArray results = jsonObject.optJSONArray(Tokens.RESULTS);
        Assert.assertNotNull(results);
        Assert.assertEquals(6, results.length());

        Assert.assertTrue(jsonObject.has(Tokens.QUERY_TIME));
    }

    @Test
    public void getSinglePrefixValid() {

        SailGraph sg = new MemoryStoreSailGraph();
        SailGraphFactory.createTinkerGraph(sg);
        final RexsterApplicationGraph rag = new RexsterApplicationGraph("graph", sg);
        final RexsterApplicationProvider rap = this.mockery.mock(RexsterApplicationProvider.class);

        final UriInfo uri = this.mockery.mock(UriInfo.class);
        final HttpServletRequest httpServletRequest = this.mockery.mock(HttpServletRequest.class);

        this.mockery.checking(new Expectations() {{
            allowing(httpServletRequest).getParameterMap();
            will(returnValue(new HashMap<String, String>()));
            allowing(rap).getApplicationGraph(with(any(String.class)));
            will(returnValue(rag));
        }});

        PrefixResource resource = new PrefixResource(uri, httpServletRequest, rap);
        Response response = resource.getSinglePrefix("graph", "tg");

        Assert.assertNotNull(response);
        Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        JSONObject jsonObject = (JSONObject) response.getEntity();
        Assert.assertNotNull(jsonObject);

        Assert.assertTrue(jsonObject.has(Tokens.RESULTS));
        Assert.assertEquals("http://tinkerpop.com#", jsonObject.optString(Tokens.RESULTS));

        Assert.assertTrue(jsonObject.has(Tokens.QUERY_TIME));
    }

    @Test
    public void deleteSinglePrefixValid() {

        SailGraph sg = new MemoryStoreSailGraph();
        SailGraphFactory.createTinkerGraph(sg);
        final RexsterApplicationGraph rag = new RexsterApplicationGraph("graph", sg);
        final RexsterApplicationProvider rap = this.mockery.mock(RexsterApplicationProvider.class);

        final UriInfo uri = this.mockery.mock(UriInfo.class);
        final HttpServletRequest httpServletRequest = this.mockery.mock(HttpServletRequest.class);

        this.mockery.checking(new Expectations() {{
            allowing(httpServletRequest).getParameterMap();
            will(returnValue(new HashMap<String, String>()));
            allowing(rap).getApplicationGraph(with(any(String.class)));
            will(returnValue(rag));
        }});

        PrefixResource resource = new PrefixResource(uri, httpServletRequest, rap);
        Response response = resource.deleteSinglePrefix("graph", "tg");

        Assert.assertNotNull(response);
        Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        JSONObject jsonObject = (JSONObject) response.getEntity();
        Assert.assertNotNull(jsonObject);

        Assert.assertTrue(jsonObject.has(Tokens.QUERY_TIME));

        Assert.assertEquals(5, sg.getNamespaces().keySet().toArray().length);
    }

    @Test(expected = WebApplicationException.class)
    public void PostSinglePrefixNoNamespaceParam() {

        SailGraph sg = new MemoryStoreSailGraph();
        SailGraphFactory.createTinkerGraph(sg);
        final RexsterApplicationGraph rag = new RexsterApplicationGraph("graph", sg);
        final RexsterApplicationProvider rap = this.mockery.mock(RexsterApplicationProvider.class);

        final UriInfo uri = this.mockery.mock(UriInfo.class);
        final HttpServletRequest httpServletRequest = this.mockery.mock(HttpServletRequest.class);

        this.mockery.checking(new Expectations() {{
            allowing(httpServletRequest).getParameterMap();
            will(returnValue(new HashMap<String, String>()));
            allowing(rap).getApplicationGraph(with(any(String.class)));
            will(returnValue(rag));
        }});

        PrefixResource resource = new PrefixResource(uri, httpServletRequest, rap);
        MultivaluedMap<String, String> map = new MultivaluedMapImpl();
        List<String> listNamespace = new ArrayList<String>();
        listNamespace.add("http://ddd.com#");
        map.put("namespace", listNamespace);

        List<String> listPrefix = new ArrayList<String>();
        listPrefix.add("ddd");
        map.put("xxx", listPrefix);

        resource.postSinglePrefix("graph", map);
    }

    @Test(expected = WebApplicationException.class)
    public void PostSinglePrefixNoPrefixParam() {

        SailGraph sg = new MemoryStoreSailGraph();
        SailGraphFactory.createTinkerGraph(sg);
        final RexsterApplicationGraph rag = new RexsterApplicationGraph("graph", sg);
        final RexsterApplicationProvider rap = this.mockery.mock(RexsterApplicationProvider.class);

        final UriInfo uri = this.mockery.mock(UriInfo.class);
        final HttpServletRequest httpServletRequest = this.mockery.mock(HttpServletRequest.class);

        this.mockery.checking(new Expectations() {{
            allowing(httpServletRequest).getParameterMap();
            will(returnValue(new HashMap<String, String>()));
            allowing(rap).getApplicationGraph(with(any(String.class)));
            will(returnValue(rag));
        }});

        PrefixResource resource = new PrefixResource(uri, httpServletRequest, rap);
        MultivaluedMap<String, String> map = new MultivaluedMapImpl();
        List<String> listNamespace = new ArrayList<String>();
        listNamespace.add("http://ddd.com#");
        map.put("xxx", listNamespace);

        List<String> listPrefix = new ArrayList<String>();
        listPrefix.add("ddd");
        map.put("prefix", listPrefix);

        resource.postSinglePrefix("graph", map);
    }

    @Test
    public void PostSinglePrefixValid() {

        SailGraph sg = new MemoryStoreSailGraph();
        SailGraphFactory.createTinkerGraph(sg);
        final RexsterApplicationGraph rag = new RexsterApplicationGraph("graph", sg);
        final RexsterApplicationProvider rap = this.mockery.mock(RexsterApplicationProvider.class);

        final UriInfo uri = this.mockery.mock(UriInfo.class);
        final HttpServletRequest httpServletRequest = this.mockery.mock(HttpServletRequest.class);

        this.mockery.checking(new Expectations() {{
            allowing(httpServletRequest).getParameterMap();
            will(returnValue(new HashMap<String, String>()));
            allowing(rap).getApplicationGraph(with(any(String.class)));
            will(returnValue(rag));
        }});

        PrefixResource resource = new PrefixResource(uri, httpServletRequest, rap);
        MultivaluedMap<String, String> map = new MultivaluedMapImpl();
        List<String> listNamespace = new ArrayList<String>();
        listNamespace.add("http://ddd.com#");
        map.put("namespace", listNamespace);

        List<String> listPrefix = new ArrayList<String>();
        listPrefix.add("ddd");
        map.put("prefix", listPrefix);

        Response response = resource.postSinglePrefix("graph", map);

        Assert.assertNotNull(response);
        Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        JSONObject jsonObject = (JSONObject) response.getEntity();
        Assert.assertNotNull(jsonObject);

        Assert.assertTrue(jsonObject.has(Tokens.QUERY_TIME));

        Assert.assertEquals(7, sg.getNamespaces().keySet().toArray().length);
    }
}
