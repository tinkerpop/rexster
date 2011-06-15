package com.tinkerpop.rexster;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseFilter;

import javax.ws.rs.core.MediaType;
import java.util.Collections;

/**
 * Adds headers to the response.
 */
public class HeaderResponseFilter implements ContainerResponseFilter {

    public static final String HEADER_ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String CHARSET = "charset";

    public ContainerResponse filter(ContainerRequest request, ContainerResponse response) {

        MediaType contentTypeSpecifiedByService = response.getMediaType();

        // only add the charset if it is not explicitly specified by the service itself.  this allows
        // an individual services to specify this value.
        if (contentTypeSpecifiedByService != null && !contentTypeSpecifiedByService.getParameters().containsKey(CHARSET)) {
            MediaType mediaTypeWithCharset = new MediaType(contentTypeSpecifiedByService.getType(),
                    contentTypeSpecifiedByService.getSubtype(),
                    Collections.singletonMap(CHARSET, WebServer.getCharacterEncoding()));
            response.getHttpHeaders().putSingle(CONTENT_TYPE, mediaTypeWithCharset);
        }

        contentTypeSpecifiedByService = response.getMediaType();

        response.getHttpHeaders().putSingle(HEADER_ACCESS_CONTROL_ALLOW_ORIGIN, "*");

        return response;
    }
}
