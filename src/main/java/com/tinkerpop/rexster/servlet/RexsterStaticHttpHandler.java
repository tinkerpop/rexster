package com.tinkerpop.rexster.servlet;

import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.grizzly.http.server.StaticHttpHandler;
import org.glassfish.grizzly.http.server.io.OutputBuffer;
import org.glassfish.grizzly.http.server.util.MimeType;
import org.glassfish.grizzly.http.util.HttpStatus;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * This class is a near replica of the StaticHttpHandler.  The only difference is that it does not cache resources.
 * Caching (at least in how it was implemented by Grizzly) caused problems in Dog House as mime-types for
 * all resources were being returned as "text/html" even if they were clearly css/png/etc.
 * <p/>
 * Will drop this class when problem with StaticHttpHandler is resolved.
 */
public class RexsterStaticHttpHandler extends StaticHttpHandler {
    public RexsterStaticHttpHandler(String path) {
        super(path);
    }

    @Override
    protected boolean handle(final String uri,
                             final Request req,
                             final Response res) throws Exception {

        boolean found = false;

        final File[] fileFolders = docRoots.getArray();
        if (fileFolders == null) {
            return false;
        }

        File resource = null;

        for (int i = 0; i < fileFolders.length; i++) {
            final File webDir = fileFolders[i];
            // local file
            resource = new File(webDir, uri);
            final boolean exists = resource.exists();
            final boolean isDirectory = resource.isDirectory();

            if (exists && isDirectory) {
                final File f = new File(resource, "/index.html");
                if (f.exists()) {
                    resource = f;
                    found = true;
                    break;
                }
            }

            if (isDirectory || !exists) {
                found = false;
            } else {
                found = true;
                break;
            }
        }

        if (!found) {
            /*
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "File not found  {0}", resource);
            }
            */
            return false;
        }

        // ********************************************
        // * commented out this bit here for caching...keep an eye on grizzly for a fix
        // ********************************************
        // addToFileCache(req, resource);
        // ********************************************

        sendAFile(res, resource);

        return true;
    }

    public static void sendAFile(final Response response, final File file)
            throws IOException {
        final String path = file.getPath();
        final FileInputStream fis = new FileInputStream(file);

        try {
            response.setStatus(HttpStatus.OK_200);
            String substr;
            int dot = path.lastIndexOf('.');
            if (dot < 0) {
                substr = file.toString();
                dot = substr.lastIndexOf('.');
            } else {
                substr = path;
            }
            if (dot > 0) {
                String ext = substr.substring(dot + 1);
                String ct = MimeType.get(ext);
                if (ct != null) {
                    response.setContentType(ct);
                }
            } else {
                response.setContentType(MimeType.get("html"));
            }

            final long length = file.length();
            response.setContentLengthLong(length);

            final OutputBuffer outputBuffer = response.getOutputBuffer();

            byte b[] = new byte[8192];
            int rd;
            while ((rd = fis.read(b)) > 0) {
                //chunk.setBytes(b, 0, rd);
                outputBuffer.write(b, 0, rd);
            }
        } finally {
            try {
                fis.close();
            } catch (IOException ignore) {
            }
        }
    }
}
