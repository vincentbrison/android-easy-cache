package com.vincentbrison.openlibraries.android.dualcache;

/**
 * Created by Iago on 26/12/2016.
 */

class VolatileCacheSerializer<T> implements CacheSerializer<VolatileCacheEntry<T>> {

    private static final String TIMESTAMP_KEY = " timestamp_key:";

    private CacheSerializer<T> serializer;

    VolatileCacheSerializer(CacheSerializer<T> serializer) {
        this.serializer = serializer;
    }

    /**
     * Create a {@link VolatileCacheEntry} from a string
     *
     * @param data is the byte array representing the serialized data.
     * @return {@link VolatileCacheEntry}
     */
    @Override
    public VolatileCacheEntry<T> fromString(String data) {
        String[] items = data.split(TIMESTAMP_KEY);
        return new VolatileCacheEntry<T>(Long.valueOf(items[1]), serializer.fromString(items[0]));
    }

    /**
     * Add the timestamp to the serializable object
     *
     * @param object is the object to serialize.
     * @return object serialized + timestamp serialized
     */
    @Override
    public String toString(VolatileCacheEntry<T> object) {
        return serializer.toString(object.getItem()) + TIMESTAMP_KEY + object.getTimestamp().getTime();
    }
}
