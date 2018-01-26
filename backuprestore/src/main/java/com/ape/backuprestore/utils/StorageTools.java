package com.ape.backuprestore.utils;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.os.StatFs;
import android.provider.MediaStore;
import android.util.Log;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class StorageTools {
    private static final String TAG = "storageTools";
    private static int mLastNum = 0;
    private static String mFilePath = null;
    public static final long UNAVAILABLE = -1L;
    public static final long PREPARING = -2L;
    public static final long UNKNOWN_SIZE = -3L;
    public static final long UNMOUNT = -4L;
    public static final long READ_ONLY = -5L;


    public static long getPathAvailableSpace(String path) {
        long mSpaceStatus = UNAVAILABLE;
        long freeSpace = 0;
        if (null == path) {
            mSpaceStatus = UNMOUNT;
            return mSpaceStatus;
        }
        freeSpace = getDirSpace(path);
        return freeSpace;
    }

    public static void setFilePath(String path) {
        mFilePath = path;
    }

    public static long getDirSpace(String strDir) {
        String storageDirectory = strDir;
        if (storageDirectory != null) {
            try {
                StatFs stat = new StatFs(storageDirectory);
                long blocs = stat.getAvailableBlocks();
                long size = stat.getBlockSize();
                return blocs * size;
            } catch (Exception ex) {
                Log.e(TAG, ">>>>statfs is failed");
                ex.printStackTrace();
                return 0;
            }
        } else {
            Log.v(TAG, ">>>> sdcard fail: storageDirectory = null");
            return 0;
        }
    }


    public static String getBucketId(String directory) {
        return String.valueOf(directory.toLowerCase(Locale.ENGLISH).hashCode());
    }

    public static String getBucketId() {
        return getBucketId(mFilePath);
    }

    private static boolean isMountPointValid(Uri uri, ContentResolver resolver) {
        String path = "";
        Cursor cursor = resolver.query(uri,
                new String[]{MediaStore.MediaColumns.DATA},
                null, null, null);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    path = cursor.getString(0);
                }
            } finally {
                cursor.close();
            }
        }

        File file = new File(path);
        String parent = file.getParent();
        boolean valid = mFilePath.equals(parent);

        return valid;
    }

    public static boolean isUriValid(Uri uri, ContentResolver resolver) {
        if (uri == null) {
            return false;
        }

        try {
            ParcelFileDescriptor pfd = resolver.openFileDescriptor(uri, "r");
            if (pfd == null) {
                Log.e(TAG, "Fail to open URI. URI=" + uri);
                return false;
            }
            pfd.close();
        } catch (IOException ex) {
            return false;
        }
        return isMountPointValid(uri, resolver);
    }

    public static void closeSilently(Closeable c) {
        if (c == null) {
            return;
        }
        try {
            c.close();
        } catch (Throwable t) {
            // do nothing
            t.printStackTrace();
        }
    }

    public static class ImageFileNamer {
        private SimpleDateFormat mFormat;

        // The date (in milliseconds) used to generate the last name.
        private long mLastDate;

        // Number of names generated for the same second.
        private int mSameSecondCount;

        public ImageFileNamer(String format) {
            mFormat = new SimpleDateFormat(format, Locale.ENGLISH);
        }

        public String generateName(long dateTaken) {
            Date date = new Date(dateTaken);
            String result = mFormat.format(date);

            // If the last name was generated for the same second,
            // we append _1, _2, etc to the name.
            if (dateTaken / 1000 == mLastDate / 1000) {
                mSameSecondCount++;
                result += "_" + mSameSecondCount;
            } else {
                mLastDate = dateTaken;
                mSameSecondCount = 0;
            }

            return result;
        }

        //for continuous shot file name
        public String generateContinuousName(long burstId, int count) {
            String index = String.format("%05d", count);
            //file name like this 00000IMG_00000_BURST20171221020932
            String name = index + "IMG_" + index + "_BURST" + burstId;
            if (count == 1) {
                name = name + "_COVER";
            }
            return name;
        }
    }
}

