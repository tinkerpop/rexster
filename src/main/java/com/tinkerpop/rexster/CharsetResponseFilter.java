package com.tinkerpop.rexster;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseFilter;

import javax.ws.rs.core.MediaType;

/**
 * Adds the character set from rexster.xml to the Content-Type header.
 */
public class CharsetResponseFilter implements ContainerResponseFilter {

    public ContainerResponse filter(ContainerRequest request, ContainerResponse response) {

        // sets the content type with the configured character encoding.
        MediaType contentType = response.getMediaType();
        if (contentType != null && !contentType.toString().contains("charset=")) {
            String contentTypeString = contentType.toString() + ";charset=" + WebServer.getCharacterEncoding();
            response.getHttpHeaders().putSingle("Content-Type", contentTypeString);
        }

        return response;
    }
}
