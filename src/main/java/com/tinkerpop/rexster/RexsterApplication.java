package com.tinkerpop.rexster;

import com.tinkerpop.blueprints.pgm.Graph;
import com.tinkerpop.blueprints.pgm.impls.neo4j.Neo4jGraph;
import com.tinkerpop.blueprints.pgm.impls.orientdb.OrientGraph;
import com.tinkerpop.blueprints.pgm.impls.tg.TinkerGraph;
import com.tinkerpop.blueprints.pgm.parser.GraphMLReader;
import com.tinkerpop.rexster.traversals.Traversal;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.restlet.Application;
import org.restlet.Restlet;
import org.restlet.routing.Router;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class RexsterApplication extends Application {

    private static final String version = "0.1-SNAPSHOT";
    private final long startTime = System.currentTimeMillis();
    protected static final Logger logger = Logger.getLogger(RexsterApplication.class);
    private ResultObjectCache resultObjectCache;
    private Configuration properties;
    private Map<String, RexsterApplicationGraph> graphs = new HashMap<String, RexsterApplicationGraph>();
    
    static {
        PropertyConfigurator.configure(RexsterApplication.class.getResource("log4j.properties"));
    }

    public RexsterApplication(final String graphName, final Graph graph) {
    	RexsterApplicationGraph rag = new RexsterApplicationGraph(graphName); 
        this.graphs.put(graphName, rag);
        logger.info("Graph " + rag.getGraph() + " loaded");
        this.resultObjectCache = new ResultObjectCache();
    }

    public RexsterApplication(final XMLConfiguration properties) {
        try {
            this.properties = properties;
            
            // get the graph configurations from the XML config file
            List graphConfigs = properties.configurationsAt("graphs.graph");
            
            // create one graph for each configuration (each <graph> element)
        	for(Iterator it = graphConfigs.iterator(); it.hasNext();)
        	{
        		HierarchicalConfiguration graphConfig = (HierarchicalConfiguration) it.next();
        	    
        		Graph graph = createGraphFromProperties(graphConfig);
        		
        		RexsterApplicationGraph rag = new RexsterApplicationGraph(graphConfig.getString(Tokens.REXSTER_GRAPH_NAME));
        		rag.setGraph(graph);
        		rag.loadPackageNames(graphConfig.getString(Tokens.REXSTER_PACKAGES_ALLOWED));
        		
        		this.graphs.put(rag.getGraphName(), rag);
        		
	            logger.info("Graph " + graph + " loaded");
        	}
        	
        	this.resultObjectCache = new ResultObjectCache();
        	
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // todo: clean up

    public Restlet createRoot() {
        Router router = new Router(getContext());
        
        // return Rexster information
        router.attachDefault(RexsterResource.class);
        
        ServiceLoader<Traversal> traversalServices = ServiceLoader.load(Traversal.class);
        
        for (RexsterApplicationGraph rag : this.graphs.values()){
        	        
        	// return specific graph information
	        router.attach("/" + rag.getGraphName(), RexsterGraphResource.class);
	        
	        if (rag.hasPackages()) {
	        	for (Traversal traversalService : traversalServices) {
	                if (-1 == traversalService.getTraversalName().indexOf("/")) {
	                    if (rag.getPackageNames().contains("")) {
	                        logger.info("loading traversal: /" + rag.getGraphName() + "/" + traversalService.getTraversalName() + " [" + traversalService.getClass().getName() + "]");
	                        router.attach("/" + rag.getGraphName() + "/" + traversalService.getTraversalName(), traversalService.getClass());
	                        rag.getLoadedTraversals().put(traversalService.getTraversalName(), traversalService.getClass());
	                    }
	                } else {
	                    if (rag.getPackageNames().contains(traversalService.getTraversalName().substring(0, traversalService.getTraversalName().indexOf("/")))) {
	                        logger.info("loading traversal: /" + rag.getGraphName() + "/" + traversalService.getTraversalName() + " [" + traversalService.getClass().getName() + "]");
	                        router.attach("/" + rag.getGraphName() + "/" + traversalService.getTraversalName(), traversalService.getClass());
	                        rag.getLoadedTraversals().put(traversalService.getTraversalName(), traversalService.getClass());
	                    }
	                }
	            }
	        } else {
	            for (Traversal traversalService : traversalServices) {
	                logger.info("loading traversal: /" + rag.getGraphName() + "/" + traversalService.getTraversalName() + " [" + traversalService.getClass().getName() + "]");
	                router.attach("/" + rag.getGraphName() + "/" + traversalService.getTraversalName(), traversalService.getClass());
	                rag.getLoadedTraversals().put(traversalService.getTraversalName(), traversalService.getClass());
	            }
	        }
	
	        router.attach("/" + rag.getGraphName() + "/vertices", VertexResource.class);
	        router.attach("/" + rag.getGraphName() + "/vertices/{id}", VertexResource.class);
	        router.attach("/" + rag.getGraphName() + "/vertices/{id}/{direction}", VertexResource.class);
	        
	        router.attach("/" + rag.getGraphName() + "/edges", EdgeResource.class);
	        router.attach("/" + rag.getGraphName() + "/edges/{id}", EdgeResource.class);
        
        }
        return router;
    }

    public Map<String, Class> getLoadedTraversalServices(String graphName) {
        return this.graphs.get(graphName).getLoadedTraversals();
    }

    public static String getVersion() {
        return version;
    }

    public Graph getGraph(String graphName) {
        return this.graphs.get(graphName).getGraph();
    }
    
    public int getGraphCount() {
        return this.graphs.size();
    }

    public ResultObjectCache getResultObjectCache() {
        return this.resultObjectCache;
    }

    public long getStartTime() {
        return this.startTime;
    }

    public void stop() throws Exception {
        super.stop();
        
        // graph may not have been initialized properly if an exception gets tossed in
        // on graph creation
        for (RexsterApplicationGraph rag : this.graphs.values()){

        	Graph graph = rag.getGraph();
            logger.info("Shutting down " + graph);
        	if (graph != null){
            	graph.shutdown();
            }
        }
        
    }

    protected static Graph createGraphFromProperties(final Configuration properties) throws Exception {
        
	    String graphType = properties.getString(Tokens.REXSTER_GRAPH_TYPE);
        String graphFile = properties.getString(Tokens.REXSTER_GRAPH_FILE);
	
        Graph graph;
        if (graphType.equals("neo4j")) {
            try {
                Properties neo4jProperties = new Properties();
                neo4jProperties.load(RexsterApplication.class.getResourceAsStream("neo4j.properties"));
                //logger.info("Loading Neo4j properties: " + neo4jProperties.toString());
                graph = new Neo4jGraph(graphFile, new HashMap<String, String>((Map) neo4jProperties));
            } catch (IOException e) {
                graph = new Neo4jGraph(graphFile);
            }
            ((Neo4jGraph) graph).setAutoTransactions(false);
        } else if (graphType.equals("orientdb")) {
        	Properties orientDbProperties = new Properties();
            orientDbProperties.load(RexsterApplication.class.getResourceAsStream("orientdb.properties"));

            // can't call the open method on orientdb if these two properties aren't
            // established.
            if (!orientDbProperties.containsKey("username") || !orientDbProperties.containsKey("password")) {
            	throw new Exception("Properties for orientdb configuration must include a username and password property.");
            }
            
            String username = orientDbProperties.getProperty("username");
            String password = orientDbProperties.getProperty("password");
            
            // calling the open method opens the connection to graphdb.  looks like the 
            // implementation of shutdown will call the orientdb close method.
            graph = new OrientGraph(graphFile).open(username, password);
        	
        } else if (graphType.equals("tinkergraph")) {
            graph = new TinkerGraph();
            GraphMLReader.inputGraph(graph, new FileInputStream(graphFile));
        } else {
            throw new Exception(graphType + " is not a supported graph type");
        }
        return graph;
    }
}
