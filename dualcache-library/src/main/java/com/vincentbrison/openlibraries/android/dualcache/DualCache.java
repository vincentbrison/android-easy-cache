/*
 * Copyright 2014 Vincent Brison.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.vincentbrison.openlibraries.android.dualcache;

import com.jakewharton.disklrucache.DiskLruCache;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;

/**
 * This class intent to provide a very easy to use, reliable, highly configurable caching library
 * for Android.
 *
 * @param <T> is the Class of object to cache.
 */
public class DualCache<T> {

    private static final int VALUES_PER_CACHE_ENTRY = 1;

    private final RamLruCache ramCacheLru;
    private final VolatileCacheSerializer<T> volatileRamSerializer;
    private final VolatileCacheSerializer<T> volatileDiskSerializer;
    private DiskLruCache diskLruCache;
    private final int maxDiskSizeBytes;
    private final File diskCacheFolder;
    private final int appVersion;
    private final DualCacheRamMode ramMode;
    private final DualCacheDiskMode diskMode;
    private final CacheSerializer<T> diskSerializer;
    private final CacheSerializer<T> ramSerializer;
    private final DualCacheLock dualCacheLock = new DualCacheLock();
    private final Logger logger;
    private final LoggerHelper loggerHelper;
    private final Long persistenceTime;

    DualCache(
            int appVersion,
            Logger logger,
            DualCacheRamMode ramMode,
            CacheSerializer<T> ramSerializer,
            int maxRamSizeBytes,
            SizeOf<T> sizeOf,
            DualCacheDiskMode diskMode,
            CacheSerializer<T> diskSerializer,
            int maxDiskSizeBytes,
            File diskFolder,
            Long persistenceTime) {
        this.appVersion = appVersion;
        this.ramMode = ramMode;
        this.ramSerializer = ramSerializer;
        this.diskMode = diskMode;
        this.diskSerializer = diskSerializer;
        this.diskCacheFolder = diskFolder;
        this.logger = logger;
        this.loggerHelper = new LoggerHelper(logger);
        this.persistenceTime = persistenceTime;

        switch (ramMode) {
            case ENABLE_WITH_SPECIFIC_SERIALIZER:
                this.ramCacheLru = new StringLruCache(maxRamSizeBytes);
                break;
            case ENABLE_WITH_REFERENCE:
                if (persistenceTime != null) {
                    this.ramCacheLru = new ReferenceLruCache<>(maxRamSizeBytes, new VolatileSizeOf<>(sizeOf));
                } else {
                    this.ramCacheLru = new ReferenceLruCache<>(maxRamSizeBytes, sizeOf);
                }
                break;
            default:
                this.ramCacheLru = null;
        }

        switch (diskMode) {
            case ENABLE_WITH_SPECIFIC_SERIALIZER:
                this.maxDiskSizeBytes = maxDiskSizeBytes;
                try {
                    openDiskLruCache(diskFolder);
                } catch (IOException e) {
                    logger.logError(e);
                }
                break;
            default:
                this.maxDiskSizeBytes = 0;
        }

        // Init volatile serializers
        if (persistenceTime != null && ramSerializer != null) {
            volatileRamSerializer = new VolatileCacheSerializer<T>(ramSerializer);
        } else {
            volatileRamSerializer = null;
        }

        if (persistenceTime != null && diskSerializer != null) {
            volatileDiskSerializer = new VolatileCacheSerializer<T>(diskSerializer);
        } else {
            volatileDiskSerializer = null;
        }

    }

    private void openDiskLruCache(File diskFolder) throws IOException {
        this.diskLruCache = DiskLruCache.open(
                diskFolder,
                this.appVersion,
                VALUES_PER_CACHE_ENTRY,
                this.maxDiskSizeBytes
        );
    }

    public long getRamUsedInBytes() {
        if (ramCacheLru == null) {
            return -1;
        } else {
            return ramCacheLru.size();
        }
    }

    public long getDiskUsedInBytes() {
        if (diskLruCache == null) {
            return -1;
        } else {
            return diskLruCache.size();
        }

    }

    /**
     * Return the way objects are cached in RAM layer.
     *
     * @return the way objects are cached in RAM layer.
     */
    public DualCacheRamMode getRAMMode() {
        return ramMode;
    }

    /**
     * Return the way objects are cached in disk layer.
     *
     * @return the way objects are cached in disk layer.
     */
    public DualCacheDiskMode getDiskMode() {
        return diskMode;
    }

    /**
     * Put an object in cache.
     *
     * @param key    is the key of the object.
     * @param object is the object to put in cache.
     */
    public void put(String key, T object) {
        // Synchronize put on each entry. Gives concurrent editions on different entries, and atomic
        // modification on the same entry.
        if (ramMode.equals(DualCacheRamMode.ENABLE_WITH_REFERENCE)) {
            if (persistenceTime != null) { // If our entry is volatile
                ramCacheLru.put(key,
                        new VolatileCacheEntry<>(
                                Calendar.getInstance().getTimeInMillis() + persistenceTime,
                                object));
            } else {
                ramCacheLru.put(key, object);
            }
        }

        String ramSerialized = null;
        if (ramMode.equals(DualCacheRamMode.ENABLE_WITH_SPECIFIC_SERIALIZER)) {
            if (persistenceTime != null) { // If our entry is volatile
                ramSerialized = volatileRamSerializer.toString(new VolatileCacheEntry<>(
                        Calendar.getInstance().getTimeInMillis() + persistenceTime,
                        object));
            } else {
                ramSerialized = ramSerializer.toString(object);
            }
            ramCacheLru.put(key, ramSerialized);
        }

        if (diskMode.equals(DualCacheDiskMode.ENABLE_WITH_SPECIFIC_SERIALIZER)) {
            try {
                dualCacheLock.lockDiskEntryWrite(key);
                DiskLruCache.Editor editor = diskLruCache.edit(key);
                if (ramSerializer == diskSerializer) {
                    // Optimization if using same serializer
                    editor.set(0, ramSerialized);
                } else if (persistenceTime != null) { // If our entry is volatile
                    editor.set(0, volatileDiskSerializer.toString(new VolatileCacheEntry<>(
                            Calendar.getInstance().getTimeInMillis() + persistenceTime,
                            object)));
                } else {
                    editor.set(0, diskSerializer.toString(object));
                }
                editor.commit();
            } catch (IOException e) {
                logger.logError(e);
            } finally {
                dualCacheLock.unLockDiskEntryWrite(key);
            }
        }
    }

    /**
     * Return the object of the corresponding key from the cache. In no object is available,
     * return null.
     *
     * @param key is the key of the object.
     * @return the object of the corresponding key from the cache. In no object is available,
     * return null.
     */
    public T get(String key) {

        Object ramResult = null;
        String diskResult = null;
        DiskLruCache.Snapshot snapshotObject = null;

        // Try to get the object from RAM.
        boolean isRamSerialized = ramMode.equals(DualCacheRamMode.ENABLE_WITH_SPECIFIC_SERIALIZER);
        boolean isRamReferenced = ramMode.equals(DualCacheRamMode.ENABLE_WITH_REFERENCE);
        if (isRamSerialized || isRamReferenced) {
            ramResult = ramCacheLru.get(key);
        }

        if (ramResult == null) {
            // Try to get the cached object from disk.
            loggerHelper.logEntryForKeyIsNotInRam(key);
            if (diskMode.equals(DualCacheDiskMode.ENABLE_WITH_SPECIFIC_SERIALIZER)) {
                try {
                    dualCacheLock.lockDiskEntryWrite(key);
                    snapshotObject = diskLruCache.get(key);
                } catch (IOException e) {
                    logger.logError(e);
                } finally {
                    dualCacheLock.unLockDiskEntryWrite(key);
                }

                if (snapshotObject != null) {
                    loggerHelper.logEntryForKeyIsOnDisk(key);
                    try {
                        diskResult = snapshotObject.getString(0);
                    } catch (IOException e) {
                        logger.logError(e);
                    }
                } else {
                    loggerHelper.logEntryForKeyIsNotOnDisk(key);
                }
            }

            T objectFromStringDisk = null;

            if (diskResult != null) {
                VolatileCacheEntry<T> cacheEntry = null;
                if (persistenceTime != null) { // If our entry is volatile
                    cacheEntry = volatileDiskSerializer.fromString(diskResult);
                    if (cacheEntry.getTimestamp().after(Calendar.getInstance().getTime())) {
                        objectFromStringDisk = cacheEntry.getItem();
                    } else {
                        //Invalidate cache
                        delete(key);
                        return null;
                    }
                } else {
                    // Load object, no need to check disk configuration since diskresult != null.
                    objectFromStringDisk = diskSerializer.fromString(diskResult);
                }
                // Refresh object in ram.
                if (ramMode.equals(DualCacheRamMode.ENABLE_WITH_REFERENCE)) {
                    if (diskMode.equals(DualCacheDiskMode.ENABLE_WITH_SPECIFIC_SERIALIZER)) {
                        if (cacheEntry != null) { // If our entry is volatile
                            ramCacheLru.put(key, cacheEntry);
                        } else {
                            ramCacheLru.put(key, objectFromStringDisk);
                        }
                    }
                } else if (ramMode.equals(DualCacheRamMode.ENABLE_WITH_SPECIFIC_SERIALIZER)) {
                    if (diskSerializer == ramSerializer) {
                        ramCacheLru.put(key, diskResult);
                    } else {
                        if (cacheEntry != null) { // If our entry is volatile
                            ramCacheLru.put(key, volatileRamSerializer.toString(cacheEntry));
                        } else {
                            ramCacheLru.put(key, ramSerializer.toString(objectFromStringDisk));
                        }
                    }
                }
                return objectFromStringDisk;
            }
        } else {
            loggerHelper.logEntryForKeyIsInRam(key);
            if (ramMode.equals(DualCacheRamMode.ENABLE_WITH_REFERENCE)) {
                if (persistenceTime != null) { // If our entry is volatile
                    VolatileCacheEntry<T> cacheEntry = (VolatileCacheEntry<T>) ramResult;
                    if (cacheEntry.getTimestamp().after(Calendar.getInstance().getTime())) {
                        return cacheEntry.getItem();
                    } else {
                        delete(key);
                        return null;
                    }
                } else {
                    return (T) ramResult;
                }
            } else if (ramMode.equals(DualCacheRamMode.ENABLE_WITH_SPECIFIC_SERIALIZER)) {
                if (persistenceTime != null) { // If our entry is volatile
                    VolatileCacheEntry<T> cacheEntry = volatileRamSerializer.fromString((String) ramResult);
                    if (cacheEntry.getTimestamp().after(Calendar.getInstance().getTime())) {
                        return cacheEntry.getItem();
                    } else {
                        delete(key);
                        return null;
                    }
                } else {
                    return ramSerializer.fromString((String) ramResult);
                }
            }
        }

        // No data is available.
        return null;
    }

    /**
     * Delete the corresponding object in cache.
     *
     * @param key is the key of the object.
     */
    public void delete(String key) {
        if (!ramMode.equals(DualCacheRamMode.DISABLE)) {
            ramCacheLru.remove(key);
        }
        if (!diskMode.equals(DualCacheDiskMode.DISABLE)) {
            try {
                dualCacheLock.lockDiskEntryWrite(key);
                diskLruCache.remove(key);
            } catch (IOException e) {
                logger.logError(e);
            } finally {
                dualCacheLock.unLockDiskEntryWrite(key);
            }
        }
    }

    /**
     * Remove all objects from cache (both RAM and disk).
     */
    public void invalidate() {
        invalidateDisk();
        invalidateRAM();
    }

    /**
     * Remove all objects from RAM.
     */
    public void invalidateRAM() {
        if (!ramMode.equals(DualCacheRamMode.DISABLE)) {
            ramCacheLru.evictAll();
        }
    }

    /**
     * Remove all objects from Disk.
     */
    public void invalidateDisk() {
        if (!diskMode.equals(DualCacheDiskMode.DISABLE)) {
            try {
                dualCacheLock.lockFullDiskWrite();
                diskLruCache.delete();
                openDiskLruCache(diskCacheFolder);
            } catch (IOException e) {
                logger.logError(e);
            } finally {
                dualCacheLock.unLockFullDiskWrite();
            }
        }
    }

    /**
     * Test if an object is present in cache.
     *
     * @param key is the key of the object.
     * @return true if the object is present in cache, false otherwise.
     */
    public boolean contains(String key) {
        if (!ramMode.equals(DualCacheRamMode.DISABLE) && ramCacheLru.snapshot().containsKey(key)) {
            return true;
        }
        try {
            dualCacheLock.lockDiskEntryWrite(key);
            if (!diskMode.equals(DualCacheDiskMode.DISABLE) && diskLruCache.get(key) != null) {
                return true;
            }
        } catch (IOException e) {
            logger.logError(e);
        } finally {
            dualCacheLock.unLockDiskEntryWrite(key);
        }
        return false;
    }
}
