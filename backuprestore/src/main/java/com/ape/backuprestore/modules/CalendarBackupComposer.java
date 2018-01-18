package com.ape.backuprestore.modules;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;

import com.ape.backuprestore.utils.Constants;
import com.ape.backuprestore.utils.Logger;
import com.ape.backuprestore.utils.ModuleType;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by android on 16-7-16.
 */
public class CalendarBackupComposer extends Composer {
    private static final String CLASS_TAG = Logger.LOG_TAG + "/CalendarBackupComposer";
    private static final Uri CALANDER_EVENT_URI = CalendarContract.Events.CONTENT_URI;
    private BufferedWriter mOut;
    private Cursor mCursor;
    private ArrayList<Integer> mFailEvents;

    public CalendarBackupComposer(Context context) {
        super(context);
    }

    @Override
    public int getModuleType() {
        return ModuleType.TYPE_CALENDAR;
    }

    @Override
    public int getCount() {
        int count = 0;
        if (mCursor != null && !mCursor.isClosed()) {
            count = mCursor.getCount();
            if (mFailEvents != null) {
                count = count - mFailEvents.size();
            }
        }

        Logger.d(CLASS_TAG, "getCount():" + count);
        return count;
    }

    @Override
    public boolean isAfterLast() {
        boolean result = true;
        if (mCursor != null) {
            result = mCursor.isAfterLast();
        }

        Logger.d(CLASS_TAG, "isAfterLast():" + result);
        return result;
    }

    @Override
    public boolean init() {
        boolean result = true;
        mFailEvents = new ArrayList<>();
        mCursor = mContext.getContentResolver().query(CALANDER_EVENT_URI, null, null, null, null);

        if (mCursor != null) {
            mCursor.moveToFirst();
            Logger.d(CLASS_TAG, "init() begin");
            for (int i = 0; i < mCursor.getCount(); i++) {
                int id = 0;
                try {
                    id = mCursor.getInt(mCursor.getColumnIndex("_id"));
                    Logger.d(CLASS_TAG, "init() id = " + id);

                    mCursor.moveToNext();
                } catch (Exception e) {
                    mFailEvents.add(id);
                    Logger.d(CLASS_TAG, "VCAL: init() fail");
                }
            }
            mCursor.moveToFirst();
        } else {
            result = false;
        }

        Logger.d(CLASS_TAG, "init(),result:" + result + ", count:"
                + (mCursor != null ? mCursor.getCount() : 0));
        return result;
    }

    @Override
    protected boolean implementComposeOneEntity() {
        boolean result = false;

        if (mCursor != null && !mCursor.isAfterLast()) {
            int id = mCursor.getInt(mCursor.getColumnIndex("_id"));
            Logger.d(CLASS_TAG, "implementComposeOneEntity id:" + id);
            mCursor.moveToNext();

        }

        return result;
    }

    public final void onStart() {
        super.onStart();
        if (getCount() > 0) {
            File path = new File(mParentFolderPath + File.separator
                    + Constants.ModulePath.FOLDER_CALENDAR);
            if (!path.exists()) {
                path.mkdirs();
            }

            File file = new File(path.getAbsolutePath() + File.separator
                    + Constants.ModulePath.NAME_CALENDAR);
            if (!file.exists()) {
                try {
                    file.createNewFile();
                } catch (Exception e) {
                    Logger.e(CLASS_TAG, "onStart():create file failed");
                }
            }

            try {
                FileWriter fstream = new FileWriter(file);
                mOut = new BufferedWriter(fstream);
            } catch (Exception e) {
                Logger.d(CLASS_TAG, "VCAL: onStart() write file failed");
            }
        }
    }

    /**
     * Describe <code>onEnd</code> method here.
     */
    public void onEnd() {
        super.onEnd();
        if (mCursor != null) {
            mCursor.close();
            mCursor = null;
        }

        Logger.d(CLASS_TAG, "onEnd");
        if (mOut != null) {
            try {
            } catch (Exception e) {
                Logger.d(CLASS_TAG, "VCAL: onEnd() write file failed");
            } finally {
                try {
                    mOut.close();
                } catch (IOException e) {
                    Logger.d(CLASS_TAG, "IOException");
                }
            }
        }
    }
}
