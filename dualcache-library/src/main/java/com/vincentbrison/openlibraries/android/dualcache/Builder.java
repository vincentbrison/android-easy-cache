package com.vincentbrison.openlibraries.android.dualcache;

import android.content.Context;

import java.io.File;

/**
 * Class used to build a cache.
 *
 * @param <T> is the class of object to store in cache.
 */
public class Builder<T> {

    /**
     * Defined the sub folder from {@link android.content.Context#getCacheDir()} used to store all
     * the data generated from the use of this library.
     */
    private static final String CACHE_FILE_PREFIX = "dualcache";

    private String id;
    private int appVersion;
    private boolean logEnabled;
    private int maxRamSizeBytes;
    private DualCacheRamMode ramMode;
    private CacheSerializer<T> ramSerializer;
    private SizeOf<T> sizeOf;
    private int maxDiskSizeBytes;
    private DualCacheDiskMode diskMode;
    private CacheSerializer<T> diskSerializer;
    private File diskFolder;
    private Long persistenceTime = null;

    /**
     * Start the building of the cache.
     *
     * @param id         is the id of the cache (should be unique).
     * @param appVersion is the app version of the app. If data are already stored in disk cache
     *                   with previous app version, it will be invalidate.
     */
    public Builder(String id, int appVersion) {
        this.id = id;
        this.appVersion = appVersion;
        this.ramMode = null;
        this.diskMode = null;
        this.logEnabled = false;
    }

    /**
     * Enabling log from the cache. By default disable.
     *
     * @return the builder.
     */
    public Builder<T> enableLog() {
        this.logEnabled = true;
        return this;
    }

    /**
     * Builder the cache. Exception will be thrown if it can not be created.
     *
     * @return the cache instance.
     */
    public DualCache<T> build() {
        if (ramMode == null) {
            throw new IllegalStateException("No ram mode set");
        }
        if (diskMode == null) {
            throw new IllegalStateException("No disk mode set");
        }

        DualCache<T> cache = new DualCache<>(
            appVersion,
            new Logger(logEnabled),
            ramMode,
            ramSerializer,
            maxRamSizeBytes,
            sizeOf,
            diskMode,
            diskSerializer,
            maxDiskSizeBytes,
                diskFolder,
                persistenceTime
        );

        boolean isRamDisable = cache.getRAMMode().equals(DualCacheRamMode.DISABLE);
        boolean isDiskDisable = cache.getDiskMode().equals(DualCacheDiskMode.DISABLE);

        if (isRamDisable && isDiskDisable) {
            throw new IllegalStateException(
                "The ram cache layer and the disk cache layer are "
                    + "disable. You have to use at least one of those "
                    + "layers.");
        }

        return cache;
    }

    /**
     * Use Json serialization/deserialization to store and retrieve object from ram cache.
     *
     * @param maxRamSizeBytes is the max amount of ram in bytes which can be used by the ram cache.
     * @param serializer      is the cache interface which provide serialization/deserialization
     *                        methods
     *                        for the ram cache layer.
     * @return the builder.
     */
    public Builder<T> useSerializerInRam(
        int maxRamSizeBytes, CacheSerializer<T> serializer
    ) {
        this.ramMode = DualCacheRamMode.ENABLE_WITH_SPECIFIC_SERIALIZER;
        this.maxRamSizeBytes = maxRamSizeBytes;
        this.ramSerializer = serializer;
        return this;
    }

    /**
     * Store directly objects in ram (without serialization/deserialization).
     * You have to provide a way to compute the size of an object in
     * ram to be able to used the LRU capacity of the ram cache.
     *
     * @param maxRamSizeBytes is the max amount of ram which can be used by the ram cache.
     * @param handlerSizeOf   computes the size of object stored in ram.
     * @return the builder.
     */
    public Builder<T> useReferenceInRam(
        int maxRamSizeBytes, SizeOf<T> handlerSizeOf
    ) {
        this.ramMode = DualCacheRamMode.ENABLE_WITH_REFERENCE;
        this.maxRamSizeBytes = maxRamSizeBytes;
        this.sizeOf = handlerSizeOf;
        return this;
    }

    /**
     * The ram cache will not be used, meaning that only the disk cache will be used.
     *
     * @return the builder for the disk cache layer.
     */
    public Builder<T> noRam() {
        this.ramMode = DualCacheRamMode.DISABLE;
        return this;
    }

    /**
     * Use custom serialization/deserialization to store and retrieve objects from disk cache.
     *
     * @param maxDiskSizeBytes is the max size of disk in bytes which an be used by the disk cache
     *                         layer.
     * @param usePrivateFiles  is true if you want to use {@link Context#MODE_PRIVATE} with the
     *                         default disk cache folder.
     * @param serializer       provides serialization/deserialization methods for the disk cache
     *                         layer.
     * @param context          is used to access file system.
     * @return the builder.
     */
    public Builder<T> useSerializerInDisk(
        int maxDiskSizeBytes,
        boolean usePrivateFiles,
        CacheSerializer<T> serializer,
        Context context
    ) {
        File folder = getDefaultDiskCacheFolder(usePrivateFiles, context);
        return useSerializerInDisk(maxDiskSizeBytes, folder, serializer);
    }

    /**
     * Use custom serialization/deserialization to store and retrieve object from disk cache.
     *
     * @param maxDiskSizeBytes is the max size of disk in bytes which an be used by the disk cache
     *                         layer.
     * @param diskCacheFolder  is the folder where the disk cache will be stored.
     * @param serializer       provides serialization/deserialization methods for the disk cache
     *                         layer.
     * @return the builder.
     */
    public Builder<T> useSerializerInDisk(
        int maxDiskSizeBytes, File diskCacheFolder, CacheSerializer<T> serializer
    ) {
        this.diskFolder = diskCacheFolder;
        this.diskMode = DualCacheDiskMode.ENABLE_WITH_SPECIFIC_SERIALIZER;
        this.maxDiskSizeBytes = maxDiskSizeBytes;
        this.diskSerializer = serializer;
        return this;
    }

    /**
     * Set a persistence time for cache entries
     *
     * @param timeInMillis time a cache entry can persist
     * @return the builder
     */
    public Builder<T> setPersistenceTime(long timeInMillis) {
        this.persistenceTime = timeInMillis;
        return this;
    }

    private File getDefaultDiskCacheFolder(boolean usePrivateFiles, Context context) {
        File folder;
        if (usePrivateFiles) {
            folder = context.getDir(
                CACHE_FILE_PREFIX + this.id,
                Context.MODE_PRIVATE
            );
        } else {
            folder = new File(context.getCacheDir().getPath()
                                  + "/" + CACHE_FILE_PREFIX
                                  + "/" + this.id
            );
        }
        return folder;
    }

    /**
     * Use this if you do not want use the disk cache layer, meaning that only the ram cache layer
     * will be used.
     *
     * @return the builder.
     */
    public Builder<T> noDisk() {
        this.diskMode = DualCacheDiskMode.DISABLE;
        return this;
    }
}
