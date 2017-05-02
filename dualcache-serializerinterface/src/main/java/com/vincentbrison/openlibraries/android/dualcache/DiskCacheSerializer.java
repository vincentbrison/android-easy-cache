package com.vincentbrison.openlibraries.android.dualcache;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * This cache interface describe the way an object should be serialized/deserialized into a
 * stream.
 * @param <T> is the class of object to serialized/deserialized.
 */
public interface DiskCacheSerializer<T> {
    /**
     * Deserialization of a stream into an object.
     * @param data is the byte array representing the serialized data.
     * @return the deserialized data.
     * @throws IOException if reading from the stream fails
     */
    T fromStream(InputStream data) throws IOException;

    /**
     * Serialization of an object into a stream.
     * @param object is the object to serialize.
     * @throws IOException if writing to the stream fails
     */
    void writeToStream(OutputStream out, T object) throws IOException;
}
