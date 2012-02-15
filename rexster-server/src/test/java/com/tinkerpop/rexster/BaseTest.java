package com.tinkerpop.rexster;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.apache.commons.configuration.XMLConfiguration;
import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.json.JSONTokener;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public abstract class BaseTest {

    private WebServer webServer;
    protected StatisticsHelper sh = new StatisticsHelper();
    private static Logger logger = Logger.getLogger(BaseTest.class.getName());
    public static final String baseURI = "http://localhost:8182/";

    public BaseTest() {
        try {
            XMLConfiguration properties = new XMLConfiguration();
            properties.load(RexsterApplication.class.getResourceAsStream("rexster.xml"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void startWebServer() throws Exception {
        Thread thread = new Thread() {
            public void run() {
                try {
                    XMLConfiguration properties = new XMLConfiguration();
                    properties.load(RexsterApplication.class.getResourceAsStream("rexster.xml"));
                    webServer = new WebServer(properties, false);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        thread.start();
        Thread.sleep(1500);
    }

    public void stopWebServer() throws Exception {
        webServer.stop();
    }

    public static JSONObject getResource(String uri) throws Exception {
        Client c = Client.create();
        WebResource r = c.resource(uri);

        ClientResponse response = r.get(ClientResponse.class);
        InputStream stream = response.getEntityInputStream();

        String entity = readToString(stream);

        JSONTokener tokener = new JSONTokener(entity);
        return new JSONObject(tokener);
    }

    public static JSONObject postResource(String uri) throws Exception {
        Client c = Client.create();
        WebResource r = c.resource(uri);

        ClientResponse response = r.post(ClientResponse.class);
        InputStream stream = response.getEntityInputStream();

        String entity = readToString(stream);

        JSONTokener tokener = new JSONTokener(entity);
        return new JSONObject(tokener);
    }

    public static void deleteResource(String uri) throws Exception {
        Client c = Client.create();
        WebResource r = c.resource(uri);
        r.delete();
    }

    public static String createURI(String extension) {
        return createURI("gratefulgraph", extension);
    }

    public static String createURI(String graphName, String extension) {
        String uri = baseURI + graphName;
        if (extension != null && extension.trim().length() > 0) {
            return uri + "/" + extension;
        }

        return uri;
    }

    public static void printPerformance(String name, Integer events, String eventName, double timeInMilliseconds) {
        if (null != events)
            logger.info(name + ": " + events + " " + eventName + " in " + timeInMilliseconds + "ms");
        else
            logger.info(name + ": " + eventName + " in " + timeInMilliseconds + "ms");
    }

    public static String readToString(InputStream in) throws IOException {
        StringBuffer out = new StringBuffer();
        byte[] b = new byte[4096];
        for (int n; (n = in.read(b)) != -1; ) {
            out.append(new String(b, 0, n));
        }
        return out.toString();
    }

    public class ThreadQuery implements Runnable {
        private final String uri;

        public ThreadQuery(String uri) {
            this.uri = uri;
        }

        public void run() {
            try {
                BaseTest.getResource(this.uri);
            } catch (Exception e) {
                System.out.println(e);
            }
        }
    }


}
