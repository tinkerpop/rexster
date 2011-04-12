package com.tinkerpop.rexster.extension;

import com.tinkerpop.blueprints.pgm.Graph;
import com.tinkerpop.blueprints.pgm.Vertex;
import com.tinkerpop.frames.FramesManager;
import com.tinkerpop.frames.Property;
import com.tinkerpop.rexster.RexsterResourceContext;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriInfo;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An extension that exposes Tinkerpop Frames via Rexster.  Configuration in rexster.xml looks like:
 *
 * <configuration>
 *   <person>com.tinkerpop.frames.domain.classes.Person</person>
 *   <project>com.tinkerpop.frames.domain.classes.Project</project>
 * </configuration>
 */
@ExtensionNaming(name = FramesExtension.EXTENSION_NAME, namespace = FramesExtension.EXTENSION_NAMESPACE)
public class FramesExtension extends AbstractRexsterExtension {
    protected static Logger logger = Logger.getLogger(GremlinExtension.class);

    public static final String EXTENSION_NAME = "frames";
    public static final String EXTENSION_NAMESPACE = "tp";

    @ExtensionDefinition(extensionPoint = ExtensionPoint.VERTEX)
    @ExtensionDescriptor("Frames extension for a vertex.")
    public ExtensionResponse doGremlinWorkOnVertex(@RexsterContext RexsterResourceContext rexsterResourceContext,
                                                   @RexsterContext Graph graph,
                                                   @RexsterContext Vertex vertex){
        ExtensionResponse extensionResponse;
        FramesManager manager = new FramesManager(graph);

        ExtensionConfiguration extensionConfig = rexsterResourceContext.getRexsterApplicationGraph()
                .findExtensionConfiguration(EXTENSION_NAMESPACE, EXTENSION_NAME);
        Map<String, String> mapFrames = extensionConfig.tryGetMapFromConfiguration();

        if (mapFrames != null && !mapFrames.isEmpty()) {
            UriInfo uriInfo = rexsterResourceContext.getUriInfo();
            List<PathSegment> pathSegmentList = uriInfo.getPathSegments();

            String domainObjectMappingName = "";
            if (pathSegmentList.size() > 5) {
                domainObjectMappingName = pathSegmentList.get(5).getPath();
            }

            if (domainObjectMappingName.isEmpty()) {
                extensionResponse = ExtensionResponse.error(
                        "A Frames class was not specified in the URI", generateErrorJson());
            }

            String frameClassName = mapFrames.get(domainObjectMappingName);
            if (frameClassName == null || frameClassName.isEmpty()) {
                extensionResponse = ExtensionResponse.error(
                        "Frames configuration does not contain a Frames class for " + domainObjectMappingName, generateErrorJson());
            }

            try {
                Class clazz = Class.forName(frameClassName);
                Object obj = manager.frame(vertex, clazz);

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

    protected JSONObject generateApiJson() {
        return null;
    }
}
