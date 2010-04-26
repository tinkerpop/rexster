package com.tinkerpop.rexster.traversals.grateful;

import com.tinkerpop.blueprints.pgm.Vertex;
import com.tinkerpop.pipes.serial.Pipe;
import com.tinkerpop.pipes.serial.Pipeline;
import com.tinkerpop.pipes.serial.filter.ObjectFilterPipe;
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

    public String getResourceName() {
        return CO_FOLLOWS_RANK;
    }

    public void traverse() {

        Vertex song = this.getVertex(GratefulDeadTokens.SONG);

        if (song != null) {
            Pipe pipe1 = new FollowsPipeline();
            Pipe pipe2 = new FollowsInversePipeline();
            Pipe pipe3 = new ObjectFilterPipe<Vertex>(Arrays.asList(song), true);

            Pipeline<Vertex, Vertex> pipeline = new Pipeline<Vertex, Vertex>(Arrays.asList(pipe1, pipe2, pipe3));
            pipeline.setStarts(Arrays.asList(song).iterator());
            this.totalRank = incrRank(pipeline, 1.0f);
            this.success = true;
        } else {
            this.success = false;
            this.message = "song not found";
        }
    }

    public void addApiToResultObject() {
        Map<String, Object> api = new HashMap<String, Object>();
        Map<String, String> parameters = this.getBaseApi();
        parameters.put("song.<key>", "the source song, where <key> is the song vertex property key");
        api.put("description", "rank all songs relative to the source song by the number of times they co-follow the source song");
        api.put("parameters", parameters);
        this.resultObject.put("api", api);
    }
}