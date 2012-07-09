package com.tinkerpop.rexster;

import com.sun.jersey.api.client.ClientRequest;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.test.framework.JerseyTest;
import org.apache.commons.configuration.XMLConfiguration;
import org.codehaus.jettison.json.JSONObject;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URLEncoder;

public abstract class AbstractResourceIntegrationTest extends JerseyTest {

    protected static String BASE_URI = "http://localhost:9998";

    public AbstractResourceIntegrationTest() throws Exception {
        super("com.tinkerpop.rexster");

        XMLConfiguration properties = new XMLConfiguration();
        properties.load(RexsterApplicationImpl.class.getResourceAsStream("rexster-integration-test.xml"));
        WebServerRexsterApplicationProvider.start(properties);
    }

    protected URI createUri(String path) {
        return URI.create(BASE_URI + "/graphs" + path);
    }

    @Override
    protected int getPort(int defaultPort) {

        ServerSocket server = null;
        int port = -1;
        try {
            server = new ServerSocket(defaultPort);
            port = server.getLocalPort();
        } catch (IOException e) {
            // ignore
        } finally {
            if (server != null) {
                try {
                    server.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
        if ((port != -1) || (defaultPort == 0)) {
            BASE_URI = "http://localhost:" + port;
            return port;
        }
        return getPort(0);
    }

    protected ClientResponse doGraphGet(GraphTestHolder testGraph, String path) {
        return doGraphGet(testGraph, path, null);
    }

    protected ClientResponse doGraphPost(GraphTestHolder testGraph, String path) {
        return doGraphPost(testGraph, path, null);
    }

    protected ClientResponse doGraphPut(GraphTestHolder testGraph, String path) {
        return doGraphPut(testGraph, path, null);
    }

    protected ClientResponse doGraphDelete(GraphTestHolder testGraph, String path) {
        return doGraphDelete(testGraph, path, null);
    }

    protected ClientResponse doGraphPostOfJson(GraphTestHolder testGraph, String path, JSONObject jsontoPost) {
        return doGraphPostOfJson(testGraph, path, null, jsontoPost);
    }

    protected ClientResponse doGraphPutOfJson(GraphTestHolder testGraph, String path, JSONObject jsontoPut) {
        return doGraphPutOfJson(testGraph, path, null, jsontoPut);
    }

    protected ClientResponse doGraphDeleteOfJson(GraphTestHolder testGraph, String path, JSONObject jsontoDelete) {
        return doGraphDeleteOfJson(testGraph, path, null, jsontoDelete);
    }

    protected ClientResponse doGraphGet(GraphTestHolder testGraph, String path, String query) {
        String uri = makeGraphUriString(testGraph, path);

        return doGet(uri, query);
    }

    protected ClientResponse doGraphPost(GraphTestHolder testGraph, String path, String query) {
        String uri = makeGraphUriString(testGraph, path);

        return doPost(uri, query);
    }

    protected ClientResponse doGraphPut(GraphTestHolder testGraph, String path, String query) {
        String uri = makeGraphUriString(testGraph, path);

        return doPut(uri, query);
    }

    protected ClientResponse doGraphDelete(GraphTestHolder testGraph, String path, String query) {
        String uri = makeGraphUriString(testGraph, path);

        return doDelete(uri, query);
    }

    protected ClientResponse doGraphPostOfJson(GraphTestHolder testGraph, String path, String query, JSONObject jsonToPost) {
        String uri = makeGraphUriString(testGraph, path);

        return doPostOfJson(uri, query, jsonToPost);
    }

    protected ClientResponse doGraphPutOfJson(GraphTestHolder testGraph, String path, String query, JSONObject jsonToPut) {
        String uri = makeGraphUriString(testGraph, path);

        return doPutOfJson(uri, query, jsonToPut);
    }

    protected ClientResponse doGraphDeleteOfJson(GraphTestHolder testGraph, String path, String query, JSONObject jsonToDelete) {
        String uri = makeGraphUriString(testGraph, path);

        return doDeleteOfJson(uri, query, jsonToDelete);
    }

    protected ClientResponse doGet(String path, String query) {
        String uri = makeUriString(path, query);

        ClientRequest graphRequest = ClientRequest.create().build(createUri("/" + uri), "GET");
        return this.client().handle(graphRequest);
    }

    protected ClientResponse doPost(String path, String query) {
        String uri = makeUriString(path, query);

        ClientRequest graphRequest = ClientRequest.create().build(createUri("/" + uri), "POST");
        return this.client().handle(graphRequest);
    }

    protected ClientResponse doPut(String path, String query) {
        String uri = makeUriString(path, query);

        ClientRequest graphRequest = ClientRequest.create().build(createUri("/" + uri), "PUT");
        return this.client().handle(graphRequest);
    }

    protected ClientResponse doDelete(String path, String query) {
        String uri = makeUriString(path, query);

        ClientRequest graphRequest = ClientRequest.create().build(createUri("/" + uri), "DELETE");
        return this.client().handle(graphRequest);
    }

    protected ClientResponse doPostOfJson(String path, String query, JSONObject jsonToPost) {
        String uri = makeUriString(path, query);

        ClientRequest graphRequest = ClientRequest.create().type(MediaType.APPLICATION_JSON_TYPE).build(createUri("/" + uri), "POST");
        graphRequest.setEntity(jsonToPost);

        return this.client().handle(graphRequest);
    }

    protected ClientResponse doPutOfJson(String path, String query, JSONObject jsonToPost) {
        String uri = makeUriString(path, query);

        ClientRequest graphRequest = ClientRequest.create().type(MediaType.APPLICATION_JSON_TYPE).build(createUri("/" + uri), "PUT");
        graphRequest.setEntity(jsonToPost);

        return this.client().handle(graphRequest);
    }

    protected ClientResponse doDeleteOfJson(String path, String query, JSONObject jsonToDelete) {
        String uri = makeUriString(path, query);

        ClientRequest graphRequest = ClientRequest.create().type(MediaType.APPLICATION_JSON_TYPE).build(createUri("/" + uri), "DELETE");
        graphRequest.setEntity(jsonToDelete);

        return this.client().handle(graphRequest);
    }

    private String makeGraphUriString(GraphTestHolder testGraph, String path) {
        String uri = testGraph.getGraphName();
        if (path != null && !path.isEmpty()) {
            uri = uri + "/" + path;
        }
        return uri;
    }

    private String makeUriString(String path, String query) {
        String uri = path;
        if (query != null && !query.isEmpty()) {
            uri = uri + "?" + query;
        }

        return uri;
    }

    public static String encode(final Object id) {
        if (id instanceof String)
            return URLEncoder.encode(id.toString());
        else
            return id.toString();
    }
}
