package com.vincentbrison.openlibraries.android.dualcache;

/**
 * Created by Iago on 26/12/2016.
 */

public class VolatileSizeOf<T> implements SizeOf<VolatileCacheEntry<T>> {

    private SizeOf<T> sizeOf;

    public VolatileSizeOf(SizeOf<T> sizeOf) {
        this.sizeOf = sizeOf;
    }

    /**
     * Add size of long to sizeof object
     *
     * @param object is the instance against the computation has to be done.
     * @return size of {@link VolatileCacheEntry}
     */
    @Override
    public int sizeOf(VolatileCacheEntry<T> object) {
        return sizeOf.sizeOf(object.getItem()) + 8; // We suppose long = 8 bytes
    }
}
