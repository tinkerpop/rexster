package com.tinkerpop.rexster.traversals.grateful;

import com.tinkerpop.blueprints.pgm.Vertex;
import com.tinkerpop.pipes.Pipe;
import com.tinkerpop.pipes.Pipeline;
import com.tinkerpop.pipes.filter.ComparisonFilterPipe;
import com.tinkerpop.pipes.filter.ObjectFilterPipe;
import com.tinkerpop.rexster.Tokens;
import com.tinkerpop.rexster.traversals.AbstractRankTraversal;
import com.tinkerpop.rexster.traversals.grateful.pipes.FollowsInversePipeline;
import com.tinkerpop.rexster.traversals.grateful.pipes.FollowsPipeline;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class CoFollowsRank extends AbstractRankTraversal {

    private static final String CO_FOLLOWS_RANK = "grateful/co-follows-rank";

    public String getTraversalName() {
        return CO_FOLLOWS_RANK;
    }

    public void traverse() throws JSONException{

        Vertex song = this.getVertex(GratefulDeadTokens.SONG);

        if (song != null) {
            Pipe pipe1 = new FollowsPipeline();
            Pipe pipe2 = new FollowsInversePipeline();
            Pipe pipe3 = new ObjectFilterPipe<Vertex>(song, ComparisonFilterPipe.Filter.EQUAL);

            Pipeline<Vertex, Vertex> pipeline = new Pipeline<Vertex, Vertex>(Arrays.asList(pipe1, pipe2, pipe3));
            pipeline.setStarts(Arrays.asList(song));
            this.totalRank = incrRank(pipeline, 1.0);
            this.success = true;
        } else {
            this.success = false;
            this.message = "song not found";
        }
    }

    public void addApiToResultObject() {
    	try {
	        Map<String, Object> api = new HashMap<String, Object>();
	        JSONObject parameters = new JSONObject(super.getParameters());
	        
	        parameters.put("song.<key>", "the source song, where <key> is the song vertex property key");
	        api.put(Tokens.DESCRIPTION, "rank all songs relative to the source song by the number of times they co-follow the source song");
	        api.put(Tokens.PARAMETERS, parameters);
        
        	this.resultObject.put(Tokens.API, api);
        } catch (JSONException e) {}
    }
}