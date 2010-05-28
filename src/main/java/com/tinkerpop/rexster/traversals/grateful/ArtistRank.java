package com.tinkerpop.rexster.traversals.grateful;

import com.tinkerpop.blueprints.pgm.Element;
import com.tinkerpop.blueprints.pgm.Vertex;
import com.tinkerpop.pipes.serial.Pipe;
import com.tinkerpop.pipes.serial.Pipeline;
import com.tinkerpop.pipes.serial.filter.ComparisonFilterPipe;
import com.tinkerpop.pipes.serial.pgm.EdgeVertexPipe;
import com.tinkerpop.pipes.serial.pgm.LabelFilterPipe;
import com.tinkerpop.pipes.serial.pgm.VertexEdgePipe;
import com.tinkerpop.rexster.RexsterTokens;
import com.tinkerpop.rexster.traversals.AbstractRankTraversal;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class ArtistRank extends AbstractRankTraversal {

    private static final String ARTIST_RANK = "grateful/artist-rank";

    public String getTraversalName() {
        return ARTIST_RANK;
    }

    public void traverse() {

        String type = this.getRequestValue(GratefulDeadTokens.ARTIST_TYPE);

        if (null != type && type.equals("writer"))
            type = GratefulDeadTokens.WRITTEN_BY;
        else if (null != type && type.equals("singer"))
            type = GratefulDeadTokens.SUNG_BY;

        if (type != null) {
            this.totalRank = 0.0f;
            for (Element element : this.graph.getIndex().get(GratefulDeadTokens.TYPE, GratefulDeadTokens.SONG)) {
                Vertex song = (Vertex) element;
                Pipe pipe1 = new VertexEdgePipe(VertexEdgePipe.Step.OUT_EDGES);
                Pipe pipe2 = new LabelFilterPipe(type, ComparisonFilterPipe.Filter.EQUALS);
                Pipe pipe3 = new EdgeVertexPipe(EdgeVertexPipe.Step.IN_VERTEX);
                Pipeline<Vertex, Vertex> pipeline = new Pipeline<Vertex, Vertex>(Arrays.asList(pipe1, pipe2, pipe3));
                pipeline.setStarts(Arrays.asList(song));
                this.totalRank = this.totalRank + incrRank(pipeline, 1.0f);
            }
            this.success = true;
        } else {
            this.success = false;
            this.message = "artist type not found (user writer or singer)";
        }
    }

    public void addApiToResultObject() {
        Map<String, Object> api = new HashMap<String, Object>();
        Map<String, Object> parameters = this.getParameters();
        parameters.put(GratefulDeadTokens.ARTIST_TYPE, "must be writer or singer");
        api.put(RexsterTokens.DESCRIPTION, "rank all writers (or singers) based on the number of songs they have written (or sung)");
        api.put(RexsterTokens.PARAMETERS, parameters);
        this.resultObject.put(RexsterTokens.API, api);
    }
}