package com.vincentbrison.openlibraries.android.dualcache.lib;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import com.vincentbrison.openlibraries.android.dualcache.CacheSerializer;
import com.vincentbrison.openlibraries.android.dualcache.DiskCacheSerializer;
import com.vincentbrison.openlibraries.android.dualcache.DualCache;
import com.vincentbrison.openlibraries.android.dualcache.DualCacheDiskMode;
import com.vincentbrison.openlibraries.android.dualcache.DualCacheRamMode;
import com.vincentbrison.openlibraries.android.dualcache.JsonDiskSerializer;
import com.vincentbrison.openlibraries.android.dualcache.JsonSerializer;
import com.vincentbrison.openlibraries.android.dualcache.SizeOf;
import com.vincentbrison.openlibraries.android.dualcache.IOUtil;
import com.vincentbrison.openlibraries.android.dualcache.lib.testobjects.AbstractVehicule;
import com.vincentbrison.openlibraries.android.dualcache.lib.testobjects.CoolBike;
import com.vincentbrison.openlibraries.android.dualcache.lib.testobjects.CoolCar;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public abstract class DualCacheTest {

    protected static final int RAM_MAX_SIZE = 1000;
    protected static final int DISK_MAX_SIZE = 20 * RAM_MAX_SIZE;
    protected static final String CACHE_NAME = "test";
    protected static final int TEST_APP_VERSION = 0;
    protected DualCache<AbstractVehicule> cache;
    protected CacheSerializer<AbstractVehicule> defaultCacheSerializer;
    protected JsonDiskSerializer<AbstractVehicule> defaultDiskCacheSerializer;
    private Context context;

    protected Context getContext() {
        return context;
    }

    @Before
    public void setUp() throws Exception {
        defaultCacheSerializer = new JsonSerializer<>(AbstractVehicule.class);
        defaultDiskCacheSerializer = new JsonDiskSerializer<>(AbstractVehicule.class);
        context = InstrumentationRegistry.getTargetContext();
    }

    @After
    public void tearDown() throws Exception {
        cache.invalidate();
    }

    @Test
    public void testBasicOperations() throws Exception {
        CoolCar car = new CoolCar();
        String keyCar = "car";
        cache.put(keyCar, car);
        if (cache.getRAMMode().equals(DualCacheRamMode.DISABLE) &&
                cache.getDiskMode().equals(DualCacheDiskMode.DISABLE)) {
            assertNull(cache.get(keyCar));
            assertEquals(false, cache.contains(keyCar));
        } else {
            assertEquals(car, cache.get(keyCar));
            assertEquals(true, cache.contains(keyCar));
        }

        cache.invalidateRAM();
        if (cache.getDiskMode().equals(DualCacheDiskMode.DISABLE)) {
            assertNull(cache.get(keyCar));
            assertEquals(false, cache.contains(keyCar));
        } else {
            assertEquals(car, cache.get(keyCar));
            assertEquals(true, cache.contains(keyCar));
        }

        cache.put(keyCar, car);
        if (cache.getRAMMode().equals(DualCacheRamMode.DISABLE) &&
                cache.getDiskMode().equals(DualCacheDiskMode.DISABLE)) {
            assertNull(cache.get(keyCar));
            assertEquals(false, cache.contains(keyCar));
        } else {
            assertEquals(car, cache.get(keyCar));
            assertEquals(true, cache.contains(keyCar));
        }

        cache.invalidate();
        assertNull(cache.get(keyCar));
        assertEquals(false, cache.contains(keyCar));

        CoolBike bike = new CoolBike();
        cache.put(keyCar, car);
        String keyBike = "bike";
        cache.put(keyBike, bike);
        if (cache.getRAMMode().equals(DualCacheRamMode.DISABLE) &&
                cache.getDiskMode().equals(DualCacheDiskMode.DISABLE)) {
            assertNull(cache.get(keyCar));
            assertEquals(false, cache.contains(keyCar));
            assertNull(cache.get(keyBike));
            assertEquals(false, cache.contains(keyBike));
        } else {
            assertEquals(cache.get(keyCar), car);
            assertEquals(true, cache.contains(keyCar));
            assertEquals(cache.get(keyBike), bike);
            assertEquals(true, cache.contains(keyBike));
        }
    }

    @Test
    public void testBasicOperations2() throws Exception {
        CoolCar car = new CoolCar();
        String keyCar = "car";
        cache.put(keyCar, car);
        cache.invalidateRAM();
        if (cache.getDiskMode().equals(DualCacheDiskMode.DISABLE)) {
            assertNull(cache.get(keyCar));
            assertEquals(false, cache.contains(keyCar));
        } else {
            assertEquals(car, cache.get(keyCar));
            assertEquals(true, cache.contains(keyCar));
            cache.invalidateRAM();
        }

        cache.invalidateDisk();
        assertNull(cache.get(keyCar));
        assertEquals(false, cache.contains(keyCar));

        cache.put(keyCar, car);
        cache.invalidateRAM();
        if (cache.getDiskMode().equals(DualCacheDiskMode.DISABLE)) {
            assertNull(cache.get(keyCar));
            assertEquals(false, cache.contains(keyCar));
        } else {
            assertEquals(car, cache.get(keyCar));
            assertEquals(true, cache.contains(keyCar));
        }

        cache.invalidate();
        assertNull(cache.get(keyCar));
        assertEquals(false, cache.contains(keyCar));

        CoolBike bike = new CoolBike();
        String keyBike = "bike";
        cache.put(keyCar, car);
        cache.put(keyBike, bike);
        cache.delete(keyCar);
        cache.delete(keyBike);
        assertNull(cache.get(keyCar));
        assertEquals(false, cache.contains(keyCar));
        assertNull(cache.get(keyBike));
        assertEquals(false, cache.contains(keyBike));
    }

    @Test
    public void testLRUPolicy() {
        cache.invalidate();
        CoolCar carToEvict = new CoolCar();
        String keyCar = "car";
        cache.put(keyCar, carToEvict);
        long size = cache.getRamUsedInBytes();
        int numberOfItemsToAddForRAMEviction = (int) (RAM_MAX_SIZE / size);
        for (int i = 0; i < numberOfItemsToAddForRAMEviction; i++) {
            cache.put(keyCar + i, new CoolCar());
        }
        cache.invalidateDisk();
        assertNull(cache.get(keyCar));
        assertEquals(false, cache.contains(keyCar));

        cache.put(keyCar, carToEvict);
        for (int i = 0; i < numberOfItemsToAddForRAMEviction; i++) {
            cache.put(keyCar + i, new CoolCar());
        }
        if (!cache.getDiskMode().equals(DualCacheDiskMode.DISABLE)) {
            assertEquals(carToEvict, cache.get(keyCar));
            assertEquals(true, cache.contains(keyCar));
        } else {
            assertNull(cache.get(keyCar));
            assertEquals(false, cache.contains(keyCar));
        }
    }

    @Test
    public void testConcurrentAccess() {
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            threads.add(createWrokerThread(cache));
        }
        Log.d("dualcachedebuglogti", "start worker threads");
        for (Thread thread : threads) {
            thread.start();
        }

        Log.d("dualcachedebuglogti", "joining worker threads");
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        Log.d("dualcachedebuglogti", "join done");
        assertFalse("test", false);
    }

    private Thread createWrokerThread(final DualCache<AbstractVehicule> cache) {
        return new Thread() {
            int sMaxNumberOfRun = 1000;
            @Override
            public void run() {
                String key = "key";
                try {
                    int numberOfRun = 0;
                    while (numberOfRun++ < sMaxNumberOfRun) {
                        Thread.sleep((long) (Math.random() * 2));
                        double choice = Math.random();
                        if (choice < 0.4) {
                            cache.put(key, new CoolCar());
                        } else if (choice < 0.5) {
                            cache.delete(key);
                        } else if (choice < 0.8) {
                            cache.get(key);
                        } else if (choice < 0.9) {
                            cache.contains(key);
                        } else if (choice < 1) {
                            cache.invalidate();
                        } else {
                            // do nothing
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
    }

    public static class SerializerForTesting implements CacheSerializer<AbstractVehicule> {

        @Override
        public AbstractVehicule fromString(String data) {
            if (new String(data).equals(CoolBike.class.getSimpleName())) {
                return new CoolBike();
            } else if (new String(data).equals(CoolCar.class.getSimpleName())) {
                return new CoolCar();
            } else {
                return null;
            }
        }

        @Override
        public String toString(AbstractVehicule object) {
            return object.getClass().getSimpleName();
        }
    }

    public static class DiskSerializerForTesting implements DiskCacheSerializer<AbstractVehicule> {
        private static final Charset CHARSET = Charset.forName("UTF-8");

        @Override
        public AbstractVehicule fromStream(InputStream data) throws IOException {
            char[] buffer = new char[1024];
            InputStreamReader reader = new InputStreamReader(data, CHARSET);
            StringBuilder builder = new StringBuilder();

            try {
                int numRead;
                while ((numRead = reader.read(buffer)) != -1) {
                    builder.append(buffer, 0, numRead);
                }
                final String string = builder.toString();

                if (string.equals(CoolBike.class.getSimpleName())) {
                    return new CoolBike();
                } else if (string.equals(CoolCar.class.getSimpleName())) {
                    return new CoolCar();
                } else {
                    return null;
                }
            } finally {
                IOUtil.closeQuietly(reader);
            }
        }

        @Override
        public void writeToStream(OutputStream out, AbstractVehicule object) throws IOException {
            OutputStreamWriter writer = new OutputStreamWriter(out, CHARSET);
            try {
                writer.write(object.getClass().getSimpleName());
            } finally {
                IOUtil.closeQuietly(writer);
            }
        }
    }

    public static class SizeOfVehiculeForTesting implements SizeOf<AbstractVehicule> {

        @Override
        public int sizeOf(AbstractVehicule object) {
            int size = 0;
            size += object.getName().length() * 2; // we suppose that char = 2 bytes
            size += 4; // we suppose that int = 4 bytes
            return size;
        }
    }
}
