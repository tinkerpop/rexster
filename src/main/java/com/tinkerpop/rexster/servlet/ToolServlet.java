package com.tinkerpop.rexster.servlet;

import com.sun.jersey.core.util.ReaderWriter;
import com.tinkerpop.gremlin.GremlinTokens;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;

import java.io.InputStreamReader;
import java.net.URL;

/**
 * Serves all content requested on "/main" context.
 * <p/>
 * Simply pushes main.html on all paths requested from the "/main" context. There might
 * be an easier and more direct way to do this.
 */
public class ToolServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, java.io.IOException {

        String rootPath = this.getInitParameter("root");
        ServletContext ctx = this.getServletContext();

        // set the MIME type of the response, "text/html"
        response.setContentType("text/html");

        URL resource = ctx.getResource(rootPath + "/main.html");
        
        // kind of opens a bad door here.  will probably rethink this a bit.  
        String content = ReaderWriter.readFromAsString(new InputStreamReader(resource.openStream()));
        content = content.replace("{{inject}}", "<script type=\"text/javascript\">var GREMLIN_VERSION = \"" + GremlinTokens.VERSION + "\";</script>");
        response.getWriter().write(content);
    }
}
