package com.tinkerpop.rexster.traversals.grateful;

import com.tinkerpop.blueprints.pgm.Vertex;
import com.tinkerpop.pipes.Pipe;
import com.tinkerpop.pipes.Pipeline;
import com.tinkerpop.rexster.Tokens;
import com.tinkerpop.rexster.traversals.AbstractRankTraversal;
import com.tinkerpop.rexster.traversals.grateful.pipes.SungByInversePipeline;
import com.tinkerpop.rexster.traversals.grateful.pipes.WrittenByPipeline;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class SingerWriterRank extends AbstractRankTraversal {

    private static final String SINGER_WRITER_RANK = "grateful/singer-writer-rank";

    public String getTraversalName() {
        return SINGER_WRITER_RANK;
    }

    public void traverse() throws JSONException {

        Vertex singer = this.getVertex(GratefulDeadTokens.SINGER);
        if (singer != null) {
            Pipe pipe1 = new SungByInversePipeline();
            Pipe pipe2 = new WrittenByPipeline();

            Pipeline<Vertex, Vertex> pipeline = new Pipeline<Vertex, Vertex>(Arrays.asList(pipe1, pipe2));
            pipeline.setStarts(Arrays.asList(singer));
            this.totalRank = incrRank(pipeline, 1.0);
            this.success = true;
        } else {
            this.success = false;
            this.message = "singer not found";
        }
    }

    @Override
    protected void addApiToResultObject() {
        try {
            Map<String, Object> api = new HashMap<String, Object>();
            JSONObject parameters = new JSONObject(super.getParameters());

            parameters.put("singer.<key>", "the source singer artist, where <key> is the singer vertex property key");
            api.put(Tokens.DESCRIPTION, "rank all writers relative to a single singer. the ranking is based on the number of songs that the writter has written that are sung by the singer.");
            api.put(Tokens.PARAMETERS, parameters);

            this.resultObject.put(Tokens.API, api);
        } catch (JSONException e) {
        }
    }
}