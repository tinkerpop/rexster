package com.tinkerpop.rexster.protocol;

import com.tinkerpop.rexster.Tokens;
import com.tinkerpop.rexster.protocol.msg.MessageFlag;
import com.tinkerpop.rexster.protocol.msg.MsgPackScriptResponseMessage;
import com.tinkerpop.rexster.protocol.msg.ScriptRequestMessage;
import org.msgpack.MessagePack;
import org.msgpack.type.Value;
import org.msgpack.unpacker.BufferUnpacker;
import org.msgpack.unpacker.Converter;
import org.msgpack.unpacker.UnpackerIterator;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import static org.msgpack.template.Templates.tMap;
import static org.msgpack.template.Templates.TString;
import static org.msgpack.template.Templates.TValue;

/**
 * A bit of an experiment.
 */
public class TryRexProSessionless {

    private static final byte[] emptyBindings;

    static {{
        byte [] empty;
        try {
            empty = BitWorks.convertSerializableBindingsToByteArray(new RexsterBindings());
        } catch (IOException ioe) {
            empty = new byte[0];
        }

        emptyBindings = empty;
    }}

    public static void main(final String[] args) throws Exception {
        int c = Integer.parseInt(args[1]);
        final CountDownLatch latch = new CountDownLatch(c);

        for (int ix = 0; ix < c; ix++) {
            new Thread(new Runnable() {

                @Override
                public void run() {
                    lotsOfCalls(false, args[0]);
                    latch.countDown();
                }
            }).start();
        }

        latch.await();
    }

    private static void lotsOfCalls(boolean doJson, final String host){

        MessagePack msgpack = new MessagePack();

        long start = System.currentTimeMillis();

        long checkpoint = System.currentTimeMillis();

        try {

            MsgPackScriptResponseMessage resultMessage = (MsgPackScriptResponseMessage)
                    RexPro.sendMessage(host, 8184, createScriptRequestMessage("g=rexster.getGraph('gratefulgraph');g.V;"));

            BufferUnpacker unpacker = msgpack.createBufferUnpacker(resultMessage.Results);
            unpacker.setArraySizeLimit(Integer.MAX_VALUE);
            unpacker.setMapSizeLimit(Integer.MAX_VALUE);
            unpacker.setRawSizeLimit(Integer.MAX_VALUE);

            int counter = 1;
            UnpackerIterator itty = unpacker.iterator();
            while (itty.hasNext()){
                final Map<String,Value> map = new Converter(msgpack, itty.next()).read(tMap(TString, TValue));
                final String vId = map.get(Tokens._ID).asRawValue().getString();

                MsgPackScriptResponseMessage vertexResultMessage = (MsgPackScriptResponseMessage) RexPro.sendMessage(host, 8184,
                          createScriptRequestMessage("g=rexster.getGraph('gratefulgraph');g.v(" + vId + ")"), 100);

                unpacker = msgpack.createBufferUnpacker(vertexResultMessage.Results);
                System.out.println(unpacker.read(tMap(TString, TValue)));

                counter++;
            }

            long end = System.currentTimeMillis() - checkpoint;
            System.out.println((checkpoint - start) + ":" + end);
            System.out.println(counter / (end / 1000));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static ScriptRequestMessage createScriptRequestMessage(String script) throws IOException {
        ScriptRequestMessage scriptMessage = new ScriptRequestMessage();
        scriptMessage.Script = script;
        scriptMessage.Bindings = emptyBindings;
        scriptMessage.LanguageName = "groovy";
        scriptMessage.Flag = MessageFlag.SCRIPT_REQUEST_NO_SESSION;
        scriptMessage.setRequestAsUUID(UUID.randomUUID());
        return scriptMessage;
    }
}