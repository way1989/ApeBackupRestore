package com.ape.backuprestore.modules;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.ape.backuprestore.utils.Constants;
import com.ape.backuprestore.utils.Logger;
import com.ape.backuprestore.utils.ModuleType;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by android on 16-7-16.
 */
public class NoteBookBackupComposer extends Composer {
    private static final String CLASS_TAG = Logger.LOG_TAG + "/NoteBookBackupComposer";
    private static final String COLUMN_NAME_TITLE = "title";
    private static final String COLUMN_NAME_NOTE = "note";
    private static final String COLUMN_NAME_CREATED = "created";
    private static final String COLUMN_NAME_MODIFIED = "modified";
    private static final String COLUMN_NAME_GROUP = "notegroup";

    private Cursor mCursor;
    private NoteBookXmlComposer mXmlComposer;

    public NoteBookBackupComposer(Context context) {
        super(context);
    }

    /**
     * Describe <code>init</code> method here.
     *
     * @return a <code>boolean</code> value
     */
    public final boolean init() {
        boolean result = false;
        Uri uri = Uri.parse(Constants.URI_NOTEBOOK);
        mCursor = mContext.getContentResolver().query(uri, null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
            result = true;
        }

        Logger.d(CLASS_TAG,
                "init():" + result + ",count::" + (mCursor != null ? mCursor.getCount() : 0));
        return result;
    }

    /**
     * Describe <code>getModuleType</code> method here.
     *
     * @return an <code>int</code> value
     */
    public final int getModuleType() {
        return ModuleType.TYPE_NOTEBOOK;
    }

    /**
     * Describe <code>getCount</code> method here.
     *
     * @return an <code>int</code> value
     */
    public final int getCount() {
        int count = 0;
        if (mCursor != null && !mCursor.isClosed()) {
            count = mCursor.getCount();
        }

        Logger.d(CLASS_TAG, "getCount():" + count);
        return count;
    }

    /**
     * Describe <code>isAfterLast</code> method here.
     *
     * @return a <code>boolean</code> value
     */
    public final boolean isAfterLast() {
        boolean result = true;
        if (mCursor != null) {
            result = mCursor.isAfterLast();
        }

        Logger.d(CLASS_TAG, "isAfterLast():" + result);
        return result;
    }

    /**
     * Describe <code>implementComposeOneEntity</code> method here.
     *
     * @return a <code>boolean</code> value
     */
    public final boolean implementComposeOneEntity() {
        boolean result = false;
        if (mCursor != null && !mCursor.isAfterLast()) {
            if (mXmlComposer != null) {
                try {
                    String title = mCursor.getString(mCursor
                            .getColumnIndexOrThrow(COLUMN_NAME_TITLE));
                    String note = mCursor
                            .getString(mCursor.getColumnIndexOrThrow(COLUMN_NAME_NOTE));
                    String created = mCursor.getString(mCursor
                            .getColumnIndexOrThrow(COLUMN_NAME_CREATED));
                    String modified = mCursor.getString(mCursor
                            .getColumnIndexOrThrow(COLUMN_NAME_MODIFIED));
                    String notegroup = mCursor.getString(mCursor
                            .getColumnIndexOrThrow(COLUMN_NAME_GROUP));
                    NoteBookXmlInfo record = new NoteBookXmlInfo(title, note, created, modified,
                            notegroup);
                    mXmlComposer.addOneMmsRecord(record);
                    result = true;
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                }
            }

            mCursor.moveToNext();
        }

        return result;
    }

    /**
     * Describe <code>onStart</code> method here.
     */
    public void onStart() {
        super.onStart();
        if (getCount() > 0) {
            File path = new File(mParentFolderPath + File.separator + Constants.ModulePath.FOLDER_NOTEBOOK);
            if (path.exists()) {
                File[] files = path.listFiles();
                for (File file : files) {
                    if (file.isFile()) {
                        file.delete();
                    }
                }
            } else {
                path.mkdirs();
            }

            mXmlComposer = new NoteBookXmlComposer();
            mXmlComposer.startCompose();
        }
    }

    /**
     * Describe <code>onEnd</code> method here.
     */
    public void onEnd() {
        super.onEnd();
        if (mXmlComposer != null) {
            mXmlComposer.endCompose();
            String tmpXmlInfo = mXmlComposer.getXmlInfo();
            if (getComposed() > 0 && tmpXmlInfo != null) {
                try {
                    writeToFile(tmpXmlInfo);
                } catch (IOException e) {
                    if (super.mReporter != null) {
                        super.mReporter.onErr(e);
                    }
                    e.printStackTrace();
                }
            }
        }

        if (mCursor != null) {
            mCursor.close();
            mCursor = null;
        }
    }

    private void writeToFile(String inBuf) throws IOException {
        try {
            FileOutputStream outStream = new FileOutputStream(mParentFolderPath + File.separator
                    + Constants.ModulePath.FOLDER_NOTEBOOK + File.separator + Constants.ModulePath.NOTEBOOK_XML);
            byte[] buf = inBuf.getBytes();
            outStream.write(buf, 0, buf.length);
            outStream.flush();
            outStream.close();
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
