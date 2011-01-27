package com.tinkerpop.rexster;

import com.sun.jersey.core.util.ReaderWriter;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URL;

/**
 * Serves all content requested on "/main" context.
 * <p/>
 * Simply pushes main.html on all paths requested from the "/main" context. There might
 * be an easier and more direct way to do this.
 */
public class ToolServlet extends HttpServlet {

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, java.io.IOException {

        String rootPath = this.getInitParameter("root");
        ServletContext ctx = this.getServletContext();

        // set the MIME type of the response, "text/html"
        response.setContentType("text/html");

        URL resource = ctx.getResource(rootPath + "/main.html");
        ReaderWriter.writeTo(resource.openStream(), response.getOutputStream());
    }
}
