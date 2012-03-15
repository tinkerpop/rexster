package com.tinkerpop.rexster.servlet;

import com.tinkerpop.rexster.RexsterApplication;
import com.tinkerpop.rexster.gremlin.GremlinEvaluationJob;
import com.tinkerpop.rexster.gremlin.GremlinSessions;
import com.tinkerpop.rexster.gremlin.converter.ConsoleResultConverter;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

/**
 * Evaluator servlet migrated from Webling (https://github.com/xedin/webling) and modified.
 * <p/>
 * Credit to Neo Technology (http://neotechnology.com/) for most of the code related to the
 * Gremlin Terminal in Rexster.  Specifically, this code was borrowed from
 * https://github.com/neo4j/webadmin and re-purposed for Rexster's needs.
 * <p/>
 * Original author Pavel A. Yaskevich
 */
public class EvaluatorServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final String newLineRegex = "(\r\n|\r|\n|\n\r)";

    private final RexsterApplication rexsterApplication;

    public EvaluatorServlet(RexsterApplication rexsterApplication) {
        this.rexsterApplication = rexsterApplication;
    }

    @SuppressWarnings("unchecked")
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        ServletContext sc = getServletContext();
        String code = request.getParameter("code");
        String logMessage = "[POST /exec?code=" + code.replaceAll(newLineRegex, " ") + "] ";
        String graphName = request.getParameter("g");

        if (code.isEmpty()) {
            sc.log(logMessage + "400 ERROR");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        String sessionId = request.getSession(true).getId();

        response.setContentType("text/plain;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_OK);

        // log request
        sc.log(logMessage + "200 OK");

        // redirecting standard output to our custom printStream
        // to be able to show user result of g:print() function
        PrintStream out = new PrintStream(response.getOutputStream());
        System.setOut(out);

        try {
            GremlinEvaluationJob job = GremlinSessions.getSession(sessionId, graphName,
                    rexsterApplication).evaluate(code);
            List<String> lines = new ConsoleResultConverter(job.getOutputWriter()).convert(job.getResult());
            for (String line : lines) {
                out.println("==>" + line);
            }
        } catch (Exception e) {
            out.println(e.getMessage());
        }

        out.close();
    }

}
