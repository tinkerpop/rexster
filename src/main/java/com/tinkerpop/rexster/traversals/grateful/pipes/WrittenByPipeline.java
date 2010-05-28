package com.tinkerpop.rexster.traversals.grateful.pipes;

import com.tinkerpop.blueprints.pgm.Vertex;
import com.tinkerpop.pipes.serial.Pipe;
import com.tinkerpop.pipes.serial.Pipeline;
import com.tinkerpop.pipes.serial.filter.ComparisonFilterPipe;
import com.tinkerpop.pipes.serial.pgm.EdgeVertexPipe;
import com.tinkerpop.pipes.serial.pgm.LabelFilterPipe;
import com.tinkerpop.pipes.serial.pgm.VertexEdgePipe;
import com.tinkerpop.rexster.traversals.grateful.GratefulDeadTokens;

import java.util.Arrays;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class WrittenByPipeline extends Pipeline<Vertex, Vertex> {

    public WrittenByPipeline() {
        Pipe pipe1 = new VertexEdgePipe(VertexEdgePipe.Step.OUT_EDGES);
        Pipe pipe2 = new LabelFilterPipe(GratefulDeadTokens.WRITTEN_BY, ComparisonFilterPipe.Filter.EQUALS);
        Pipe pipe3 = new EdgeVertexPipe(EdgeVertexPipe.Step.IN_VERTEX);
        this.setPipes(Arrays.asList(pipe1, pipe2, pipe3));
    }
}