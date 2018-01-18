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
public class MusicBackupComposer extends Composer {
    private static final String TAG = "MusicBackupComposer";
    private static final Uri MUSIC_URI = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

    private static final String[] MUSIC_PROJECTION = new String[]{MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DATA};
    private ArrayList<String> mNameList;
    private Cursor mMusicCursor;
    private BackupZip mZipFileHandler;

    public MusicBackupComposer(Context context) {
        super(context);
    }

    @Override
    public int getModuleType() {
        return ModuleType.TYPE_MUSIC;
    }

    @Override
    public int getCount() {
        int count = 0;
        if (mMusicCursor != null && !mMusicCursor.isClosed() && mMusicCursor.getCount() > 0) {
            count = mMusicCursor.getCount();
        }

        Logger.d(TAG, "getCount():" + count);
        return count;
    }

    @Override
    public boolean isAfterLast() {
        boolean result = true;
        if (mMusicCursor != null && !mMusicCursor.isAfterLast()) {
            result = false;
        }
        Logger.d(TAG, "isAfterLast():" + result);
        return result;
    }

    @Override
    public boolean init() {
        boolean result = false;

        mMusicCursor = mContext.getContentResolver().query(MUSIC_URI,
                MUSIC_PROJECTION, null, null, null);
        if (mMusicCursor != null) {
            result = mMusicCursor.moveToFirst();
        }

        mNameList = new ArrayList<>();
        Logger.d(TAG, "init():" + result + ",count:" + getCount());
        return result;
    }

    @Override
    protected boolean implementComposeOneEntity() {
        boolean result = false;
        if (mMusicCursor != null && !mMusicCursor.isAfterLast()) {
            int dataColumn = mMusicCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
            String data = mMusicCursor.getString(dataColumn);
            String destFileName;
            try {
                String tmpName = mParentFolderPath + File.separator + Constants.ModulePath.FOLDER_MUSIC +
                        data.subSequence(data.lastIndexOf(File.separator), data.length()).toString();
                destFileName = getDestinationName(tmpName);
                if (destFileName != null) {
                    try {
                        mZipFileHandler.addFileByFileName(data, destFileName);
                        mNameList.add(destFileName);
                        result = true;
                    } catch (IOException e) {
                        Logger.d(TAG, Logger.MUSIC_TAG + "copy file fail");
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

                Logger.d(TAG, data + ",destFileName:" + destFileName);
            } catch (StringIndexOutOfBoundsException e) {
                Logger.e(TAG, Logger.MUSIC_TAG
                        + " StringIndexOutOfBoundsException");
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
            mMusicCursor.moveToNext();
        }
        return result;
    }


    /**
     * Describe <code>onStart</code> method here.
     */
    @Override
    public void onStart() {
        super.onStart();
        if (getCount() > 0) {
            File path = new File(mParentFolderPath + File.separator + Constants.ModulePath.FOLDER_MUSIC);
            if (!path.exists()) {
                path.mkdirs();
            }
            try {
                mZipFileHandler = new BackupZip(path + File.separator + Constants.ModulePath.NAME_MUSICZIP);
            } catch (IOException e) {
                if (super.mReporter != null) {
                    super.mReporter.onErr(e);
                }
                e.printStackTrace();
            }
        }

    }

    /**
     * Describe <code>onEnd</code> method here.
     */
    @Override
    public void onEnd() {
        super.onEnd();
        if (mNameList != null && mNameList.size() > 0) {
            mNameList.clear();
        }

        if (mMusicCursor != null) {
            mMusicCursor.close();
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


    /**
     * Describe <code>getDestinationName</code> method here.
     *
     * @param name a <code>String</code> value
     * @return a <code>String</code> value
     */
    private String getDestinationName(String name) {
        if (!mNameList.contains(name)) {
            return name;
        } else {
            return rename(name);
        }
    }

    /**
     * Describe <code>rename</code> method here.
     *
     * @param name a <code>String</code> value
     * @return a <code>String</code> value
     */
    private String rename(String name) {
        String tmpName;
        int id = name.lastIndexOf(".");
        int id2, leftLen;
        for (int i = 1; i < (1 << 12); ++i) {
            leftLen = 255 - (1 + Integer.toString(i).length() + name.length() - id);
            id2 = id <= leftLen ? id : leftLen;
            tmpName = name.subSequence(0, id2) + "~" + i + name.subSequence(id, name.length());
            if (!mNameList.contains(tmpName)) {
                return tmpName;
            }
        }

        return null;
    }

}
