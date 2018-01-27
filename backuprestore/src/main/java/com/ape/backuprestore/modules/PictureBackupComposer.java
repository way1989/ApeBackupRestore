package com.ape.backuprestore.modules;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import com.ape.backuprestore.utils.BackupZip;
import com.ape.backuprestore.utils.Constants;
import com.ape.backuprestore.utils.Logger;
import com.ape.backuprestore.utils.ModuleType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by android on 16-7-16.
 */
public class PictureBackupComposer extends Composer {
    private static final String TAG = "PictureBackupComposer";
    private static final String[] PICTURE_PROJECTION = new String[]{MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATA};
    private static final Uri PICTURE_URI = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
    private Cursor mPictureCursor;

    private ArrayList<String> mFileNameList = null;
    private BackupZip mZipFileHandler;

    public PictureBackupComposer(Context context) {
        super(context);
    }

    @Override
    public int getModuleType() {
        return ModuleType.TYPE_PICTURE;
    }

    @Override
    public int getCount() {
        int count = 0;
        if (mPictureCursor != null && !mPictureCursor.isClosed() && mPictureCursor.getCount() > 0) {
            count = mPictureCursor.getCount();
        }
        Logger.d(TAG, "getCount():" + count);
        return count;
    }

    @Override
    public boolean isAfterLast() {
        boolean result = true;
        if (mPictureCursor != null && !mPictureCursor.isAfterLast()) {
            result = false;
        }

        Logger.d(TAG, "isAfterLast():" + result);
        return result;
    }

    @Override
    public boolean init() {
        boolean result = false;
        mPictureCursor = mContext.getContentResolver().query(PICTURE_URI, PICTURE_PROJECTION,
                null, null, null);

        if (mPictureCursor != null) {
            result = mPictureCursor.moveToFirst();
        }

        mFileNameList = new ArrayList<>();

        Logger.d(TAG, "init():" + result + ",count:" + getCount());
        return result;
    }

    @Override
    protected boolean implementComposeOneEntity() {
        boolean result = false;
        if (mPictureCursor != null && !mPictureCursor.isAfterLast()) {
            int dataColumn = mPictureCursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            String data = mPictureCursor.getString(dataColumn);

            String destinationFileName = null;
            try {
                String tmpName = mParentFolderPath + File.separator + Constants.ModulePath.FOLDER_PICTURE +
                        data.subSequence(data.lastIndexOf(File.separator), data.length()).toString();
                destinationFileName = getDestinationName(tmpName);
            } catch (StringIndexOutOfBoundsException e) {
                Logger.d(TAG, "data OutOfBoundsException:data" + data);
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (destinationFileName != null) {
                try {
                    mZipFileHandler.addFileByFileName(data, destinationFileName);
                    mFileNameList.add(destinationFileName);
                    result = true;
                } catch (IOException e) {
                    Logger.d(TAG, "copy file fail");
                    try {
                        Logger.e(TAG, "[implementComposeOneEntity] finish");
                        mZipFileHandler.finish();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                    if (super.mReporter != null) {
                        super.mReporter.onErr(e);
                    }
                }
            }
            Logger.d(TAG, "pic:" + data + ",destName:" + destinationFileName);
            mPictureCursor.moveToNext();
        }

        return result;
    }

    private String getDestinationName(String name) {
        if (!mFileNameList.contains(name)) {
            return name;
        } else {
            return rename(name);
        }
    }

    private String rename(String name) {
        String tmpName;
        int id = name.lastIndexOf(".");
        int id2, leftLen;
        for (int i = 1; i < (1 << 12); ++i) {
            leftLen = 255 - (1 + Integer.toString(i).length() + name.length() - id);
            id2 = id <= leftLen ? id : leftLen;
            tmpName = name.subSequence(0, id2) + "~" + i + name.subSequence(id, name.length());
            if (!mFileNameList.contains(tmpName)) {
                return tmpName;
            }
        }

        return null;
    }

    /**
     * Describe <code>onStart</code> method here.
     */
    public final void onStart() {
        super.onStart();
        if (getCount() > 0) {
            File path = new File(mParentFolderPath + File.separator + Constants.ModulePath.FOLDER_PICTURE);
            if (!path.exists()) {
                path.mkdirs();
            }
            try {
                mZipFileHandler = new BackupZip(path + File.separator + Constants.ModulePath.NAME_PICTUREZIP);
            } catch (IOException e) {
                if (super.mReporter != null) {
                    super.mReporter.onErr(e);
                }
                e.printStackTrace();
            }
        }

    }

    public void onEnd() {
        super.onEnd();
        if (mFileNameList != null && mFileNameList.size() > 0) {
            mFileNameList.clear();
        }

        if (mPictureCursor != null) {
            mPictureCursor.close();
        }
        if (mZipFileHandler != null) {
            try {
                mZipFileHandler.finish();
            } catch (IOException e) {
                e.printStackTrace();
                if (super.mReporter != null) {
                    super.mReporter.onErr(e);
                }
            } finally {
                mZipFileHandler = null;
            }
        }
    }

}
