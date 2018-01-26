package com.ape.backuprestore.utils;

import android.content.Context;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.util.Log;

import java.io.File;
import java.lang.reflect.Method;


public class StorageDirManager {
    // for storage index
    public static final int SDM_INTERANL_STORAGE_INDEX = 0;
    public static final int SDM_SDCARD_STORAGE_INDEX = 1;
    public static final int SDM_USBOTG_STORAE_INDEX = 2;
    public static final int SDM_NUM_STORAGE_INDEX = 3;
    public static final int STORAGE_NOT_ENOUGH = 100;
    public static final long LOW_STORAGE_THRESHOLD = 50000000;
    public static final String FOLDER_PATH = "/" + Environment.DIRECTORY_DCIM + "/Camera";
    public static final String RAW_FOLDER_PATH = File.separator + Environment.DIRECTORY_DCIM
            + File.separator + "Camera" + File.separator + "Raw";
    private static final String TAG = "storageDirManager";
    private static final String STORAGEMANAGER = "android.os.storage.StorageManager";
    private static final String STORAGEVOLUME = "android.os.storage.StorageVolume";
    private static volatile StorageDirManager sInstance;
    public boolean mStorageFull = false;
    // the method
    private Method getVolumeState = null;
    private Method getVolumeList = null;
    private Method getPath = null;
    private Method isRemovable = null;
    private Method getMaxFileSize = null;
    private StorageManager mStorageManager = null;
    private Context mContext;
    private Object[] mStorageVolumes;
    private String[] mStoragePath;
    private long[] mStorageMaxFileSize;
    private String sMountPoint;
    private long mMountMaxFileSize;
    private boolean mIsThirdParty = false;
    private StorageSpaceStatusListener mSpaceListener;
    private int mStorageStyle;

    private StorageDirManager(Context ct) {
        mContext = ct.getApplicationContext();
        mStorageManager = (StorageManager) mContext.getSystemService(Context.STORAGE_SERVICE);//new StorageManager();

        mStoragePath = new String[SDM_NUM_STORAGE_INDEX];
        mStorageMaxFileSize = new long[SDM_NUM_STORAGE_INDEX];

        getVolumeState = ApiHelper.getMethod(STORAGEMANAGER, "getVolumeState", String.class);
        getVolumeList = ApiHelper.getMethod(STORAGEMANAGER, "getVolumeList");

        getPath = ApiHelper.getMethod(STORAGEVOLUME, "getPath");
        isRemovable = ApiHelper.getMethod(STORAGEVOLUME, "isRemovable");
        getMaxFileSize = ApiHelper.getMethod(STORAGEVOLUME, "getMaxFileSize");

        triggerDirProb();
    }

    public static StorageDirManager getInstance(Context context) {
        if (sInstance == null) {
            synchronized (StorageDirManager.class) {
                if (sInstance == null) {
                    sInstance = new StorageDirManager(context);
                }
            }
        }
        return sInstance;
    }

    public void SetStorageSpaceListener(StorageSpaceStatusListener listener) {
        mSpaceListener = listener;
    }

    public StorageSpaceStatusListener getStorageSpaceListerner() {
        return mSpaceListener;
    }

    public void setThirdPartyToken(boolean isthirparty) {
        mIsThirdParty = isthirparty;
    }

    public void triggerDirProb() {
        final long start = System.currentTimeMillis();
        Log.d(TAG, "triggerDirProb: start....");
        resetStoragePath();
        mStorageVolumes = (Object[]) ApiHelper.invokeMethod(getVolumeList, mStorageManager);
        Log.d(TAG, "triggerDirProb: mStorageVolumes.length = " + mStorageVolumes.length);
        for (Object storageVolume : mStorageVolumes) {
            String tmpPath = (String) ApiHelper.invokeMethod(getPath, storageVolume);
            Log.d(TAG, "triggerDirProb: tmpPath = " + tmpPath);
            if (tmpPath != null) {
                long maxfilesize = (long) ApiHelper.invokeMethod(getMaxFileSize, storageVolume);
                Log.v(TAG, "maxfilesize=" + maxfilesize + "  tmpPath=" + tmpPath);
                String status = getStorageState(tmpPath);
                Log.i(TAG, "the probed dir " + tmpPath + " is under " + status + " status");
                if (status.equalsIgnoreCase(Environment.MEDIA_MOUNTED)) {
                    Boolean isRemove = (Boolean) ApiHelper.invokeMethod(isRemovable, storageVolume);
                    if (isRemove) {
                        if (tmpPath.contains("otg")) {
                            mStoragePath[SDM_USBOTG_STORAE_INDEX] = tmpPath;
                            mStorageMaxFileSize[SDM_USBOTG_STORAE_INDEX] = maxfilesize;
                        } else {//  sd
                            mStoragePath[SDM_SDCARD_STORAGE_INDEX] = tmpPath;
                            mStorageMaxFileSize[SDM_SDCARD_STORAGE_INDEX] = maxfilesize;
                        }

                    } else {// internal
                        mStoragePath[SDM_INTERANL_STORAGE_INDEX] = tmpPath;
                        mStorageMaxFileSize[SDM_INTERANL_STORAGE_INDEX] = maxfilesize;
                    }
                }
            }

        }
        if (mSpaceListener != null) {
            mSpaceListener.onCurrentAvailableStorageDirList(mStoragePath);
        }
        Log.d(TAG, "triggerDirProb: end.... cost time = " + (System.currentTimeMillis() - start) + "ms");
    }

    public String[] getStoragePath() {
        return mStoragePath;
    }

    private int setMountPoint(int index) {
        if (checkAvailableSpace(mStoragePath[index])) {
            sMountPoint = mStoragePath[index];
            mMountMaxFileSize = mStorageMaxFileSize[index];
            return 0;
        } else {
            sMountPoint = mStoragePath[index];
            mMountMaxFileSize = mStorageMaxFileSize[index];
            return STORAGE_NOT_ENOUGH;
        }
    }

    public long getMaxFileSize() {
        return mMountMaxFileSize;
    }

    public int updateMountPoint() {
        int result = 0;
        String currentplace = null;

        if (currentplace == null) {
            currentplace = "internal";
        } else {
            switch (currentplace) {
                case "phone":
                    currentplace = "internal";
                    break;
                case "T card":
                    currentplace = "sdcard";
                    break;
                case "usbotg":
                    currentplace = "usbotg";
                    break;
            }
        }

        switch (StoragePos.getStoragePos(currentplace)) {
            case internal:
                if (mStoragePath[SDM_INTERANL_STORAGE_INDEX] != null) {
                    boolean hasInternalSpace = checkAvailableSpace(SDM_INTERANL_STORAGE_INDEX);
                    if (!hasInternalSpace && mStoragePath[SDM_SDCARD_STORAGE_INDEX] != null
                            && checkAvailableSpace(SDM_SDCARD_STORAGE_INDEX)) {
                        mStorageStyle = ToastStyle.STYLE_INTERNAL_TO_SDCARD_TOAST;
                        result = setMountPoint(SDM_SDCARD_STORAGE_INDEX);
                    } else {
                        result = setMountPoint(SDM_INTERANL_STORAGE_INDEX);
                    }
                } else {
                    result = setMountPoint(SDM_SDCARD_STORAGE_INDEX);
                }
                break;
            case sdcard:
                if (mStoragePath[SDM_SDCARD_STORAGE_INDEX] != null) {
                    boolean hasSdcardSpace = checkAvailableSpace(SDM_SDCARD_STORAGE_INDEX);
                    if (!hasSdcardSpace && mStoragePath[SDM_INTERANL_STORAGE_INDEX] != null
                            && checkAvailableSpace(SDM_INTERANL_STORAGE_INDEX)) {
                        mStorageStyle = ToastStyle.STYLE_SDCARD_TO_INTERNAL_TOAST;
                        result = setMountPoint(SDM_INTERANL_STORAGE_INDEX);
                    } else {
                        result = setMountPoint(SDM_SDCARD_STORAGE_INDEX);
                    }
                } else {
                    result = setMountPoint(SDM_INTERANL_STORAGE_INDEX);
                }
                break;
            case usbotg:
                if (mStoragePath[SDM_USBOTG_STORAE_INDEX] != null) {
                    result = setMountPoint(SDM_USBOTG_STORAE_INDEX);
                } else {
                    result = setMountPoint(SDM_INTERANL_STORAGE_INDEX);
                }
                break;

            default:
                if (mStoragePath[SDM_INTERANL_STORAGE_INDEX] != null) {
                    result = setMountPoint(SDM_INTERANL_STORAGE_INDEX);
                }
                break;
        }

        StorageTools.setFilePath(getFileDirectory());
        return result;
    }

    public String[] getStorageDirList() {
        return mStoragePath;
    }

    public String getStorageDir(int index) {
        if (index < 0 || index >= SDM_NUM_STORAGE_INDEX) {
            return null;
        } else {
            return mStoragePath[index];
        }
    }

    public String getMountPoint() {
        return sMountPoint;
    }

    private void resetStoragePath() {
        if (mStoragePath != null) {
            for (int i = 0; i < SDM_NUM_STORAGE_INDEX; i++) {
                mStoragePath[i] = null;
            }
        }
    }

    private String getStorageState(String path) {
        try {
            //if(getVolumeState != null)
            return (String) ApiHelper.invokeMethod(getVolumeState, mStorageManager, path);
            //return mStorageManager.getVolumeState(path);
        } catch (Exception e) {
            Log.w(TAG, "Failed to read SDCard storage state; assuming REMOVED: " + e);
            return Environment.MEDIA_REMOVED;
        }
    }

    public boolean isWriteable(String path) {
        final String state = getStorageState(path);
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    public boolean isSdCardWriteable() {
        return mStoragePath[SDM_SDCARD_STORAGE_INDEX] != null && isWriteable(mStoragePath[SDM_SDCARD_STORAGE_INDEX]);
    }

    public String getFileDirectory() {
        String path = sMountPoint + FOLDER_PATH;
        Log.d(TAG, "getFilePath return " + path);
        return path;
    }

    public String getRawFileDirectory() {
        String path = sMountPoint + RAW_FOLDER_PATH;
        File rawDir = new File(path);
        if (!rawDir.exists()) rawDir.mkdir();
        Log.d(TAG, "getFilePath return " + path);
        return path;
    }

    public void mkFileDir(String path) {

        //StorageTools.createFolder(path);

        File dir = new File(path);
        if (dir != null) {
            if (!dir.exists()) {
                Log.d(TAG, "dir not exit,will create this");
                dir.mkdirs();
            }
        }
    }

    public boolean checkAvailableSpace(String path) {
        //if(mIsThirdParty) return true;

        String checkpath = path != null ? path : getMountPoint();
        long internalleft = StorageTools.getDirSpace(checkpath);
        if (internalleft < LOW_STORAGE_THRESHOLD
                || Math.abs(internalleft - LOW_STORAGE_THRESHOLD) <= 10000000) {
            mStorageFull = true;
            /*boolean showToast = true;
            if(checkpath == null && path == null) {
				showToast = false;
			}*/
            int style = ToastStyle.STYLE_FULL_TOAST;
            if (checkpath == null && path == null) {
                style = ToastStyle.STYLE_NO_TOAST;
            }
            getStorageSpaceListerner().onStorageSpaceStatus(true, style);
            Log.i(TAG, "checkAvailableSpace the path is" + path + " the checkpath is " + checkpath + " the internalleft is " + internalleft);
            return false;
        } else {
            mStorageFull = false;
            int style = mStorageStyle;
            if (path == null || mIsThirdParty) {
                style = ToastStyle.STYLE_NO_TOAST;
            }
            getStorageSpaceListerner().onStorageSpaceStatus(false, style);
            mStorageStyle = ToastStyle.STYLE_NO_TOAST;
            return true;
        }
    }

    private boolean checkAvailableSpace(int index) {
        //if(mIsThirdParty) return true;

        final String checkpath = mStoragePath[index];
        final long internalleft = StorageTools.getDirSpace(checkpath);
        if (internalleft < LOW_STORAGE_THRESHOLD
                || Math.abs(internalleft - LOW_STORAGE_THRESHOLD) <= 10000000) {
            Log.i(TAG, "checkAvailableSpace the checkpath is " + checkpath + " the internalleft is " + internalleft);
            return false;
        } else {
            return true;
        }
    }

    private enum StoragePos {
        internal, sdcard, usbotg;

        public static StoragePos getStoragePos(String place) {
            return valueOf(place.toLowerCase());
        }
    }

    public interface StorageSpaceStatusListener {
        void onStorageSpaceStatus(boolean full, int style);

        public void onCurrentAvailableStorageDirList(String[] dirs);
    }

    public static class ToastStyle {
        public static final int STYLE_NO_TOAST = 0;
        public static final int STYLE_FULL_TOAST = 1;
        public static final int STYLE_SDCARD_TO_INTERNAL_TOAST = 2;
        public static final int STYLE_INTERNAL_TO_SDCARD_TOAST = 3;
    }
}
