package com.ape.backuprestore.modules;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.provider.CallLog;
import android.text.TextUtils;

import com.ape.backuprestore.utils.Constants;
import com.ape.backuprestore.utils.Logger;
import com.ape.backuprestore.utils.ModuleType;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

/**
 * Created by android on 16-7-20.
 */
public class CallLogRestoreComposer extends Composer {
    private static final String TAG = "CallLogRestoreComposer";
    private static final Uri CALL_LOG_URI = CallLog.Calls.CONTENT_URI;
    private int mIdx;
    private ArrayList<CallLogXmlInfo> mRecordList;

    public CallLogRestoreComposer(Context context) {
        super(context);
    }

    @Override
    public int getModuleType() {
        return ModuleType.TYPE_CALL_LOG;
    }

    @Override
    public int getCount() {
        int count = 0;
        if (mRecordList != null) {
            count = mRecordList.size();
        }
        Logger.d(TAG, "getCount():" + count);
        return count;
    }

    @Override
    public boolean isAfterLast() {
        boolean result = true;

        if (mRecordList != null) {
            result = mIdx >= mRecordList.size();
        }

        Logger.d(TAG, "isAfterLast():" + result);
        return result;
    }

    @Override
    public boolean init() {
        boolean result = false;
        String content = readFileContent(mParentFolderPath + File.separator
                + Constants.ModulePath.FOLDER_CALL_LOG + File.separator + Constants.ModulePath.CALL_LOG_XML);
        if (!TextUtils.isEmpty(content)) {
            mRecordList = CallLogXmlParser.parse(content);
        } else {
            mRecordList = new ArrayList<>();
        }
        result = true;

        Logger.d(TAG, "init():" + result + ",count:" + mRecordList.size());
        return result;
    }

    @Override
    protected boolean implementComposeOneEntity() {
        boolean result = false;
        if (!isAfterLast()) {
            CallLogXmlInfo record = mRecordList.get(mIdx++);
            ContentValues v = new ContentValues();
            v.put(NoteBookXmlInfo.TITLE, record.getTitle());
            v.put(NoteBookXmlInfo.NOTE, record.getNote());
            v.put(NoteBookXmlInfo.CREATED, record.getCreated());
            v.put(NoteBookXmlInfo.MODIFIED, record.getModified());
            v.put(NoteBookXmlInfo.NOTEGROUP, record.getNoteGroup());

            try {
                Uri tmpUri = mContext.getContentResolver().insert(CALL_LOG_URI, v);
                Logger.d(TAG, "tmpUri:" + tmpUri);
                result = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        Logger.d(TAG, Logger.NOTEBOOK_TAG + "implementComposeOneEntity():" + result);
        return result;
    }

    /**
     * Describe <code>deleteAllNoteBook</code> method here.
     */
    private void deleteAllNoteBook() {
        mContext.getContentResolver().delete(CALL_LOG_URI, null, null);
    }

    @Override
    public void onStart() {
        super.onStart();
        deleteAllNoteBook();
    }

    private String readFileContent(String fileName) {
        try {
            InputStream is = new FileInputStream(fileName);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int len = -1;
            byte[] buffer = new byte[512];
            while ((len = is.read(buffer, 0, 512)) != -1) {
                baos.write(buffer, 0, len);
            }

            is.close();
            return baos.toString();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
