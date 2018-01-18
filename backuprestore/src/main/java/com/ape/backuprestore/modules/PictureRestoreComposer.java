package com.ape.backuprestore.modules;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.provider.MediaStore;

import com.ape.backuprestore.utils.BackupZip;
import com.ape.backuprestore.utils.Constants;
import com.ape.backuprestore.utils.FileUtils;
import com.ape.backuprestore.utils.Logger;
import com.ape.backuprestore.utils.ModuleType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by android on 16-7-16.
 */
public class PictureRestoreComposer extends Composer {
    private static final String TAG ="PictureRestoreComposer";
    private static final String[] PROJECTION = new String[]{
            MediaStore.Images.Media._ID, MediaStore.Images.Media.DATA};
    private int mIndex;
    private final Object mLock = new Object();
    private File[] mFileList;
    private ArrayList<String> mFileNameList;
    private String mDestPath;
    private String mZipFileName;
    private boolean mImport;
    private boolean mIsMediaScannerConnected;


    /**
     * Creates a new <code>PictureRestoreComposer</code> instance.
     *
     * @param context a <code>Context</code> value
     */
    public PictureRestoreComposer(Context context) {
        super(context);
    }

    public int getModuleType() {
        return ModuleType.TYPE_PICTURE;
    }

    public int getCount() {
        int count = 0;
        if (mFileNameList != null) {
            count = mFileNameList.size();
        }
        if (count == 0 && mFileList != null) {
            count = mFileList.length;
        }
        Logger.d(TAG, "getCount():" + count);
        return count;
    }

    public boolean init() {
        boolean result = false;
        String path = mParentFolderPath + File.separator + Constants.ModulePath.FOLDER_PICTURE;
        mFileNameList = new ArrayList<>();
        File folder = new File(path);
        if (folder.exists() && folder.isDirectory()) {
            try {
                mZipFileName = path + File.separator + Constants.ModulePath.NAME_PICTUREZIP;
                File file = new File(mZipFileName);
                if (file.exists()) {
                    mFileNameList = (ArrayList<String>) BackupZip.getFileList(mZipFileName, true,
                            true, ".*");
                    String tmppath = (new File(mParentFolderPath)).getParent();
                    if (tmppath != null) {
                        mDestPath = tmppath + File.separator
                                + RESTORE + File.separator
                                + Constants.ModulePath.FOLDER_PICTURE;
                        Logger.d(TAG, "mDestPath:" + mDestPath);
                        result = true;
                    }
                } else {
                    // for old datas
                    mFileList = folder.listFiles();
                    result = true;
                }
            } catch (IOException e) {
                e.printStackTrace();
                if (super.mReporter != null) {
                    super.mReporter.onErr(e);
                }
            } catch (StringIndexOutOfBoundsException e) {
                e.printStackTrace();
            }
        }

        Logger.d(TAG, "init():" + result + ",count:" + getCount());
        return result;
    }

    public boolean isAfterLast() {
        boolean result = true;
        if (mFileNameList != null && mFileNameList.size() > 0) {
            result = (mIndex >= mFileNameList.size());
        } else if (mFileList != null) {
            // for old data
            result = (mIndex >= mFileList.length);
        }

        Logger.d(TAG, "isAfterLast():" + result);
        return result;
    }

    public boolean implementComposeOneEntity() {
        boolean result = false;
        if (mDestPath == null) {
            // for old data
            if (mFileList != null) {
                mIndex++;
                result = true;
            }
            return result;
        }
        synchronized (mLock) {
            while (!mIsMediaScannerConnected) {
                try {
                    mLock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        if (mFileNameList != null && mIndex < mFileNameList.size()) {
            String picName = mFileNameList.get(mIndex++);
            String destFileName = mDestPath + picName;
            if (mImport) {
                File fileName = new File(destFileName);
                if (fileName.exists()) {
                    destFileName = rename(destFileName);
                }
            }
            try {
                BackupZip.unZipFile(mZipFileName, picName, destFileName);
                Logger.d(TAG, " insert database mDestFileName ="
                        + destFileName);
                FileUtils.scanPathforMediaStore(destFileName, mContext);
                result = true;
            } catch (IOException e) {
                if (super.mReporter != null) {
                    super.mReporter.onErr(e);
                }
                result = false;
                e.printStackTrace();
            }
        }

        Logger.d(TAG, "implementComposeOneEntity:" + result);
        return result;
    }

    private void deleteFolder(File file) {
        if (file.exists()) {
            if (file.isFile()) {
                try {
                    int count = mContext.getContentResolver().delete(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            MediaStore.Images.Media.DATA + " like ?",
                            new String[]{file.getAbsolutePath()});
                    Logger.d(TAG, "deleteFolder():" + count + ":"
                            + file.getAbsolutePath());
                    file.delete();
                } catch (NullPointerException e) {
                    Logger.d(TAG, "deleteFolder: exception");
                }

            } else if (file.isDirectory()) {
                File files[] = file.listFiles();
                if (files != null) {
                    for (File f : files) {
                        this.deleteFolder(f);
                    }
                }
            }
            file.delete();
        }
    }

    /**
     * Describe <code>onStart</code> method here.
     */
    public void onStart() {
        super.onStart();
        MediaScannerConnection mediaScannerConnection = new MediaScannerConnection(this.mContext,
                new ScanCompletedListener());
        mediaScannerConnection.connect();
        // && super.checkedCommand())
        if (mDestPath != null) {
            File tmpFolder = new File(mDestPath);
            deleteFolder(tmpFolder);

            if (!tmpFolder.exists()) {
                tmpFolder.mkdirs();
            }
        } else {
            mImport = true;
        }

        Logger.d(TAG, "onStart()");
    }

    private String rename(String name) {
        String tmpName = name.subSequence(name.lastIndexOf(File.separator) + 1,
                name.length()).toString();
        String path = name.subSequence(0, name.lastIndexOf(File.separator) + 1)
                .toString();
        Logger.d(TAG, " rename:tmpName  ==== " + tmpName);
        String rename = null;
        File tmpFile = null;
        int id = tmpName.lastIndexOf(".");
        int id2;
        int leftLen;
        for (int i = 1; i < (1 << 12); ++i) {
            leftLen = 255 - (1 + Integer.toString(i).length()
                    + tmpName.length() - id);
            id2 = id <= leftLen ? id : leftLen;
            rename = tmpName.subSequence(0, id2) + "~" + i
                    + tmpName.subSequence(id, tmpName.length());
            tmpFile = new File(path + rename);
            String tmpFileName = tmpFile.getAbsolutePath();
            Logger.d(TAG, " rename:tmpFileName == " + tmpFileName);
            if (!tmpFile.exists()) {
                Logger.d(TAG, " rename: rename === " + rename);
                break;
            }
        }
        return path + rename;
    }

    /**
     * Describe class <code>ScanCompletedListener</code> here.
     *
     * @author way
     */
    private class ScanCompletedListener implements MediaScannerConnection.MediaScannerConnectionClient {
        @Override
        public void onScanCompleted(String path, Uri uri) {
            Logger.d(TAG, mIndex + "nScanCompleted");
            if (uri != null) {
                Logger.d(TAG, mIndex + path + "insert to db successed!");
            } else {
                Logger.d(TAG, mIndex + path + "insert to db fail");
            }
        }

        @Override
        public void onMediaScannerConnected() {
            synchronized (mLock) {
                mIsMediaScannerConnected = true;
                Logger.d(TAG, "MediaScannerConnected");
                mLock.notifyAll();
            }
        }
    }
}
