package com.tinkerpop.rexster.traversals.grateful.pipes;

import com.tinkerpop.blueprints.pgm.Vertex;
import com.tinkerpop.pipes.Pipe;
import com.tinkerpop.pipes.Pipeline;
import com.tinkerpop.pipes.filter.ComparisonFilterPipe;
import com.tinkerpop.pipes.pgm.EdgeVertexPipe;
import com.tinkerpop.pipes.pgm.LabelFilterPipe;
import com.tinkerpop.pipes.pgm.VertexEdgePipe;
import com.tinkerpop.rexster.traversals.grateful.GratefulDeadTokens;

import java.util.Arrays;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class FollowsPipeline extends Pipeline<Vertex, Vertex> {

    public FollowsPipeline() {
        Pipe pipe1 = new VertexEdgePipe(VertexEdgePipe.Step.OUT_EDGES);
        Pipe pipe2 = new LabelFilterPipe(GratefulDeadTokens.FOLLOWED_BY, ComparisonFilterPipe.Filter.NOT_EQUAL);
        Pipe pipe3 = new EdgeVertexPipe(EdgeVertexPipe.Step.IN_VERTEX);
        this.setPipes(Arrays.asList(pipe1, pipe2, pipe3));
    }
}
