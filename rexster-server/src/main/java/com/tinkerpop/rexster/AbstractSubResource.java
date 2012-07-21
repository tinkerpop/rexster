package com.tinkerpop.rexster;

import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.rexster.extension.ExtensionDefinition;
import com.tinkerpop.rexster.extension.ExtensionDescriptor;
import com.tinkerpop.rexster.extension.ExtensionMethod;
import com.tinkerpop.rexster.extension.ExtensionNaming;
import com.tinkerpop.rexster.extension.ExtensionPoint;
import com.tinkerpop.rexster.extension.ExtensionRequestParameter;
import com.tinkerpop.rexster.extension.ExtensionResponse;
import com.tinkerpop.rexster.extension.ExtensionSegmentSet;
import com.tinkerpop.rexster.extension.HttpMethod;
import com.tinkerpop.rexster.extension.RexsterContext;
import com.tinkerpop.rexster.extension.RexsterExtension;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * Base "sub" resource class which contains helper methods for getting extensions, the RexsterApplicationGraph,
 * and request object.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public abstract class AbstractSubResource extends BaseResource {

    private static final Logger logger = Logger.getLogger(AbstractSubResource.class);

    protected static final Map<ExtensionSegmentSet, List<RexsterExtension>> extensionCache = new HashMap<ExtensionSegmentSet, List<RexsterExtension>>();

    protected AbstractSubResource(RexsterApplication ra) {
        super(ra);

        try {

            this.resultObject.put(Tokens.VERSION, RexsterApplicationImpl.getVersion());

        } catch (JSONException ex) {

            logger.error(ex);

            final JSONObject error = generateErrorObjectJsonFail(ex);
            throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR).entity(error).build());
        }
    }

    public RexsterApplicationGraph getRexsterApplicationGraph(final String graphName) {
        final RexsterApplicationGraph rag = this.getRexsterApplication().getApplicationGraph(graphName);
        if (rag == null) {

            if (!graphName.equals("favicon.ico")) {
                logger.info("Request for a non-configured graph [" + graphName + "]");
            }

            final JSONObject error = generateErrorObject("Graph [" + graphName + "] could not be found");
            throw new WebApplicationException(Response.status(Status.NOT_FOUND).entity(error).build());
        }

        return rag;
    }

    /**
     * Tries to find an extension given the specified namespace and extension name. Extensions
     * are loaded using ServiceLoader so ensure that the RexsterExtension file in META-INF.services
     * has all required extension implementations.
     * <p/>
     * This method tries to look for an ExtensionNaming annotation on the RexsterExtension
     * implementation and uses that for the namespace and extension name.  If for some reason that
     * annotation cannot be found or if the annotation is only partially defined defaults are used.
     * The default for the names is the reserved "g" namespace which is "global".  The default for
     * the extension name is the name of the class.
     *
     * @return The found extension instance or null if one cannot be found.
     */
    protected static List<RexsterExtension> findExtensionClasses(final ExtensionSegmentSet extensionSegmentSet) {

        List<RexsterExtension> extensionsForSegmentSet = extensionCache.get(extensionSegmentSet);
        if (extensionsForSegmentSet == null) {
            final ServiceLoader<? extends RexsterExtension> extensions = ServiceLoader.load(RexsterExtension.class);
            extensionsForSegmentSet = new ArrayList<RexsterExtension>();
            for (RexsterExtension extension : extensions) {
                final Class clazz = extension.getClass();
                final ExtensionNaming extensionNaming = (ExtensionNaming) clazz.getAnnotation(ExtensionNaming.class);

                // initialize the defaults
                String currentExtensionNamespace = "g";
                String currentExtensionName = clazz.getName();

                if (extensionNaming != null) {

                    // naming annotation is present to try to override the defaults
                    // if the values are valid.
                    if (extensionNaming.name() != null && !extensionNaming.name().isEmpty()) {
                        currentExtensionName = extensionNaming.name();
                    }

                    // naming annotation is defaulted to "g" anyway but checking anyway to make sure
                    // no one tries to pull any funny business.
                    if (extensionNaming.namespace() != null && !extensionNaming.namespace().isEmpty()) {
                        currentExtensionNamespace = extensionNaming.namespace();
                    }
                }

                if (extensionSegmentSet.getNamespace().equals(currentExtensionNamespace)
                        && extensionSegmentSet.getExtension().equals(currentExtensionName)) {
                    // found what we're looking for
                    extensionsForSegmentSet.add(extension);
                }
            }

            if (extensionsForSegmentSet.size() == 0) {
                extensionsForSegmentSet = null;
                extensionCache.put(extensionSegmentSet, null);
            } else {
                extensionCache.put(extensionSegmentSet, extensionsForSegmentSet);
            }
        }

        return extensionsForSegmentSet;
    }

    /**
     * Reads the URI of the request and tries to parse the extension requested.
     *
     * @throws WebApplicationException If the segment is an invalid format.
     */
    protected ExtensionSegmentSet parseUriForExtensionSegment(final String graphName, final ExtensionPoint extensionPoint) {
        final ExtensionSegmentSet extensionSegmentSet = new ExtensionSegmentSet(this.uriInfo, extensionPoint);

        if (!extensionSegmentSet.isValidFormat()) {
            logger.error("Tried to parse the extension segments but they appear invalid: " + extensionSegmentSet);
            final JSONObject error = this.generateErrorObject(
                    "The [" + extensionSegmentSet + "] extension appears invalid for [" + graphName + "]");
            throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR).entity(error).build());
        }

        return extensionSegmentSet;
    }

    /**
     * Find the method on the RexsterExtension implementation to call given the ExtensionPoint and the
     * extension action to be executed.
     * <p/>
     * This method will find the first matching extension method.  If multiple extension method matches then
     * the remainder will be ignored.
     * <p/>
     * The logic of this method takes the following approach: match a method on the extension point and extension
     * method.  Then match the method that has an action and matches an definition path or has no action and
     * and has no definition path.  If no match is found there then the methods are cycled again to find a
     * match where there is an action and no definition path.
     *
     * @param rexsterExtensions   The extension instance to be called.
     * @param extensionPoint      One of the extension points (graph, edge, vertex).
     * @param extensionAction     This value may be null or empty if the RexsterExtension is being exposed as a
     *                            root level call (ie. the ExtensionDefinition annotation does not specify a
     *                            path, just an ExtensionPoint).
     * @param httpMethodRequested The HTTP method made on the request.
     * @return The method to call or null if it cannot be found.
     */
    protected static ExtensionMethod findExtensionMethod(List<RexsterExtension> rexsterExtensions,
                                                         ExtensionPoint extensionPoint,
                                                         String extensionAction, HttpMethod httpMethodRequested) {
        ExtensionMethod methodToCall = null;
        for (RexsterExtension rexsterExtension : rexsterExtensions) {
            Class rexsterExtensionClass = rexsterExtension.getClass();
            Method[] methods = rexsterExtensionClass.getMethods();

            for (Method method : methods) {
                // looks for the first method that matches.  methods that multi-match will be ignored right now
                ExtensionDefinition extensionDefinition = method.getAnnotation(ExtensionDefinition.class);
                ExtensionDescriptor extensionDescriptor = method.getAnnotation(ExtensionDescriptor.class);

                // checks if the extension point is graph, and if the method path matches the specified action on
                // the uri (if it exists) or if the method has no path.
                if (extensionDefinition != null && extensionDefinition.extensionPoint() == extensionPoint
                        && (extensionDefinition.method() == HttpMethod.ANY || extensionDefinition.method() == httpMethodRequested)) {

                    if ((!extensionAction.isEmpty() && extensionDefinition.path().equals(extensionAction))
                            || (extensionAction.isEmpty() && extensionDefinition.path().isEmpty())) {
                        methodToCall = new ExtensionMethod(method, extensionDefinition, extensionDescriptor, rexsterExtension);
                        break;
                    }
                }
            }

            if (methodToCall == null) {
                for (Method method : methods) {
                    ExtensionDefinition extensionDefinition = method.getAnnotation(ExtensionDefinition.class);
                    ExtensionDescriptor extensionDescriptor = method.getAnnotation(ExtensionDescriptor.class);

                    if (extensionDefinition != null && extensionDefinition.extensionPoint() == extensionPoint
                            && (extensionDefinition.method() == HttpMethod.ANY || extensionDefinition.method() == httpMethodRequested)) {

                        if (!extensionAction.isEmpty() && extensionDefinition.path().isEmpty()) {
                            methodToCall = new ExtensionMethod(method, extensionDefinition, extensionDescriptor, rexsterExtension);
                            break;
                        }
                    }
                }
            }
        }

        return methodToCall;
    }

    protected Object invokeExtension(final RexsterApplicationGraph rexsterApplicationGraph, final ExtensionMethod methodToCall)
            throws IllegalAccessException, InvocationTargetException {
        return this.invokeExtension(rexsterApplicationGraph, methodToCall, null, null);
    }

    protected Object invokeExtension(final RexsterApplicationGraph rexsterApplicationGraph, final ExtensionMethod methodToCall, final Vertex vertexContext)
            throws IllegalAccessException, InvocationTargetException {
        return this.invokeExtension(rexsterApplicationGraph, methodToCall, null, vertexContext);
    }

    protected Object invokeExtension(final RexsterApplicationGraph rexsterApplicationGraph, final ExtensionMethod methodToCall, final Edge edgeContext)
            throws IllegalAccessException, InvocationTargetException {
        return this.invokeExtension(rexsterApplicationGraph, methodToCall, edgeContext, null);
    }

    protected Object invokeExtension(final RexsterApplicationGraph rexsterApplicationGraph,
                                     final ExtensionMethod methodToCall, final Edge edgeContext,
                                     final Vertex vertexContext)
            throws IllegalAccessException, InvocationTargetException {

        final RexsterExtension rexsterExtension = methodToCall.getRexsterExtension();
        final Method method = methodToCall.getMethod();

        final RexsterResourceContext rexsterResourceContext = new RexsterResourceContext(
                rexsterApplicationGraph,
                this.uriInfo,
                this.httpServletRequest,
                this.getRequestObject(),
                this.getRequestObjectFlat(),
                methodToCall,
                this.securityContext);

        final Annotation[][] parametersAnnotations = method.getParameterAnnotations();
        final ArrayList methodToCallParams = new ArrayList();
        for (int ix = 0; ix < parametersAnnotations.length; ix++) {
            final Annotation[] annotation = parametersAnnotations[ix];
            final Class[] parameterTypes = method.getParameterTypes();

            if (annotation != null) {
                if (annotation[0] instanceof RexsterContext) {
                    if (parameterTypes[ix].equals(Graph.class)) {
                        methodToCallParams.add(rexsterApplicationGraph.getGraph());
                    } else if (parameterTypes[ix].equals(RexsterApplicationGraph.class)) {
                        methodToCallParams.add(rexsterApplicationGraph);
                    } else if (parameterTypes[ix].equals(ExtensionMethod.class)) {
                        methodToCallParams.add(methodToCall);
                    } else if (parameterTypes[ix].equals(UriInfo.class)) {
                        methodToCallParams.add(this.uriInfo);
                    } else if (parameterTypes[ix].equals(HttpServletRequest.class)) {
                        methodToCallParams.add(this.httpServletRequest);
                    } else if (parameterTypes[ix].equals(SecurityContext.class)) {
                        methodToCallParams.add(this.securityContext);
                    } else if (parameterTypes[ix].equals(RexsterResourceContext.class)) {
                        methodToCallParams.add(rexsterResourceContext);
                    } else if (parameterTypes[ix].equals(Edge.class)) {
                        methodToCallParams.add(edgeContext);
                    } else if (parameterTypes[ix].equals(Vertex.class)) {
                        methodToCallParams.add(vertexContext);
                    } else {
                        // don't know what it is so just push a null
                        methodToCallParams.add(null);
                    }
                } else if (annotation[0] instanceof ExtensionRequestParameter) {
                    final ExtensionRequestParameter extensionRequestParameter = (ExtensionRequestParameter) annotation[0];
                    if (parameterTypes[ix].equals(String.class)) {
                        if (extensionRequestParameter.parseToJson()) {
                            methodToCallParams.add(this.getRequestObject().optString(extensionRequestParameter.name()));
                        } else {
                            methodToCallParams.add(this.getRequestObjectFlat().optString(extensionRequestParameter.name()));
                        }
                    } else if (parameterTypes[ix].equals(Integer.class)) {
                        if (this.getRequestObject().has(extensionRequestParameter.name())) {
                            if (extensionRequestParameter.parseToJson()) {
                                int intValue = this.getRequestObject().optInt(extensionRequestParameter.name());
                                methodToCallParams.add(new Integer(intValue));
                            } else {
                                int intValue = this.getRequestObjectFlat().optInt(extensionRequestParameter.name());
                                methodToCallParams.add(new Integer(intValue));
                            }
                        } else {
                            methodToCallParams.add(null);
                        }
                    } else if (parameterTypes[ix].equals(Float.class)) {
                        if (this.getRequestObject().has(extensionRequestParameter.name())) {
                            if (extensionRequestParameter.parseToJson()) {
                                float floatValue = (float) this.getRequestObject().optDouble(extensionRequestParameter.name());
                                methodToCallParams.add(new Float(floatValue));
                            } else {
                                float floatValue = (float) this.getRequestObjectFlat().optDouble(extensionRequestParameter.name());
                                methodToCallParams.add(new Float(floatValue));
                            }
                        } else {
                            methodToCallParams.add(null);
                        }
                    } else if (parameterTypes[ix].equals(Double.class)) {
                        if (this.getRequestObject().has(extensionRequestParameter.name())) {
                            if (extensionRequestParameter.parseToJson()) {
                                double doubleValue = this.getRequestObject().optDouble(extensionRequestParameter.name());
                                methodToCallParams.add(new Double(doubleValue));
                            } else {
                                double doubleValue = this.getRequestObjectFlat().optDouble(extensionRequestParameter.name());
                                methodToCallParams.add(new Double(doubleValue));
                            }
                        } else {
                            methodToCallParams.add(null);
                        }
                    } else if (parameterTypes[ix].equals(Long.class)) {
                        if (this.getRequestObject().has(extensionRequestParameter.name())) {
                            if (extensionRequestParameter.parseToJson()) {
                                long longValue = this.getRequestObject().optLong(extensionRequestParameter.name());
                                methodToCallParams.add(new Long(longValue));
                            } else {
                                long longValue = this.getRequestObjectFlat().optLong(extensionRequestParameter.name());
                                methodToCallParams.add(new Long(longValue));
                            }
                        } else {
                            methodToCallParams.add(null);
                        }
                    } else if (parameterTypes[ix].equals(Boolean.class)) {
                        if (this.getRequestObject().has(extensionRequestParameter.name())) {
                            if (extensionRequestParameter.parseToJson()) {
                                boolean booleanValue = this.getRequestObject().optBoolean(extensionRequestParameter.name());
                                methodToCallParams.add(new Boolean(booleanValue));
                            } else {
                                boolean booleanValue = this.getRequestObjectFlat().optBoolean(extensionRequestParameter.name());
                                methodToCallParams.add(new Boolean(booleanValue));
                            }
                        } else {
                            methodToCallParams.add(null);
                        }
                    } else if (parameterTypes[ix].equals(JSONObject.class)) {
                        if (extensionRequestParameter.parseToJson()) {
                            methodToCallParams.add(this.getRequestObject().optJSONObject(extensionRequestParameter.name()));
                        } else {
                            methodToCallParams.add(this.getRequestObjectFlat().optJSONObject(extensionRequestParameter.name()));
                        }
                    } else if (parameterTypes[ix].equals(JSONArray.class)) {
                        if (extensionRequestParameter.parseToJson()) {
                            methodToCallParams.add(this.getRequestObject().optJSONArray(extensionRequestParameter.name()));
                        } else {
                            methodToCallParams.add(this.getRequestObjectFlat().optJSONArray(extensionRequestParameter.name()));
                        }
                    } else {
                        // don't know what it is so just push a null
                        methodToCallParams.add(null);
                    }
                } else {
                    // the parameter was not marked with an annotation rexster cares about.
                    methodToCallParams.add(null);
                }
            } else {
                // the parameter was not marked with any annotation
                methodToCallParams.add(null);
            }
        }

        return method.invoke(rexsterExtension, methodToCallParams.toArray());
    }

    protected ExtensionResponse tryAppendRexsterAttributesIfJson(final ExtensionResponse extResponse,
                                                                 final ExtensionMethod methodToCall,
                                                                 final String mediaType) {
        ExtensionResponse newExtensionResponse = extResponse;
        if (mediaType.equals(MediaType.APPLICATION_JSON)
                && methodToCall.getExtensionDefinition().tryIncludeRexsterAttributes()) {

            final Object obj = extResponse.getJerseyResponse().getEntity();
            if (obj instanceof JSONObject) {
                final JSONObject entity = (JSONObject) obj;

                if (entity != null) {
                    try {
                        entity.put(Tokens.VERSION, RexsterApplicationImpl.getVersion());
                        entity.put(Tokens.QUERY_TIME, this.sh.stopWatch());
                    } catch (JSONException jsonException) {
                        // nothing bad happening here
                        logger.error("Couldn't add Rexster attributes to response for an extension.");
                    }

                    newExtensionResponse = new ExtensionResponse(
                            Response.fromResponse(extResponse.getJerseyResponse()).entity(entity).build());

                }
            }
        }

        return newExtensionResponse;
    }
}
