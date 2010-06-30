package com.tinkerpop.rexster;

import junit.framework.TestCase;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.InputStreamReader;
import java.net.URL;
import java.util.Properties;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class BaseTest extends TestCase {

    private WebServer webServer;
    protected StatisticsHelper sh = new StatisticsHelper();
    private static JSONParser parser = new JSONParser();
    public static final String baseURI = "http://localhost:8182/";

    public void testTrue() {
        assertTrue(true);
    }

    public void startWebServer() throws Exception {
        Thread thread = new Thread() {
            public void run() {
                try {
                    Properties properties = new Properties();
                    properties.load(RexsterApplication.class.getResourceAsStream("rexster.properties"));
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
        return (JSONObject) parser.parse(new InputStreamReader(new URL(uri).openStream()));
    }

    public static String createURI(String extension) {
        return baseURI + extension;
    }

    public static void printPerformance(String name, Integer events, String eventName, double timeInMilliseconds) {
        if (null != events)
            System.out.println("\t" + name + ": " + events + " " + eventName + " in " + timeInMilliseconds + "ms");
        else
            System.out.println("\t" + name + ": " + eventName + " in " + timeInMilliseconds + "ms");
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
