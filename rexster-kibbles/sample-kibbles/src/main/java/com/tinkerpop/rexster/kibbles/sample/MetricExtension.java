package com.tinkerpop.rexster.kibbles.sample;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.tinkerpop.rexster.RexsterResourceContext;
import com.tinkerpop.rexster.extension.ExtensionDefinition;
import com.tinkerpop.rexster.extension.ExtensionDescriptor;
import com.tinkerpop.rexster.extension.ExtensionNaming;
import com.tinkerpop.rexster.extension.ExtensionPoint;
import com.tinkerpop.rexster.extension.ExtensionResponse;
import com.tinkerpop.rexster.extension.RexsterContext;

/**
 * An extension that shows how to register custom metrics to extensions to expose via reporters.
 *
 * Rexster utilizes "Metrics":http://metrics.codahale.com/ for gathering and reporting measurements on internal
 * activity.  Rexster already tracks and reports many measurements on its internal operations and extension activity
 * can be generally measured through existing REST metrics.  For a greater level of control and flexibility, as
 * well as a finer degree of granularity to measurements within a specific extension, there are options to
 * register new metrics within the extension itself.
 *
 * To try this extension after deployment of the samples to the packaged "emtpygraph", resolve this url:
 *
 * http://localhost:8182/graphs/emptygraph/tp-sample/metric
 *
 * and then view the reported metrics by going here:
 *
 * http://localhost:8182/metrics
 *
 * This url will resolve to a JSON file that contains a key for "counters" that looks something like this:
 *
 * counters: { http.rest.extension.sample.counter: { count: 3 } }
 */
@ExtensionNaming(namespace = AbstractSampleExtension.EXTENSION_NAMESPACE, name = MetricExtension.EXTENSION_NAME)
public class MetricExtension extends AbstractSampleExtension {

    public static final String EXTENSION_NAME = "metric";

    @ExtensionDefinition(extensionPoint = ExtensionPoint.GRAPH)
    @ExtensionDescriptor(description = "a counter extension.")
    public ExtensionResponse evaluateCounter(@RexsterContext RexsterResourceContext context) {

        // get or create a sample counter metric
        final Counter counter = context.getMetricRegistry().counter(
                MetricRegistry.name("http", "rest", "extension", "sample", "counter"));

        // increment the counter for each request.
        counter.inc();

        return ExtensionResponse.noContent();
    }
}
