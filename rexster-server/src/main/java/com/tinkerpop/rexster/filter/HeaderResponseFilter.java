package com.tinkerpop.rexster.filter;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import com.tinkerpop.rexster.Application;
import com.tinkerpop.rexster.Tokens;
import org.codehaus.jettison.json.JSONObject;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Adds headers to the response.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class HeaderResponseFilter implements ContainerResponseFilter {

    public static final String HEADER_ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String CHARSET = "charset";

    private final String defaultCharacterEncoding;

    public HeaderResponseFilter(final String defaultCharacterEncoding) {
        this.defaultCharacterEncoding = defaultCharacterEncoding;
    }

    public ContainerResponse filter(ContainerRequest request, ContainerResponse response) {

        String acceptCharsetHeaderValue = request.getHeaderValue("Accept-Charset");
        if (acceptCharsetHeaderValue == null || acceptCharsetHeaderValue.isEmpty()) {
            // assign the default charset since none is specified.
            acceptCharsetHeaderValue = this.defaultCharacterEncoding;
        }

        final CharsetHolder firstSupportedCharset = CharsetHolder.getFirstSupportedCharset(acceptCharsetHeaderValue);
        if (firstSupportedCharset != null) {

            final MediaType contentTypeSpecifiedByService = response.getMediaType();

            // only add the charset if it is not explicitly specified by the service itself.  this allows
            // an individual services to specify this value.
            if (contentTypeSpecifiedByService != null && !contentTypeSpecifiedByService.getParameters().containsKey(CHARSET)) {
                final MediaType mediaTypeWithCharset = new MediaType(contentTypeSpecifiedByService.getType(),
                        contentTypeSpecifiedByService.getSubtype(),
                        Collections.singletonMap(CHARSET, firstSupportedCharset.getCharset()));
                response.getHttpHeaders().putSingle(CONTENT_TYPE, mediaTypeWithCharset);
            }
        } else {
            response.setStatus(Response.Status.NOT_ACCEPTABLE.getStatusCode());

            final Map<String, String> m = new HashMap<String, String>();
            m.put(Tokens.MESSAGE, "[" + firstSupportedCharset.getCharset() + "] is not a valid character set or is not supported by Rexster.  Check the Accept-Charset of the request and the <character-set> setting in rexster.xml");

            final JSONObject jsonError = new JSONObject(m);
            response.setEntity(jsonError);
        }

        response.getHttpHeaders().putSingle(HEADER_ACCESS_CONTROL_ALLOW_ORIGIN, "*");

        return response;
    }
}
