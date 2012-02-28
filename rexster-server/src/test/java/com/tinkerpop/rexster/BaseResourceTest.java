package com.tinkerpop.rexster;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class BaseResourceTest {

    @Test
    public void testQueryParametersToJson() throws JSONException {
        Map<String, String> qp = new HashMap<String, String>();
        qp.put("a", "true");
        qp.put("b", "false");
        qp.put("c.a", "12.0");
        qp.put("c.b", "\"marko\"");

        qp.put("c.c", "peter");
        qp.put("c.d.a.b", "true");
        qp.put("d", "[marko,rodriguez,10]");

        BaseResource tt = new MockResource(qp);

        Assert.assertTrue(tt.getRequestObject().optBoolean("a"));
        Assert.assertFalse(tt.getRequestObject().optBoolean("b"));
        Assert.assertEquals(12.0, tt.getRequestObject().optJSONObject("c").optDouble("a"), 0);
        Assert.assertEquals("\"marko\"", tt.getRequestObject().optJSONObject("c").optString("b"));
        Assert.assertEquals("peter", tt.getRequestObject().optJSONObject("c").optString("c"));
        Assert.assertTrue(tt.getRequestObject().optJSONObject("c").optJSONObject("d").optJSONObject("a").optBoolean("b"));
        Assert.assertEquals("marko", tt.getRequestObject().optJSONArray("d").optString(0));
        Assert.assertEquals("rodriguez", tt.getRequestObject().optJSONArray("d").optString(1));
        // TODO: make this not a string but a number?
        Assert.assertEquals("10", tt.getRequestObject().optJSONArray("d").optString(2));
    }

    @Test
    public void testQueryParametersToFlat() throws JSONException {
        Map<String, String> qp = new HashMap<String, String>();
        qp.put("a", "true");
        qp.put("b", "false");
        qp.put("c.a", "12.0");
        qp.put("c.b", "\"marko\"");

        qp.put("c.c", "peter");

        BaseResource tt = new MockResource(qp);

        Assert.assertTrue(tt.getRequestObjectFlat().optBoolean("a"));
        Assert.assertFalse(tt.getRequestObjectFlat().optBoolean("b"));
        Assert.assertEquals(12.0, tt.getRequestObjectFlat().optDouble("c.a"), 0);
        Assert.assertEquals("\"marko\"", tt.getRequestObjectFlat().optString("c.b"));
        Assert.assertEquals("peter", tt.getRequestObjectFlat().optString("c.c"));
    }

    protected class MockResource extends BaseResource {
        public MockResource() {
            super(new MockRexsterApplicationProvider());
        }

        public MockResource(Map map) {
            super(new MockRexsterApplicationProvider());
            this.httpServletRequest = new MockHttpServletRequest(map);
        }

        public void setRequestObject(JSONObject queryParameters) {
            super.setRequestObject(queryParameters);
        }
    }

    protected class MockHttpServletRequest implements HttpServletRequest {

        private Map map = new HashMap();

        public MockHttpServletRequest(Map map) {
            this.map = map;
        }

        public Object getAttribute(String arg0) {
            // TODO Auto-generated method stub
            return null;
        }

        public Enumeration getAttributeNames() {
            // TODO Auto-generated method stub
            return null;
        }

        public String getCharacterEncoding() {
            // TODO Auto-generated method stub
            return null;
        }

        public int getContentLength() {
            // TODO Auto-generated method stub
            return 0;
        }

        public String getContentType() {
            // TODO Auto-generated method stub
            return null;
        }

        public ServletInputStream getInputStream() throws IOException {
            // TODO Auto-generated method stub
            return null;
        }

        public String getLocalAddr() {
            // TODO Auto-generated method stub
            return null;
        }

        public String getLocalName() {
            // TODO Auto-generated method stub
            return null;
        }

        public int getLocalPort() {
            // TODO Auto-generated method stub
            return 0;
        }

        public ServletContext getServletContext() {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public AsyncContext startAsync() throws IllegalStateException {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse) throws IllegalStateException {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public boolean isAsyncStarted() {
            return false;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public boolean isAsyncSupported() {
            return false;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public AsyncContext getAsyncContext() {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public DispatcherType getDispatcherType() {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public Locale getLocale() {
            // TODO Auto-generated method stub
            return null;
        }

        public Enumeration getLocales() {
            // TODO Auto-generated method stub
            return null;
        }

        public String getParameter(String arg0) {
            // TODO Auto-generated method stub
            return null;
        }

        public Map getParameterMap() {
            return map;
        }

        public Enumeration getParameterNames() {
            // TODO Auto-generated method stub
            return null;
        }

        public String[] getParameterValues(String arg0) {
            // TODO Auto-generated method stub
            return null;
        }

        public String getProtocol() {
            // TODO Auto-generated method stub
            return null;
        }

        public BufferedReader getReader() throws IOException {
            // TODO Auto-generated method stub
            return null;
        }

        public String getRealPath(String arg0) {
            // TODO Auto-generated method stub
            return null;
        }

        public String getRemoteAddr() {
            // TODO Auto-generated method stub
            return null;
        }

        public String getRemoteHost() {
            // TODO Auto-generated method stub
            return null;
        }

        public int getRemotePort() {
            // TODO Auto-generated method stub
            return 0;
        }

        public RequestDispatcher getRequestDispatcher(String arg0) {
            // TODO Auto-generated method stub
            return null;
        }

        public String getScheme() {
            // TODO Auto-generated method stub
            return null;
        }

        public String getServerName() {
            // TODO Auto-generated method stub
            return null;
        }

        public int getServerPort() {
            // TODO Auto-generated method stub
            return 0;
        }

        public boolean isSecure() {
            // TODO Auto-generated method stub
            return false;
        }

        public void removeAttribute(String arg0) {
            // TODO Auto-generated method stub

        }

        public void setAttribute(String arg0, Object arg1) {
            // TODO Auto-generated method stub

        }

        public void setCharacterEncoding(String arg0)
                throws UnsupportedEncodingException {
            // TODO Auto-generated method stub

        }

        public String getAuthType() {
            // TODO Auto-generated method stub
            return null;
        }

        public String getContextPath() {
            // TODO Auto-generated method stub
            return null;
        }

        public Cookie[] getCookies() {
            // TODO Auto-generated method stub
            return null;
        }

        public long getDateHeader(String arg0) {
            // TODO Auto-generated method stub
            return 0;
        }

        public String getHeader(String arg0) {
            // TODO Auto-generated method stub
            return null;
        }

        public Enumeration getHeaderNames() {
            // TODO Auto-generated method stub
            return null;
        }

        public Enumeration getHeaders(String arg0) {
            // TODO Auto-generated method stub
            return null;
        }

        public int getIntHeader(String arg0) {
            // TODO Auto-generated method stub
            return 0;
        }

        public String getMethod() {
            // TODO Auto-generated method stub
            return null;
        }

        public String getPathInfo() {
            // TODO Auto-generated method stub
            return null;
        }

        public String getPathTranslated() {
            // TODO Auto-generated method stub
            return null;
        }

        public String getQueryString() {
            // TODO Auto-generated method stub
            return null;
        }

        public String getRemoteUser() {
            // TODO Auto-generated method stub
            return null;
        }

        public String getRequestURI() {
            // TODO Auto-generated method stub
            return null;
        }

        public StringBuffer getRequestURL() {
            // TODO Auto-generated method stub
            return null;
        }

        public String getRequestedSessionId() {
            // TODO Auto-generated method stub
            return null;
        }

        public String getServletPath() {
            // TODO Auto-generated method stub
            return null;
        }

        public HttpSession getSession() {
            // TODO Auto-generated method stub
            return null;
        }

        public HttpSession getSession(boolean arg0) {
            // TODO Auto-generated method stub
            return null;
        }

        public Principal getUserPrincipal() {
            // TODO Auto-generated method stub
            return null;
        }

        public boolean isRequestedSessionIdFromCookie() {
            // TODO Auto-generated method stub
            return false;
        }

        public boolean isRequestedSessionIdFromURL() {
            // TODO Auto-generated method stub
            return false;
        }

        public boolean isRequestedSessionIdFromUrl() {
            // TODO Auto-generated method stub
            return false;
        }

        public boolean authenticate(HttpServletResponse httpServletResponse) throws IOException, ServletException {
            return false;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public void login(String s, String s1) throws ServletException {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public void logout() throws ServletException {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public Collection<Part> getParts() throws IOException, ServletException {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public Part getPart(String s) throws IOException, ServletException {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public boolean isRequestedSessionIdValid() {
            // TODO Auto-generated method stub
            return false;
        }

        public boolean isUserInRole(String arg0) {
            // TODO Auto-generated method stub
            return false;
        }

    }

    protected class MockRexsterApplicationProvider implements RexsterApplicationProvider {

        private final long startTime = System.currentTimeMillis();

        public RexsterApplication getRexsterApplication() {
            // TODO Auto-generated method stub
            return null;
        }

        public RexsterApplicationGraph getApplicationGraph(String graphName) {
            // TODO Auto-generated method stub
            return null;
        }

        public Set<String> getGraphsNames() {
            // TODO Auto-generated method stub
            return null;
        }

        public long getStartTime() {
            return this.startTime;
        }

    }
}

