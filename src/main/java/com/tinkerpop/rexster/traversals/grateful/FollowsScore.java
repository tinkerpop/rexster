package com.tinkerpop.rexster.traversals.grateful;

import com.tinkerpop.blueprints.pgm.Vertex;
import com.tinkerpop.pipes.Pipe;
import com.tinkerpop.pipes.PipeHelper;
import com.tinkerpop.rexster.Tokens;
import com.tinkerpop.rexster.traversals.AbstractScoreTraversal;
import com.tinkerpop.rexster.traversals.grateful.pipes.FollowsPipeline;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.jettison.json.JSONException;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class FollowsScore extends AbstractScoreTraversal {

    private static final String FOLLOWS_SCORE = "grateful/follows-score";

    public String getTraversalName() {
        return FOLLOWS_SCORE;
    }

    public void traverse() {
        Vertex song = this.getVertex(GratefulDeadTokens.SONG);
        if (song != null) {
            Pipe pipe1 = new FollowsPipeline();
            pipe1.setStarts(Arrays.asList(song));
            this.score = new Float(PipeHelper.counter(pipe1));
            this.success = true;
        } else {
            this.success = false;
            this.message = "song not found";
        }
    }

    public void addApiToResultObject() {
        Map<String, Object> api = new HashMap<String, Object>();
        Map<String, Object> parameters = this.getParameters();
        parameters.put("song.<key>", "the source song, where <key> is the song vertex property key");
        api.put(Tokens.DESCRIPTION, "scores a song by how many songs follow it.");
        api.put(Tokens.PARAMETERS, parameters);
        
        try {
        	this.resultObject.put(Tokens.API, api);
        } catch (JSONException e) {}
    }
}