package com.ape.backuprestore.modules;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
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
 * Created by android on 16-7-16.
 */
public class NoteBookRestoreComposer extends Composer {
    private static final String CLASS_TAG = Logger.LOG_TAG + "/NoteBookBackupComposer";
    private Uri mUri = Uri.parse(Constants.URI_NOTEBOOK);
    private int mIdx;
    private ArrayList<NoteBookXmlInfo> mRecordList;


    /**
     * Creates a new <code>NoteBookRestoreComposer</code> instance.
     *
     * @param context a <code>Context</code> value
     */
    public NoteBookRestoreComposer(Context context) {
        super(context);
    }

    /**
     * Describe <code>getCount</code> method here.
     *
     * @return an <code>int</code> value
     */
    public int getCount() {
        int count = 0;
        if (mRecordList != null) {
            count = mRecordList.size();
        }
        Logger.d(CLASS_TAG, "getCount():" + count);
        return count;
    }

    /**
     * Describe <code>getModuleType</code> method here.
     *
     * @return an <code>int</code> value
     */
    public int getModuleType() {
        return ModuleType.TYPE_NOTEBOOK;
    }

    /**
     * Describe <code>init</code> method here.
     *
     * @return a <code>boolean</code> value
     */
    public boolean init() {
        boolean result = false;
        String content = readFileContent(mParentFolderPath + File.separator
                + Constants.ModulePath.FOLDER_NOTEBOOK + File.separator + Constants.ModulePath.NOTEBOOK_XML);
        if (!TextUtils.isEmpty(content)) {
            mRecordList = NoteBookXmlParser.parse(content);
        } else {
            mRecordList = new ArrayList<>();
        }
        result = true;

        Logger.d(CLASS_TAG, "init():" + result + ",count:" + mRecordList.size());
        return result;
    }

    /**
     * Describe <code>isAfterLast</code> method here.
     *
     * @return a <code>boolean</code> value
     */
    public boolean isAfterLast() {
        boolean result = true;

        if (mRecordList != null) {
            result = mIdx >= mRecordList.size();
        }

        Logger.d(CLASS_TAG, "isAfterLast():" + result);
        return result;
    }

    /**
     * Describe <code>implementComposeOneEntity</code> method here.
     *
     * @return a <code>boolean</code> value
     */
    protected boolean implementComposeOneEntity() {
        boolean result = false;
        if (!isAfterLast()) {
            NoteBookXmlInfo record = mRecordList.get(mIdx++);
            ContentValues v = new ContentValues();
            v.put(NoteBookXmlInfo.TITLE, record.getTitle());
            v.put(NoteBookXmlInfo.NOTE, record.getNote());
            v.put(NoteBookXmlInfo.CREATED, record.getCreated());
            v.put(NoteBookXmlInfo.MODIFIED, record.getModified());
            v.put(NoteBookXmlInfo.NOTEGROUP, record.getNoteGroup());

            try {
                Uri tmpUri = mContext.getContentResolver().insert(mUri, v);
                Logger.d(CLASS_TAG, "tmpUri:" + tmpUri);
                result = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        Logger.d(CLASS_TAG, Logger.NOTEBOOK_TAG + "implementComposeOneEntity():" + result);
        return result;
    }

    /**
     * Describe <code>deleteAllNoteBook</code> method here.
     */
    private void deleteAllNoteBook() {
        mContext.getContentResolver().delete(mUri, null, null);
    }

    /**
     * Describe <code>onStart</code> method here.
     */
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
