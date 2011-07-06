package com.tinkerpop.rexster;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;

/**
 * Adds headers to the response.
 */
public class HeaderResponseFilter implements ContainerResponseFilter {

    private static Logger logger = Logger.getLogger(HeaderResponseFilter.class);

    public static final String HEADER_ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String CHARSET = "charset";

    public ContainerResponse filter(ContainerRequest request, ContainerResponse response) {

        String acceptCharsetHeaderValue = request.getHeaderValue("Accept-Charset");
        if (acceptCharsetHeaderValue == null || acceptCharsetHeaderValue.isEmpty()) {
            // assign the default charset since none is specified.
            acceptCharsetHeaderValue = WebServer.getCharacterEncoding();
        }

        List<CharsetHolder> charsetRanks = CharsetHolder.getAcceptableCharsets(acceptCharsetHeaderValue);

        CharsetHolder firstSupportedCharset = null;
        for (CharsetHolder charsetRank : charsetRanks) {
            if (charsetRank.isSupported()) {
                firstSupportedCharset = charsetRank;
                break;
            }
        }

        if (firstSupportedCharset != null) {

            MediaType contentTypeSpecifiedByService = response.getMediaType();

            // only add the charset if it is not explicitly specified by the service itself.  this allows
            // an individual services to specify this value.
            if (contentTypeSpecifiedByService != null && !contentTypeSpecifiedByService.getParameters().containsKey(CHARSET)) {
                MediaType mediaTypeWithCharset = new MediaType(contentTypeSpecifiedByService.getType(),
                        contentTypeSpecifiedByService.getSubtype(),
                        Collections.singletonMap(CHARSET, firstSupportedCharset.getCharset()));
                response.getHttpHeaders().putSingle(CONTENT_TYPE, mediaTypeWithCharset);
            }

            contentTypeSpecifiedByService = response.getMediaType();

            response.getHttpHeaders().putSingle(HEADER_ACCESS_CONTROL_ALLOW_ORIGIN, "*");
        } else {
            response.setStatus(Response.Status.NOT_ACCEPTABLE.getStatusCode());

            Map<String, String> m = new HashMap<String, String>();
            m.put(Tokens.MESSAGE, "[" + firstSupportedCharset.getCharset() + "] is not a valid character set or is not supported by Rexster.  Check the Accept-Charset of the request and the <character-set> setting in rexster.xml");

            JSONObject jsonError = new JSONObject(m);
            response.setEntity(jsonError);
        }

        return response;
    }
}
