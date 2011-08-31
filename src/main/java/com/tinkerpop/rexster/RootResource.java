package com.tinkerpop.rexster;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/")
public class RootResource extends BaseResource {
    public RootResource() {
        super(null);
    }

    public RootResource(RexsterApplicationProvider rap) {
        super(rap);
    }

    @GET
    @Produces({MediaType.TEXT_HTML})
    public Response evaluate() {
        StringBuffer sb = new StringBuffer();
        sb.append("<html style=\"background-color:#111111\">");
        sb.append("<head><meta charset=\"UTF-8\"><title>Rexster</title><link rel=\"shortcut icon\" type=\"image/x-icon\" href=\"/doghouse/favicon.ico\"></head>");
        sb.append("<body>");
        sb.append("<div align=\"center\"><img src=\"/doghouse/img/rexster-logo.png\"/></div>");
        sb.append("<div align=\"center\">");
        sb.append("<h3 style=\"color:#B5B5B5\">Rexster - " + RexsterApplication.getVersion() + "</h3>");
        sb.append("<p><a style=\"color:#B5B5B5\" href=\"/doghouse\">The Dog House</a></p>");
        sb.append("<p><a style=\"color:#B5B5B5\" href=\"/graphs\">REST API<a></p>");
        sb.append("</div>");
        sb.append("</body>");
        sb.append("</html>");

        return Response.ok(sb.toString()).build();
    }
}
