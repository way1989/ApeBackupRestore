package com.ape.backuprestore.modules;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CallLog;

import com.ape.backuprestore.utils.Constants;
import com.ape.backuprestore.utils.ModuleType;
import com.ape.backuprestore.utils.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by android on 16-7-20.
 */
public class CallLogBackupComposer extends Composer {
    static final String[] CALL_LOG_PROJECTION = new String[]{
            CallLog.Calls.DATE,
            CallLog.Calls.DURATION,
            CallLog.Calls.NUMBER,
            CallLog.Calls.TYPE,
            CallLog.Calls.COUNTRY_ISO,
            CallLog.Calls.GEOCODED_LOCATION,
            CallLog.Calls.NUMBER_PRESENTATION,
            CallLog.Calls.PHONE_ACCOUNT_COMPONENT_NAME,
            CallLog.Calls.PHONE_ACCOUNT_ID,
            CallLog.Calls.FEATURES,
            CallLog.Calls.DATA_USAGE,
            CallLog.Calls.TRANSCRIPTION,
    };
    static final int DATE_COLUMN_INDEX = 0;
    static final int DURATION_COLUMN_INDEX = 1;
    static final int NUMBER_COLUMN_INDEX = 2;
    static final int CALL_TYPE_COLUMN_INDEX = 3;
    static final int COUNTRY_ISO_COLUMN_INDEX = 4;
    static final int GEOCODED_LOCATION_COLUMN_INDEX = 5;
    static final int NUMBER_PRESENTATION_COLUMN_INDEX = 6;
    static final int ACCOUNT_COMPONENT_NAME = 7;
    static final int ACCOUNT_ID = 8;
    static final int FEATURES = 9;
    static final int DATA_USAGE = 10;
    static final int TRANSCRIPTION_COLUMN_INDEX = 11;
    private static final String TAG = "CallLogBackupComposer";
    private static final Uri CALL_LOG_URI = CallLog.Calls.CONTENT_URI;
    private Cursor mCursor;
    private CallLogXmlComposer mXmlComposer;

    public CallLogBackupComposer(Context context) {
        super(context);
    }

    @Override
    public int getModuleType() {
        return ModuleType.TYPE_CALL_LOG;
    }

    @Override
    public int getCount() {
        int count = 0;
        if (mCursor != null && !mCursor.isClosed()) {
            count = mCursor.getCount();
        }

        Logger.d(TAG, "getCount():" + count);
        return count;
    }

    @Override
    public boolean isAfterLast() {
        boolean result = true;
        if (mCursor != null) {
            result = mCursor.isAfterLast();
        }

        Logger.d(TAG, "isAfterLast():" + result);
        return result;
    }

    @Override
    public boolean init() {
        boolean result = false;
        mCursor = mContext.getContentResolver().query(CALL_LOG_URI, null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
            result = true;
        }

        Logger.d(TAG, "init():" + result + ",count::" + (mCursor != null ? mCursor.getCount() : 0));
        return result;
    }

    @Override
    protected boolean implementComposeOneEntity() {
        boolean result = false;
        if (mCursor != null && !mCursor.isAfterLast()) {
            if (mXmlComposer != null) {
                try {
//                    String title = mCursor.getString(mCursor
//                            .getColumnIndexOrThrow(COLUMN_NAME_TITLE));
//                    String note = mCursor
//                            .getString(mCursor.getColumnIndexOrThrow(COLUMN_NAME_NOTE));
//                    String created = mCursor.getString(mCursor
//                            .getColumnIndexOrThrow(COLUMN_NAME_CREATED));
//                    String modified = mCursor.getString(mCursor
//                            .getColumnIndexOrThrow(COLUMN_NAME_MODIFIED));
//                    String notegroup = mCursor.getString(mCursor
//                            .getColumnIndexOrThrow(COLUMN_NAME_GROUP));
//                    CallLogXmlInfo record = new CallLogXmlInfo(title, note, created, modified,
//                            notegroup);
//                    mXmlComposer.addOneMmsRecord(record);
                    result = true;
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                }
            }

            mCursor.moveToNext();
        }

        return result;
    }

    @Override
    public final void onStart() {
        super.onStart();
        if (getCount() > 0) {
            File path = new File(mParentFolderPath + File.separator + Constants.ModulePath.FOLDER_CALL_LOG);
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

            mXmlComposer = new CallLogXmlComposer();
            mXmlComposer.startCompose();
        }
    }

    /**
     * Describe <code>onEnd</code> method here.
     */
    @Override
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
                    + Constants.ModulePath.FOLDER_CALL_LOG + File.separator + Constants.ModulePath.CALL_LOG_XML);
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
