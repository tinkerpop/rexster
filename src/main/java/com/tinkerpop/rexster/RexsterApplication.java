package com.tinkerpop.rexster;

import com.tinkerpop.rexster.traversals.Traversal;
import org.apache.log4j.Logger;
import org.restlet.Application;
import org.restlet.Restlet;
import org.restlet.routing.Router;

import java.util.ServiceLoader;

/**
 * @author: Marko A. Rodriguez (http://markorodriguez.com)
 */
public class RexsterApplication extends Application {

    protected static Logger logger = Logger.getLogger(RexsterApplication.class);

    public Restlet createRoot() {
        Router router = new Router(getContext());
        router.attachDefault(RexsterResource.class);
        ServiceLoader<Traversal> traversalServices = ServiceLoader.load(Traversal.class);
        for (Traversal traversalService : traversalServices) {
            logger.info("loading traversal: /" + traversalService.getResourceName() + " [" + traversalService.getClass().getName() + "]");
            router.attach("/" + traversalService.getResourceName(), traversalService.getClass());
        }
        return router;
    }
}
