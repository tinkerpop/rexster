package com.tinkerpop.rexster.servlet;

import com.tinkerpop.rexster.RexsterApplicationProvider;
import com.tinkerpop.rexster.WebServerRexsterApplicationProvider;
import com.tinkerpop.rexster.gremlin.GremlinEvaluationJob;
import com.tinkerpop.rexster.gremlin.GremlinSessions;
import com.tinkerpop.rexster.gremlin.converter.ConsoleResultConverter;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * Visualization servlet migrated from Webling (https://github.com/xedin/webling) and modified.
 * <p/>
 * Credit to Neo Technology (http://neotechnology.com/) for most of the code related to the
 * Gremlin in Rexster.  Specifically, this code was borrowed from
 * https://github.com/neo4j/webadmin and re-purposed for Rexster's needs.
 * <p/>
 * Original author Pavel A. Yaskevich
 */
public class VisualizationServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        ServletContext sc = getServletContext();
        String sessionId = request.getSession(true).getId();
        String code = "g:vis(" + request.getParameter("v") + ")";
        String graphName = request.getParameter("g");

        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_OK);
        sc.log("[GET /visualize?v=" + request.getParameter("v") + "] 200 OK");

        try {
            RexsterApplicationProvider rap = new WebServerRexsterApplicationProvider(this.getServletContext());

            GremlinEvaluationJob job = GremlinSessions.getSession(sessionId, graphName, rap).evaluate(code);
            List<String> result = new ConsoleResultConverter(job.getOutputWriter()).convert(job.getResult());
            response.getWriter().println(((result.size() == 1) ? result.get(0) : result));
        } catch (Exception e) {
            response.getWriter().println(e.getMessage());
        }
    }
}
