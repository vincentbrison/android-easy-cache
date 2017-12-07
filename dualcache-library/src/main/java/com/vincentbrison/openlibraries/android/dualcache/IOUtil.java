package com.vincentbrison.openlibraries.android.dualcache;

import java.io.Closeable;
import java.io.IOException;

public final class IOUtil {

    private IOUtil() { }

    /**
     * Closes the stream and swallows up the exception
     */
    public static void closeQuietly(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (IOException e) {
            // ignore
        }
    }
}
