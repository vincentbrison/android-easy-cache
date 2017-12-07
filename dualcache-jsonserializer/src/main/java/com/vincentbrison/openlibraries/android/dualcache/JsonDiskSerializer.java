package com.vincentbrison.openlibraries.android.dualcache;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Serializer which will serialize and deserialize object using <a href="https://github.com/FasterXML/jackson">Jackson</a>
 * converter.
 * @param <T> is the class of object to serialize/deserialize.
 */
public class JsonDiskSerializer<T> implements DiskCacheSerializer<T> {
    private final ObjectMapper mapper;
    private final Class<T> clazz;

    /**
     * Default constructor.
     * @param clazz is the class of object to serialize/deserialize.
     */
    public JsonDiskSerializer(Class<T> clazz) {
        this.clazz = clazz;
        mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
    }

    @Override
    public T fromStream(InputStream data) {
        try {
            return mapper.readValue(data, clazz);
        } catch (IOException e) {
            e.printStackTrace();
        }
        throw new IllegalStateException();
    }

    @Override
    public void writeToStream(OutputStream out, T object) {
        try {
            mapper.writeValue(out, object);
            return;
        } catch (IOException e) {
            e.printStackTrace();
        }
        throw new IllegalStateException();
    }
}
