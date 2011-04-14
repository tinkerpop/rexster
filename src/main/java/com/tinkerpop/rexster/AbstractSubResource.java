package com.tinkerpop.rexster;

import com.tinkerpop.blueprints.pgm.Edge;
import com.tinkerpop.blueprints.pgm.Graph;
import com.tinkerpop.blueprints.pgm.Vertex;
import com.tinkerpop.rexster.extension.*;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

public abstract class AbstractSubResource extends BaseResource {

    private static final Logger logger = Logger.getLogger(AbstractSubResource.class);
    
    protected AbstractSubResource(RexsterApplicationProvider rap) {
        super(rap);
        
        /*
        this.rag = this.rexsterApplicationProvider.getApplicationGraph(graphName);
        if (this.rag == null) {

            logger.info("Request for a non-configured graph [" + graphName + "]");

            JSONObject error = generateErrorObject("Graph [" + graphName + "] could not be found");
            throw new WebApplicationException(Response.status(Status.NOT_FOUND).entity(error).build());
        }
         */
        
        try {
            
            this.resultObject.put(Tokens.VERSION, RexsterApplication.getVersion());

        } catch (JSONException ex) {

            logger.error(ex);

            JSONObject error = generateErrorObjectJsonFail(ex);
            throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR).entity(error).build());
        }
    }
    
    public RexsterApplicationGraph getRexsterApplicationGraph(String graphName) {
    	RexsterApplicationGraph rag = this.getRexsterApplicationProvider().getApplicationGraph(graphName);
        if (rag == null) {

            logger.info("Request for a non-configured graph [" + graphName + "]");

            JSONObject error = generateErrorObject("Graph [" + graphName + "] could not be found");
            throw new WebApplicationException(Response.status(Status.NOT_FOUND).entity(error).build());
        }
        
        return rag;
    }

    protected JSONArray getExtensionHypermedia(String graphName, ExtensionPoint extensionPoint) {

        RexsterApplicationGraph rag = this.getRexsterApplicationGraph(graphName);

        JSONArray hypermediaLinks = new JSONArray();

        ServiceLoader<? extends RexsterExtension> extensions = ServiceLoader.load(RexsterExtension.class);
        for (RexsterExtension extension : extensions) {

            Class clazz = extension.getClass();
            ExtensionNaming extensionNaming = (ExtensionNaming) clazz.getAnnotation(ExtensionNaming.class);

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

            // test the configuration to see if the extension should even be available
            ExtensionConfiguration extensionConfig = rag.findExtensionConfiguration(
                    currentExtensionNamespace, currentExtensionName);
            RexsterExtension rexsterExtension = null;
            try {
                rexsterExtension = (RexsterExtension) clazz.newInstance();
            } catch (Exception ex) {
                logger.warn("Failed extension configuration check for " + currentExtensionNamespace + ":"
                        + currentExtensionName + "on graph " + graphName);
            }

            if (rexsterExtension != null && rexsterExtension.isConfigurationValid(extensionConfig)) {
                Method[] methods = clazz.getMethods();
                for (Method method : methods) {
                    ExtensionDescriptor descriptor = method.getAnnotation(ExtensionDescriptor.class);
                    ExtensionDefinition definition = method.getAnnotation(ExtensionDefinition.class);

                    if (definition != null && definition.extensionPoint() == extensionPoint) {
                        String href = currentExtensionNamespace + "/" + currentExtensionName;
                        if (!definition.path().isEmpty()) {
                            href = href + "/" + definition.path();
                        }

                        HashMap hypermediaLink = new HashMap();
                        hypermediaLink.put("href", href);
                        hypermediaLink.put("title", descriptor.value());

                        hypermediaLinks.put(new JSONObject(hypermediaLink));
                    }
                }
            }
        }

        if (hypermediaLinks.length() == 0) {
            return null;
        } else {
            return hypermediaLinks;
        }
    }

    /**
     * Tries to find an extension given the specified namespace and extension name. Extensions
     * are loaded using ServiceLoader so ensure that the RexsterExtension file in META-INF.services
     * has all required extension implementations.
     *
     * This method tries to look for an ExtensionNaming annotation on the RexsterExtension
     * implementation and uses that for the namespace and extension name.  If for some reason that
     * annotation cannot be found or if the annotation is only partially defined defaults are used.
     * The default for the names is the reserved "g" namespace which is "global".  The default for
     * the extension name is the name of the class.
     *
     * @return The found extension instance or null if one cannot be found.
     */
    protected static RexsterExtension findExtension(ExtensionSegmentSet extensionSegmentSet) {
        ServiceLoader<? extends RexsterExtension> extensions = ServiceLoader.load(RexsterExtension.class);
        RexsterExtension rexsterExtension = null;
        for (RexsterExtension extension : extensions) {
            Class clazz = extension.getClass();
            ExtensionNaming extensionNaming = (ExtensionNaming) clazz.getAnnotation(ExtensionNaming.class);

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
                rexsterExtension = extension;
                break;
            }
        }

        return rexsterExtension;
    }

    /**
     * Reads the URI of the request and tries to parse the extension requested.
     *
     * @throws WebApplicationException If the segment is an invalid format.
     */
    protected ExtensionSegmentSet parseUriForExtensionSegment(String graphName, ExtensionPoint extensionPoint) {
        ExtensionSegmentSet extensionSegmentSet = new ExtensionSegmentSet(this.uriInfo, extensionPoint);

        if (!extensionSegmentSet.isValidFormat()) {
            logger.error("Tried to parse the extension segments but they appear invalid: " + extensionSegmentSet);
            JSONObject error = this.generateErrorObject(
                    "The [" + extensionSegmentSet + "] extension appears invalid for [" + graphName + "]");
            throw new WebApplicationException(this.addHeaders(Response.status(Status.INTERNAL_SERVER_ERROR).entity(error)).build());
        }

        return extensionSegmentSet;
    }

    /**
     * Find the method on the RexsterExtension implementation to call given the ExtensionPoint and the
     * extension action to be executed.
     *
     * This method will find the first matching extension method.  If multiple extension method matches then
     * the remainder will be ignored.
     *
     * @param rexsterExtension The extension instance to be called.
     * @param extensionPoint One of the extension points (graph, edge, vertex).
     * @param extensionAction This value may be null or empty if the RexsterExtension is being exposed as a
     *                       root level call (ie. the ExtensionDefinition annotation does not specify a
     *                       path, just an ExtensionPoint).
     * @return The method to call or null if it cannot be found.
     */
    protected static ExtensionMethod findExtensionMethod(RexsterExtension rexsterExtension, ExtensionPoint extensionPoint, String extensionAction) {
        Class rexsterExtensionClass = rexsterExtension.getClass();
        Method[] methods = rexsterExtensionClass.getMethods();

        ExtensionMethod methodToCall = null;
        for (Method method : methods) {
            // looks for the first method that matches.  methods that multi-match will be ignored right now
            // todo: we probably need to add some kind of up-front validation of extensions.
            ExtensionDefinition extensionDefinition = method.getAnnotation(ExtensionDefinition.class);

            // checks if the extension point is graph, and if the method path matches the specified action on
            // the uri (if it exists) or if the method has no path.
            if (extensionDefinition != null && extensionDefinition.extensionPoint() == extensionPoint) {

                if (extensionDefinition.path().isEmpty()) {
                    // try to use a root level method definition
                    methodToCall = new ExtensionMethod(method, extensionDefinition);
                    break;
                } else if ((!extensionAction.equals("") && extensionDefinition.path().equals(extensionAction))
                    || (extensionAction.equals("") && extensionDefinition.path().equals(""))) {
                    // the extension path is valid so try to match on the action
                    methodToCall = new ExtensionMethod(method, extensionDefinition);
                    break;
                }
            }
        }

        return methodToCall;
    }

    protected Object invokeExtension(String graphName, RexsterExtension rexsterExtension, Method methodToCall)
                throws IllegalAccessException, InvocationTargetException {
        return this.invokeExtension(graphName, rexsterExtension, methodToCall, null, null);
    }

    protected Object invokeExtension(String graphName, RexsterExtension rexsterExtension, Method methodToCall, Vertex vertexContext)
            throws IllegalAccessException, InvocationTargetException {
        return this.invokeExtension(graphName, rexsterExtension, methodToCall, null, vertexContext);
    }

    protected Object invokeExtension(String graphName, RexsterExtension rexsterExtension, Method methodToCall, Edge edgeContext)
            throws IllegalAccessException, InvocationTargetException {
        return this.invokeExtension(graphName, rexsterExtension, methodToCall, edgeContext, null);
    }

    protected Object invokeExtension(String graphName, RexsterExtension rexsterExtension, Method methodToCall, Edge edgeContext, Vertex vertexContext)
            throws IllegalAccessException, InvocationTargetException {
        RexsterApplicationGraph rag = this.getRexsterApplicationGraph(graphName);

        RexsterResourceContext rexsterResourceContext = new RexsterResourceContext(
                this.getRexsterApplicationGraph(graphName),
                this.uriInfo,
                this.httpServletRequest,
                this.getRequestObject(),
                this.getRexsterApplicationProvider().getResultObjectCache());

        Annotation[][] parametersAnnotations = methodToCall.getParameterAnnotations();
        ArrayList methodToCallParams = new ArrayList();
        for (int ix = 0; ix < parametersAnnotations.length; ix++) {
            Annotation[] annotation = parametersAnnotations[ix];
            Class[] parameterTypes = methodToCall.getParameterTypes();

            if (annotation[0] instanceof RexsterContext) {
                if (parameterTypes[ix].equals(Graph.class)) {
                    methodToCallParams.add(rag.getGraph());
                } else if (parameterTypes[ix].equals(RexsterApplicationGraph.class)) {
                    methodToCallParams.add(rag);
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
            } else {
                // the parameter was not marked with rexstercontext.
                methodToCallParams.add(null);
            }
        }

        return methodToCall.invoke(rexsterExtension, methodToCallParams.toArray());
    }

    /**
     * Takes a property value string from the URI and attempts to parse it
     * to its defined data type.
     * <p/>
     * The format of the property value must be enclosed with parens with
     * two values within it separated by a comma.  The first value is the data
     * type which may be one of the following:
     * <p/>
     * i or integer
     * f or float
     * l or long
     * d or double
     * s or string
     * map
     * list
     * <p/>
     * In the event that the type is not set or the parens cannot be tracked
     * as open and closed the parser will determine the value to simply be a string.
     * <p/>
     * The value of a list must be defined with enclosing square brackets with values
     * separated by commas.  Values are defined with the same rules involving parens
     * defined above.
     * <p/>
     * The value of a map must be defined with enclosing parens where comma
     * separated segments of new key-value pairs represent the properties of the
     * map itself.
     * <p/>
     * Values may be nested to any depth.
     *
     * @param propertyValue The value from a key-value pair.  At a top level, this will
     *                      come from the URI as a one of the query parameters.
     * @return The property value coerced to the appropriate Java data type.
     */
    protected Object getTypedPropertyValue(String propertyValue) {
        Object typedPropertyValue = propertyValue;
        if (typedPropertyValue == null) {
            typedPropertyValue = "";
        }

        // determine if the property is typed, otherwise assume it is a string
        if (propertyValue != null && propertyValue.startsWith("(") && propertyValue.endsWith(")")) {
            String dataType = this.getDataTypeSegment(propertyValue);
            String theValue = this.getValueSegment(propertyValue);

            if (dataType.equals("string")) {
                typedPropertyValue = theValue;
            } else if (dataType.equals("integer")) {
                typedPropertyValue = tryParseInteger(theValue);
            } else if (dataType.equals("long")) {
                typedPropertyValue = tryParseLong(theValue);
            } else if (dataType.equals("double")) {
                typedPropertyValue = tryParseDouble(theValue);
            } else if (dataType.equals("float")) {
                typedPropertyValue = tryParseFloat(theValue);
            } else if (dataType.equals("list")) {
                ArrayList<String> items = this.tryParseList(theValue);
                ArrayList typedItems = new ArrayList();
                for (String item : items) {
                    typedItems.add(this.getTypedPropertyValue(item));
                }

                typedPropertyValue = typedItems;
            } else if (dataType.equals("map")) {
                HashMap<String, String> stringProperties = this.tryParseMap(theValue);
                HashMap<String, Object> properties = new HashMap<String, Object>();

                for (Map.Entry<String, String> entry : stringProperties.entrySet()) {
                    properties.put(entry.getKey(), this.getTypedPropertyValue(entry.getValue()));
                }

                typedPropertyValue = properties;
            }
        }

        return typedPropertyValue;
    }

    protected ExtensionResponse tryAppendRexsterAttributesIfJson(ExtensionResponse extResponse, ExtensionMethod methodToCall, String mediaType) {
        if (mediaType.equals(MediaType.APPLICATION_JSON)
            && methodToCall.getExtensionDefinition().tryIncludeRexsterAttributes()) {

            Object obj = extResponse.getJerseyResponse().getEntity();
            if (obj instanceof JSONObject) {
                JSONObject entity = (JSONObject) obj;

                if (entity != null) {
                    try {
                        entity.put(Tokens.VERSION, RexsterApplication.getVersion());
                        entity.put(Tokens.QUERY_TIME, this.sh.stopWatch());
                    } catch (JSONException jsonException) {
                        // nothing bad happening here
                        logger.error("Couldn't add Rexster attributes to response for an extension.");
                    }

                    extResponse = new ExtensionResponse(
                            Response.fromResponse(extResponse.getJerseyResponse()).entity(entity).build());

                }
            }
        }

        return extResponse;
    }


    private HashMap<String, String> tryParseMap(String mapValue) {
        // parens on the ends have been validated already...they must be
        // here to have gotten this far.
        String stripped = mapValue.substring(1, mapValue.length() - 1);

        HashMap<String, String> pairs = new HashMap<String, String>();

        ArrayList<Integer> delimiterPlaces = new ArrayList<Integer>();
        int parensOpened = 0;

        for (int ix = 0; ix < stripped.length(); ix++) {
            char c = stripped.charAt(ix);
            if (c == ',') {
                if (parensOpened == 0) {
                    delimiterPlaces.add(ix);
                }
            } else if (c == '(') {
                parensOpened++;
            } else if (c == ')') {
                parensOpened--;
            }
        }

        int lastPlace = 0;
        int equalPlace = 0;
        for (Integer place : delimiterPlaces) {
            String property = stripped.substring(lastPlace, place);
            equalPlace = property.indexOf("=");
            pairs.put(property.substring(0, equalPlace), property.substring(equalPlace + 1));
            lastPlace = place + 1;
        }

        String property = stripped.substring(lastPlace);
        equalPlace = property.indexOf("=");
        pairs.put(property.substring(0, equalPlace), property.substring(equalPlace + 1));

        return pairs;
    }

    private ArrayList<String> tryParseList(String listValue) {

        // square brackets on the ends have been validated already...they must be
        // here to have gotten this far.
        String stripped = listValue.substring(1, listValue.length() - 1);

        ArrayList<String> items = new ArrayList<String>();

        int place = stripped.indexOf(',');

        if (place > -1) {

            boolean isEndItem = false;
            int parensOpened = 0;
            StringBuffer sb = new StringBuffer();
            for (int ix = 0; ix < stripped.length(); ix++) {
                char c = stripped.charAt(ix);
                if (c == ',') {
                    // a delimiter was found, determine if it is a true
                    // delimiter or if it is part of a value or a separator
                    // with a paren set
                    if (parensOpened == 0) {
                        isEndItem = true;
                    }
                } else if (c == '(') {
                    parensOpened++;
                } else if (c == ')') {
                    parensOpened--;
                }

                if (ix == stripped.length() - 1) {
                    // this is the last character in the value...by default
                    // this is an end item event
                    isEndItem = true;

                    // append the character because it is valid in the value and
                    // not a delimiter that would normally be added.
                    sb.append(c);
                }

                if (isEndItem) {
                    items.add(sb.toString());
                    sb = new StringBuffer();
                    isEndItem = false;
                } else {
                    sb.append(c);
                }
            }
        } else {
            // there is one item in the array or the array is empty
            items.add(stripped);
        }

        return items;
    }

    private String getValueSegment(String propertyValue) {
        // assumes that the propertyValue has open and closed parens
        String value = "";
        String stripped = propertyValue.substring(1, propertyValue.length() - 1);
        int place = stripped.indexOf(',');

        // assume that the first char could be the comma, which
        // would default to a string data type.
        if (place > -1) {
            if (place + 1 < stripped.length()) {
                // stripped doesn't have the trailing paren so take
                // the value from the first character after the comma to the end
                value = stripped.substring(place + 1);
            } else {
                // there is nothing after the comma
                value = "";
            }
        } else {
            // there was no comma, so the whole thing is the value and it
            // is assumed to be a string
            value = stripped;
        }

        return value;
    }

    private String getDataTypeSegment(String propertyValue) {
        // assumes that the propertyValue has open and closed parens
        String dataType = "string";

        // strip the initial parens and read up to the comman.
        // no need to check for string as that is the default
        int place = propertyValue.indexOf(',');
        String inner = propertyValue.substring(1, place).trim();
        if (inner.equals("s") || inner.equals("string")) {
            dataType = "string";
        } else if (inner.equals("i") || inner.equals("integer")) {
            dataType = "integer";
        } else if (inner.equals("d") || inner.equals("double")) {
            dataType = "double";
        } else if (inner.equals("f") || inner.equals("float")) {
            dataType = "float";
        } else if (inner.equals("list")) {
            // TODO: need to ensure square brackets enclose the array
            dataType = "list";
        } else if (inner.equals("l") || inner.equals("long")) {
            dataType = "long";
        } else if (inner.equals("map")) {
            // TODO: need to validate format...outer parens plus name value pairs
            dataType = "map";
        }

        return dataType;
    }

    private Object tryParseInteger(String intValue) {
        Object parsedValue;
        try {
            parsedValue = Integer.parseInt(intValue);
        } catch (NumberFormatException nfe) {
            parsedValue = intValue;
        }

        return parsedValue;
    }

    private Object tryParseFloat(String floatValue) {
        Object parsedValue;
        try {
            parsedValue = Float.parseFloat(floatValue);
        } catch (NumberFormatException nfe) {
            parsedValue = floatValue;
        }

        return parsedValue;
    }

    private Object tryParseLong(String longValue) {
        Object parsedValue;
        try {
            parsedValue = Long.parseLong(longValue);
        } catch (NumberFormatException nfe) {
            parsedValue = longValue;
        }

        return parsedValue;
    }

    private Object tryParseDouble(String doubleValue) {
        Object parsedValue;
        try {
            parsedValue = Double.parseDouble(doubleValue);
        } catch (NumberFormatException nfe) {
            parsedValue = doubleValue;
        }

        return parsedValue;
    }
}
