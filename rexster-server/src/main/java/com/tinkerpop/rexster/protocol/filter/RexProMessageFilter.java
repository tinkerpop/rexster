package com.tinkerpop.rexster.protocol.filter;

import com.tinkerpop.rexster.protocol.RexProSession;
import com.tinkerpop.rexster.protocol.msg.ConsoleScriptResponseMessage;
import com.tinkerpop.rexster.protocol.msg.ErrorResponseMessage;
import com.tinkerpop.rexster.protocol.msg.MessageType;
import com.tinkerpop.rexster.protocol.msg.RexProMessage;
import com.tinkerpop.rexster.protocol.msg.ScriptRequestMessage;
import com.tinkerpop.rexster.protocol.msg.SessionRequestMessage;
import com.tinkerpop.rexster.protocol.msg.SessionResponseMessage;
import org.apache.log4j.Logger;
import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;
import org.glassfish.grizzly.memory.MemoryManager;
import org.msgpack.MessagePack;
import org.msgpack.packer.Packer;
import org.msgpack.unpacker.Unpacker;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RexProMessageFilter extends BaseFilter {

    private static final Logger logger = Logger.getLogger(RexProSession.class);
    private static final MessagePack msgpack = new MessagePack();

    public NextAction handleRead(final FilterChainContext ctx) throws IOException {
        // Get the source buffer from the context
        final Buffer sourceBuffer = ctx.getMessage();
        final int sourceBufferLength = sourceBuffer.remaining();
        sourceBuffer.rewind();
        byte[] messageAsBytes = new byte[sourceBufferLength];
        sourceBuffer.get(messageAsBytes);

        ByteArrayInputStream in = new ByteArrayInputStream(messageAsBytes);
        Unpacker unpacker = msgpack.createUnpacker(in);
        byte messageType = unpacker.readByte();

        RexProMessage message = null;
        if (messageType == MessageType.SCRIPT_REQUEST) {
            message = unpacker.read(ScriptRequestMessage.class);
        } else if (messageType == MessageType.SESSION_REQUEST) {
            message = unpacker.read(SessionRequestMessage.class);
        } else if (messageType == MessageType.SCRIPT_RESPONSE) {
            message = unpacker.read(ConsoleScriptResponseMessage.class);
        } else if (messageType == MessageType.SESSION_RESPONSE) {
            message = unpacker.read(SessionResponseMessage.class);
        } else if (messageType == MessageType.ERROR) {
            message = unpacker.read(ErrorResponseMessage.class);
        }

        if (message == null) {
            logger.warn("Message did not match an expected type.");

            ErrorResponseMessage errorMessage = new ErrorResponseMessage();
            errorMessage.setSessionAsUUID(RexProMessage.EMPTY_SESSION);
            errorMessage.Request = new byte[0];
            errorMessage.ErrorMessage = "Message did not match an expected type.";
            errorMessage.Flag = ErrorResponseMessage.FLAG_ERROR_MESSAGE_VALIDATION;

            ctx.write(errorMessage);
        }

        ctx.setMessage(message);

        sourceBuffer.tryDispose();

        return ctx.getInvokeAction();
    }

    public NextAction handleWrite(final FilterChainContext ctx) throws IOException {
        // Get the source message to be written
        final List<RexProMessage> messages = new ArrayList<RexProMessage>();
        try {
            List<RexProMessage> msgs = ctx.getMessage();
            messages.addAll(msgs);
        } catch (ClassCastException cce) {
            RexProMessage msg = ctx.getMessage();
            messages.add(msg);
        }

        final RexProMessage message = messages.get(0);

        List<RexProMessage> remainingMessages = null;
        if (messages.size() > 1) {
            remainingMessages = messages.subList(1, messages.size());
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Packer packer = msgpack.createPacker(out);

        if (message instanceof SessionResponseMessage) {
            packer.write(MessageType.SESSION_RESPONSE);
        } else if (message instanceof ConsoleScriptResponseMessage) {
            packer.write(MessageType.SCRIPT_RESPONSE);
        } else if (message instanceof ErrorResponseMessage) {
            packer.write(MessageType.ERROR);
        } else if (message instanceof ScriptRequestMessage) {
            packer.write(MessageType.SCRIPT_REQUEST);
        } else if (message instanceof SessionRequestMessage) {
            packer.write(MessageType.SESSION_REQUEST);
        }

        packer.write(message);
        byte[] messageAsBytes = out.toByteArray();
        out.close();

        final int size = messageAsBytes.length;

        // Retrieve the memory manager
        final MemoryManager memoryManager =
                ctx.getConnection().getTransport().getMemoryManager();

        // allocate the buffer of required size
        final Buffer output = memoryManager.allocate(size);
        output.put(messageAsBytes);

        // Allow Grizzly core to dispose the buffer, once it's written
        output.allowBufferDispose(true);

        // Set the Buffer as a context message
        ctx.setMessage(output.flip());

        // Instruct the FilterChain to call the next filter
        return ctx.getInvokeAction(remainingMessages);
    }
}
