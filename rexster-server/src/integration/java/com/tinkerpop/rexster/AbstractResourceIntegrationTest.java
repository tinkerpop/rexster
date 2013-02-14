package com.tinkerpop.rexster;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientRequest;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.tinkerpop.rexster.protocol.EngineController;
import com.tinkerpop.rexster.server.HttpRexsterServer;
import com.tinkerpop.rexster.server.RexsterApplication;
import com.tinkerpop.rexster.server.RexsterServer;
import com.tinkerpop.rexster.server.XmlRexsterApplication;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.codehaus.jettison.json.JSONObject;

import javax.ws.rs.core.MediaType;
import java.io.File;
import java.net.URI;
import java.net.URLEncoder;
import java.util.List;

public abstract class AbstractResourceIntegrationTest {

    protected static String BASE_URI = "http://127.0.0.1:8182";
    protected RexsterServer rexsterServer;
    protected final ClientConfig clientConfiguration = new DefaultClientConfig();
    protected Client client;

    static {
        EngineController.configure(-1, null);
    }

    public void setUp() throws Exception {
        clean();

        final XMLConfiguration properties = new XMLConfiguration();
        properties.load(Application.class.getResourceAsStream("rexster-integration-test.xml"));
        rexsterServer = new HttpRexsterServer(properties);

        final List<HierarchicalConfiguration> graphConfigs = properties.configurationsAt(Tokens.REXSTER_GRAPH_PATH);
        final RexsterApplication application = new XmlRexsterApplication(graphConfigs);
        rexsterServer.start(application);

        client = Client.create(clientConfiguration);
    }

    public void tearDown() throws Exception {
        rexsterServer.stop();
    }

    protected URI createUri(String path) {
        return URI.create(BASE_URI + "/graphs" + path);
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
        return this.client.handle(graphRequest);
    }

    protected ClientResponse doPost(String path, String query) {
        String uri = makeUriString(path, query);

        ClientRequest graphRequest = ClientRequest.create().build(createUri("/" + uri), "POST");
        return this.client.handle(graphRequest);
    }

    protected ClientResponse doPut(String path, String query) {
        String uri = makeUriString(path, query);

        ClientRequest graphRequest = ClientRequest.create().build(createUri("/" + uri), "PUT");
        return this.client.handle(graphRequest);
    }

    protected ClientResponse doDelete(String path, String query) {
        String uri = makeUriString(path, query);

        ClientRequest graphRequest = ClientRequest.create().build(createUri("/" + uri), "DELETE");
        return this.client.handle(graphRequest);
    }

    protected ClientResponse doPostOfJson(String path, String query, JSONObject jsonToPost) {
        String uri = makeUriString(path, query);

        ClientRequest graphRequest = ClientRequest.create().type(MediaType.APPLICATION_JSON_TYPE).build(createUri("/" + uri), "POST");
        graphRequest.setEntity(jsonToPost);

        return this.client.handle(graphRequest);
    }

    protected ClientResponse doPutOfJson(String path, String query, JSONObject jsonToPost) {
        String uri = makeUriString(path, query);

        ClientRequest graphRequest = ClientRequest.create().type(MediaType.APPLICATION_JSON_TYPE).build(createUri("/" + uri), "PUT");
        graphRequest.setEntity(jsonToPost);

        return this.client.handle(graphRequest);
    }

    protected ClientResponse doDeleteOfJson(String path, String query, JSONObject jsonToDelete) {
        String uri = makeUriString(path, query);

        ClientRequest graphRequest = ClientRequest.create().type(MediaType.APPLICATION_JSON_TYPE).build(createUri("/" + uri), "DELETE");
        graphRequest.setEntity(jsonToDelete);

        return this.client.handle(graphRequest);
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

    private static void clean() {
        removeDirectory(new File("/tmp/rexster-integration-tests"));
    }

    private static boolean removeDirectory(final File directory) {
        if (directory == null)
            return false;
        if (!directory.exists())
            return true;
        if (!directory.isDirectory())
            return false;

        final String[] list = directory.list();

        if (list != null) {
            for (int i = 0; i < list.length; i++) {
                final File entry = new File(directory, list[i]);
                if (entry.isDirectory())
                {
                    if (!removeDirectory(entry))
                        return false;
                }
                else
                {
                    if (!entry.delete())
                        return false;
                }
            }
        }

        return directory.delete();
    }
}
