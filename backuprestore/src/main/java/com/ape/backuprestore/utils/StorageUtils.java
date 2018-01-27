package com.ape.backuprestore.utils;


import android.content.Context;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.IOException;


public class StorageUtils {
    // for storage index
    public static final int SDM_INTERANL_STORAGE_INDEX = 0;
    public static final int SDM_SDCARD_STORAGE_INDEX = 1;
    public static final int SDM_USBOTG_STORAE_INDEX = 2;
    public static final int SDM_NUM_STORAGE_INDEX = 3;
    public final static int MINIMUM_SIZE = 512;
    private static final String TAG = "StorageUtils";
    private static final String getVolumeList = "getVolumeList";
    private static final String getPath = "getPath";
    private static final String getMaxFileSize = "getMaxFileSize";
    private static final String getVolumeState = "getVolumeState";
    private static final String isRemovable = "isRemovable";
    private static volatile StorageUtils sInstance;
    private String[] mStoragePath;
    private StorageManager mStorageManager;
    private Context mContext;

    private StorageUtils(Context context) {
        mContext = context;
        mStorageManager = (StorageManager) mContext.getSystemService(Context.STORAGE_SERVICE);
        mStoragePath = new String[SDM_NUM_STORAGE_INDEX];
    }

    public static StorageUtils getInstance(Context context) {
        if (sInstance == null) {
            synchronized (StorageUtils.class) {
                if (sInstance == null) {
                    sInstance = new StorageUtils(context);
                }
            }
        }
        return sInstance;
    }

    public static long getAvailableSize(String file) {
        android.os.StatFs stat = new android.os.StatFs(file);
        long count = stat.getAvailableBlocks();
        long size = stat.getBlockSize();
        long totalSize = count * size;
        Logger.d(TAG, "file remain size = " + totalSize);
        return totalSize;
    }

    private void resetStoragePath() {
        if (mStoragePath != null && mStoragePath.length > 0) {
            for (int i = 0; i < mStoragePath.length; i++) {
                mStoragePath[i] = null;
            }
        }
    }

    public String[] getStoragePath() {
        final long start = System.currentTimeMillis();
        Log.d(TAG, "getStoragePath: start....");
        resetStoragePath();
        final Object[] storageVolumes = ReflectUtils.reflect(mStorageManager).method(getVolumeList).get();
        Log.d(TAG, "getStoragePath: storageVolumes.length = " + storageVolumes.length);
        for (Object storageVolume : storageVolumes) {
            final String path = ReflectUtils.reflect(storageVolume).method(getPath).get();
            Log.d(TAG, "getStoragePath: path = " + path);
            if (!TextUtils.isEmpty(path)) {
                long maxFileSize = ReflectUtils.reflect(storageVolume).method(getMaxFileSize).get();
                final String status = ReflectUtils.reflect(mStorageManager).method(getVolumeState, path).get();
                boolean isRemove = ReflectUtils.reflect(storageVolume).method(isRemovable).get();
                Log.i(TAG, "getStoragePath path = " + path
                        + ", isRemovable = " + isRemove
                        + ", status = " + status + ", maxFileSize = " + maxFileSize);
                if (status.equalsIgnoreCase(Environment.MEDIA_MOUNTED)) {
                    if (isRemove) {
                        if (path.contains("otg")) {
                            mStoragePath[SDM_USBOTG_STORAE_INDEX] = path;
                        } else {//  sd
                            mStoragePath[SDM_SDCARD_STORAGE_INDEX] = path;
                        }

                    } else {// internal
                        mStoragePath[SDM_INTERANL_STORAGE_INDEX] = path;
                    }
                }
            }

        }
        Log.d(TAG, "getStoragePath: end.... cost time = " + (System.currentTimeMillis() - start) + "ms");
        return mStoragePath;
    }

    public String getSavePath() {
        final String[] paths = StorageUtils.getInstance(mContext).getStoragePath();
        for (int i = paths.length - 1; i >= 0; i--) {
            final String path = paths[i];
            if (!TextUtils.isEmpty(path)) {
                Log.d(TAG, "getSavePath: path = " + path);
                return path;
            }
        }
        return null;
    }

    public String getBackupPath() {
        String storagePath = getSavePath()
                + File.separator + Constants.ModulePath.FOLDER_BACKUP;
        Logger.d(TAG, "getStoragePath: path is " + storagePath);
        File file = new File(storagePath);
        if (file.exists() && file.isDirectory()) {
            return storagePath;
        } else if (file.mkdirs()) {
            return storagePath;
        } else {
            return null;
        }
    }

    public boolean isStorageMissing() {
        boolean isStorageMissing = false;
        String path = getBackupPath();
        if (path == null) {
            isStorageMissing = true;
        } else {
            // create file to check for sure
            File temp = new File(path + File.separator + ".temp");
            if (temp.exists()) {
                if (!temp.delete()) {
                    isStorageMissing = true;
                }
            } else {
                try {
                    if (!temp.createNewFile()) {
                        isStorageMissing = true;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    Logger.e(TAG, "Cannot create temp file");
                    isStorageMissing = true;
                } finally {
                    temp.delete();
                }
            }
        }
        return isStorageMissing;
    }

}

