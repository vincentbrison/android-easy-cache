package com.vincentbrison.openlibraries.android.dualcache;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.vb.openlibraries.android.dualcache.R;

import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * This Activity provide a very simple demo usage of the dualcache.
 */
public class DemoActivity extends Activity {

    /**
     * Intent extra key to retrieve the disk cache size as an integer.
     */
    public static final String EXTRA_DISK_CACHE_SIZE = "EXTRA_DISK_CACHE_SIZE";

    /**
     * Intent extra key to retrieve the ram cache size as an integer.
     */
    public static final String EXTRA_RAM_CACHE_SIZE = "EXTRA_RAM_CACHE_SIZE";

    /**
     * Intent extra key to retrieve the id of the cache as a string.
     */
    public static final String EXTRA_ID_CACHE = "EXTRA_ID_CACHE";

    private int mDiskCacheSize;
    private int mRamCacheSize;
    private String mCacheId;
    private DualCache<String> mCache;

    private Handler mHandler;

    //CHECKSTYLE:OFF
    @BindView(R.id.buttonAddObjectAToCache) protected Button mButtonAddObjectA;
    @BindView(R.id.buttonAddObjectBToCache) protected Button mButtonAddObjectB;
    @BindView(R.id.buttonAddRandomObjectToCache) protected Button mButtonAddRandomObject;
    @BindView(R.id.buttonDisplayObjectB) protected Button mButtonDisplayObjectB;
    @BindView(R.id.buttonDisplayObjectA) protected Button mButtonDisplayObjectA;
    @BindView(R.id.buttonDisplayRandomObject) protected Button mButtonDisplayRandomObject;
    @BindView(R.id.buttonInvalidateCache) protected Button mButtonInvalidateCache;
    @BindView(R.id.textViewDataSizeRam) protected TextView mTextViewDataRam;
    @BindView(R.id.textViewDataSizeDisk) protected TextView mTextViewDataDisk;
    @BindView(R.id.textViewDataTime) protected TextView mTextViewDataTime;
    //CHECKSTYLE:ON

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_demo);
        ButterKnife.bind(this);

        mCacheId = getIntent().getStringExtra(EXTRA_ID_CACHE);
        mDiskCacheSize = getIntent().getIntExtra(EXTRA_DISK_CACHE_SIZE, 100);
        mRamCacheSize = getIntent().getIntExtra(EXTRA_RAM_CACHE_SIZE, 50);

        CacheSerializer<String> jsonSerializer = new JsonSerializer<>(String.class);
        DiskCacheSerializer<String> jsonDiskSerializer = new JsonDiskSerializer<>(String.class);

        mCache = new Builder<String>(mCacheId, 1)
            .enableLog()
            .useSerializerInRam(mRamCacheSize, jsonSerializer)
            .useSerializerInDisk(mDiskCacheSize, true, jsonDiskSerializer, getApplicationContext())
            .build();

        mHandler = new Handler();
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                refreshCacheSize();
                mHandler.postDelayed(this, 500);
            }
        });

        mButtonAddObjectA.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCache.put("a", "objectA");
            }
        });

        mButtonAddObjectB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCache.put("b", "objectB");
            }
        });

        mButtonAddRandomObject.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCache.put(UUID.randomUUID().toString(), UUID.randomUUID().toString());
            }
        });

        mButtonDisplayObjectB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                long start = System.currentTimeMillis();
                String result = mCache.get("b");
                long end = System.currentTimeMillis();
                long time = end - start;
                if (result != null) {
                    Toast.makeText(DemoActivity.this, result, Toast.LENGTH_SHORT).show();
                }
                mTextViewDataTime.setText("Last access time : " + time + " ms");
            }
        });

        mButtonDisplayObjectA.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                long start = System.currentTimeMillis();
                String result = mCache.get("a");
                long end = System.currentTimeMillis();
                long time = end - start;
                if (result != null) {
                    Toast.makeText(DemoActivity.this, result, Toast.LENGTH_SHORT).show();
                }
                mTextViewDataTime.setText("Last access time : " + time + " ms");
            }
        });

        mButtonInvalidateCache.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCache.invalidate();
            }
        });
    }

    private void refreshCacheSize() {
        mTextViewDataRam.setText("Ram : " + mCache.getRamUsedInBytes() + "/" + mRamCacheSize + " B");
        mTextViewDataDisk.setText("Disk : " + mCache.getDiskUsedInBytes() + "/" + mDiskCacheSize + " B");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacksAndMessages(null);
    }
}

