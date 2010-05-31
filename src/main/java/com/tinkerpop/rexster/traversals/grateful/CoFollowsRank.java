package com.tinkerpop.rexster.traversals.grateful;

import com.tinkerpop.blueprints.pgm.Vertex;
import com.tinkerpop.pipes.Pipe;
import com.tinkerpop.pipes.Pipeline;
import com.tinkerpop.pipes.filter.ComparisonFilterPipe;
import com.tinkerpop.pipes.filter.ObjectFilterPipe;
import com.tinkerpop.rexster.RexsterTokens;
import com.tinkerpop.rexster.traversals.AbstractRankTraversal;
import com.tinkerpop.rexster.traversals.grateful.pipes.FollowsInversePipeline;
import com.tinkerpop.rexster.traversals.grateful.pipes.FollowsPipeline;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class CoFollowsRank extends AbstractRankTraversal {

    private static final String CO_FOLLOWS_RANK = "grateful/co-follows-rank";

    public String getTraversalName() {
        return CO_FOLLOWS_RANK;
    }

    public void traverse() {

        Vertex song = this.getVertex(GratefulDeadTokens.SONG);

        if (song != null) {
            Pipe pipe1 = new FollowsPipeline();
            Pipe pipe2 = new FollowsInversePipeline();
            Pipe pipe3 = new ObjectFilterPipe<Vertex>(song, ComparisonFilterPipe.Filter.NOT_EQUALS);

            Pipeline<Vertex, Vertex> pipeline = new Pipeline<Vertex, Vertex>(Arrays.asList(pipe1, pipe2, pipe3));
            pipeline.setStarts(Arrays.asList(song));
            this.totalRank = incrRank(pipeline, 1.0f);
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
        api.put(RexsterTokens.DESCRIPTION, "rank all songs relative to the source song by the number of times they co-follow the source song");
        api.put(RexsterTokens.PARAMETERS, parameters);
        this.resultObject.put(RexsterTokens.API, api);
    }
}