package com.tinkerpop.rexster.traversals.grateful;

import com.tinkerpop.blueprints.pgm.Element;
import com.tinkerpop.blueprints.pgm.Index;
import com.tinkerpop.blueprints.pgm.IndexableGraph;
import com.tinkerpop.blueprints.pgm.Vertex;
import com.tinkerpop.pipes.Pipe;
import com.tinkerpop.pipes.Pipeline;
import com.tinkerpop.pipes.filter.ComparisonFilterPipe;
import com.tinkerpop.pipes.pgm.EdgeVertexPipe;
import com.tinkerpop.pipes.pgm.LabelFilterPipe;
import com.tinkerpop.pipes.pgm.VertexEdgePipe;
import com.tinkerpop.rexster.Tokens;
import com.tinkerpop.rexster.traversals.AbstractRankTraversal;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

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

    protected void traverse() throws JSONException {

        String type = this.getRequestValue(GratefulDeadTokens.ARTIST_TYPE);

        if (null != type && type.equals("writer"))
            type = GratefulDeadTokens.WRITTEN_BY;
        else if (null != type && type.equals("singer"))
            type = GratefulDeadTokens.SUNG_BY;

        if (type != null) {
            this.totalRank = 0.0f;
            for (Element element : ((IndexableGraph) this.graph).getIndex(Index.VERTICES, Element.class).get(GratefulDeadTokens.TYPE, GratefulDeadTokens.SONG)) {
                Vertex song = (Vertex) element;
                Pipe pipe1 = new VertexEdgePipe(VertexEdgePipe.Step.OUT_EDGES);
                Pipe pipe2 = new LabelFilterPipe(type, ComparisonFilterPipe.Filter.NOT_EQUAL);
                Pipe pipe3 = new EdgeVertexPipe(EdgeVertexPipe.Step.IN_VERTEX);
                Pipeline<Vertex, Vertex> pipeline = new Pipeline<Vertex, Vertex>(Arrays.asList(pipe1, pipe2, pipe3));
                pipeline.setStarts(Arrays.asList(song));
                this.totalRank = this.totalRank + incrRank(pipeline, 1.0);
            }
            this.success = true;
        } else {
            this.success = false;
            this.message = "artist type not found (user writer or singer)";
        }
    }

    protected void addApiToResultObject() {
        try {
            Map<String, Object> api = new HashMap<String, Object>();
            JSONObject parameters = new JSONObject(super.getParameters());

            parameters.put(GratefulDeadTokens.ARTIST_TYPE, "must be writer or singer");
            api.put(Tokens.DESCRIPTION, "rank all writers (or singers) based on the number of songs they have written (or sung)");
            api.put(Tokens.PARAMETERS, parameters);

            this.resultObject.put(Tokens.API, api);
        } catch (JSONException e) {
        }
    }
}