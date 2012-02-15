package com.tinkerpop.rexster.kibbles.frames;

import com.tinkerpop.blueprints.pgm.Edge;
import com.tinkerpop.blueprints.pgm.Element;
import com.tinkerpop.blueprints.pgm.Graph;
import com.tinkerpop.blueprints.pgm.Vertex;
import com.tinkerpop.frames.Direction;
import com.tinkerpop.frames.FramesManager;
import com.tinkerpop.frames.Property;
import com.tinkerpop.rexster.RexsterResourceContext;
import com.tinkerpop.rexster.extension.AbstractRexsterExtension;
import com.tinkerpop.rexster.extension.ExtensionConfiguration;
import com.tinkerpop.rexster.extension.ExtensionDefinition;
import com.tinkerpop.rexster.extension.ExtensionDescriptor;
import com.tinkerpop.rexster.extension.ExtensionMethod;
import com.tinkerpop.rexster.extension.ExtensionNaming;
import com.tinkerpop.rexster.extension.ExtensionPoint;
import com.tinkerpop.rexster.extension.ExtensionRequestParameter;
import com.tinkerpop.rexster.extension.ExtensionResponse;
import com.tinkerpop.rexster.extension.RexsterContext;
import org.apache.log4j.Logger;

import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An extension that exposes Tinkerpop Frames via Rexster.  Configuration in rexster.xml looks like:
 * <p/>
 * <configuration>
 * <person>com.tinkerpop.frames.domain.classes.Person</person>
 * <project>com.tinkerpop.frames.domain.classes.Project</project>
 * </configuration>
 */
@ExtensionNaming(name = FramesExtension.EXTENSION_NAME, namespace = FramesExtension.EXTENSION_NAMESPACE)
public class FramesExtension extends AbstractRexsterExtension {
    protected static Logger logger = Logger.getLogger(FramesExtension.class);

    public static final String EXTENSION_NAME = "frames";
    public static final String EXTENSION_NAMESPACE = "tp";

    public static final String TOKEN_STANDARD = "standard";
    public static final String TOKEN_INVERSE = "inverse";

    @ExtensionDefinition(extensionPoint = ExtensionPoint.EDGE)
    @ExtensionDescriptor(description = "Frames extension for an edge.")
    public ExtensionResponse doFramesWorkOnEdge(@RexsterContext RexsterResourceContext rexsterResourceContext,
                                                @RexsterContext Graph graph,
                                                @RexsterContext Edge edge,
                                                @ExtensionRequestParameter(name = "direction", description = "the direction of the edge (must be \"" + TOKEN_STANDARD + "\" or \"" + TOKEN_INVERSE + "\" with the default being \"" + TOKEN_STANDARD + "\"") String directionString) {
        Direction direction = Direction.STANDARD;
        if (directionString != null && !directionString.isEmpty()) {
            if (directionString.equals(TOKEN_STANDARD)) {
                direction = Direction.STANDARD;
            } else if (directionString.equals(TOKEN_INVERSE)) {
                direction = Direction.INVERSE;
            } else {
                ExtensionMethod extMethod = rexsterResourceContext.getExtensionMethod();
                return ExtensionResponse.error(
                        "the direction parameter must be (must be \"" + TOKEN_STANDARD + "\" or \"" + TOKEN_INVERSE + "\"",
                        null,
                        Response.Status.BAD_REQUEST.getStatusCode(),
                        null,
                        generateErrorJson(extMethod.getExtensionApiAsJson()));
            }
        }

        return this.frameItUp(rexsterResourceContext, graph, edge, direction);
    }

    @ExtensionDefinition(extensionPoint = ExtensionPoint.VERTEX)
    @ExtensionDescriptor(description = "Frames extension for a vertex.")
    public ExtensionResponse doFramesWorkOnVertex(@RexsterContext RexsterResourceContext rexsterResourceContext,
                                                  @RexsterContext Graph graph,
                                                  @RexsterContext Vertex vertex) {
        return this.frameItUp(rexsterResourceContext, graph, vertex, null);
    }

    /**
     * Frames up a graph element.
     *
     * @param rexsterResourceContext The Rexster context.
     * @param graph                  The graph from which the element is framed.
     * @param element                A vertex or an edge.
     * @param direction              The direction of the edge.  Only relevant for frame edges and should be
     *                               set to null for vertices.
     * @return The response.
     */
    private ExtensionResponse frameItUp(RexsterResourceContext rexsterResourceContext, Graph graph, Element element, Direction direction) {

        if (element instanceof Edge && direction == null) {
            throw new IllegalArgumentException("Direction cannot be null");
        }

        ExtensionResponse extensionResponse;
        FramesManager manager = new FramesManager(graph);

        ExtensionConfiguration extensionConfig = rexsterResourceContext.getRexsterApplicationGraph()
                .findExtensionConfiguration(EXTENSION_NAMESPACE, EXTENSION_NAME);
        Map<String, String> mapFrames = extensionConfig.tryGetMapFromConfiguration();

        if (mapFrames != null && !mapFrames.isEmpty()) {
            UriInfo uriInfo = rexsterResourceContext.getUriInfo();
            List<PathSegment> pathSegmentList = uriInfo.getPathSegments();

            String domainObjectMappingName = "";
            if (pathSegmentList.size() > 6) {
                domainObjectMappingName = pathSegmentList.get(6).getPath();
            }

            if (domainObjectMappingName.isEmpty()) {
                return ExtensionResponse.error(
                        "A Frames class was not specified in the URI", generateErrorJson());
            }

            String frameClassName = mapFrames.get(domainObjectMappingName);
            if (frameClassName == null || frameClassName.isEmpty()) {
                return ExtensionResponse.error(
                        "Frames configuration does not contain a Frames class for " + domainObjectMappingName, generateErrorJson());
            }

            try {
                Class clazz = Class.forName(frameClassName);
                Object obj = null;

                if (element instanceof Vertex) {
                    obj = manager.frame((Vertex) element, clazz);
                } else if (element instanceof Edge) {
                    obj = manager.frame((Edge) element, Direction.STANDARD, clazz);
                }

                Map map = new HashMap();

                Method[] methods = clazz.getMethods();
                for (Method method : methods) {
                    // assumes that the properties are unique within a frame
                    Property property = method.getAnnotation(Property.class);
                    if (property != null && method.getName().startsWith("get")) {
                        map.put(property.value(), method.invoke(obj, null));
                    }
                }

                extensionResponse = ExtensionResponse.ok(map);
            } catch (Exception x) {
                logger.error("Frames encountered a problem with " + frameClassName, x);
                extensionResponse = ExtensionResponse.error(
                        "Frames encountered a problem with " + frameClassName, generateErrorJson());
            }
        } else {
            // bad configuration
            extensionResponse = ExtensionResponse.error(
                    "Frames configuration is not valid.  Please check rexster.xml", generateErrorJson());
        }

        return extensionResponse;
    }

    /**
     * By default this returns true.  Overriding classes should evaluate the configuration to determine
     * if it is correct.
     */
    @Override
    public boolean isConfigurationValid(ExtensionConfiguration extensionConfiguration) {
        boolean valid = false;

        if (extensionConfiguration != null) {
            Map<String, String> mapFrames = extensionConfiguration.tryGetMapFromConfiguration();
            valid = mapFrames != null && !mapFrames.isEmpty() && mapFrames.size() > 0;
        }

        return valid;
    }
}
