package com.tinkerpop.rexster.servlet;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.tinkerpop.rexster.servlet.gremlin.ConsoleSessions;
import com.tinkerpop.rexster.servlet.gremlin.ConsoleSession;
	
/**
 * Visualization servlet migrated from Webling (https://github.com/xedin/webling) and modified.
 * 
 * Credit to Neo Technology (http://neotechnology.com/) for most of the code related to the 
 * Gremlin Terminal in Rexster.  Specifically, this code was borrowed from 
 * https://github.com/neo4j/webadmin and re-purposed for Rexster's needs.
 * 
 * Original author Pavel A. Yaskevich
 */
public class VisualizationServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        ServletContext sc = getServletContext();
        String sessionId = request.getSession(true).getId();
        String code	= "g:vis(" + request.getParameter("v") + ")";
        String graphName = request.getParameter("g");
      
        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_OK);
        sc.log("[GET /visualize?v=" + request.getParameter("v") + "] 200 OK");

        try {
        	List<String> result = ConsoleSessions.getSession(sessionId, graphName).evaluate(code);
            response.getWriter().println(((result.size() == 1) ? result.get(0) : result));
        } catch(Exception e) {
            response.getWriter().println(e.getMessage());
        }
    }
}
