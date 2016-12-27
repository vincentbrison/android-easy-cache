package com.vincentbrison.openlibraries.android.dualcache;

import java.sql.Timestamp;

/**
 * Created by Iago on 26/12/2016.
 * Wraps the catchable object setting a duration
 */
class VolatileCacheEntry<T> {

    private Timestamp timestamp;
    private T item;

    VolatileCacheEntry(Long time, T item) {
        this.timestamp = new Timestamp(time);
        this.item = item;
    }

    Timestamp getTimestamp() {
        return timestamp;
    }

    T getItem() {
        return item;
    }

}
