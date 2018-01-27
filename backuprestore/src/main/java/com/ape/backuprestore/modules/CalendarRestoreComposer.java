package com.ape.backuprestore.modules;

import android.content.Context;
import android.net.Uri;
import android.provider.CalendarContract;

import com.ape.backuprestore.utils.Constants;
import com.ape.backuprestore.utils.Logger;
import com.ape.backuprestore.utils.ModuleType;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * Created by android on 16-7-16.
 */
public class CalendarRestoreComposer extends Composer {
    private static final String TAG = Logger.LOG_TAG + "/CalendarRestoreComposer";
    private static final String COLUMN_ID = "_id";
    private static final Uri CALANDER_EVENT_URI = CalendarContract.Events.CONTENT_URI;
    private int mIndex;
    private boolean mResult = true;

    private int mCount;

    public CalendarRestoreComposer(Context context) {
        super(context);
    }

    @Override
    public int getModuleType() {
        return ModuleType.TYPE_CALENDAR;
    }

    @Override
    public int getCount() {
        Logger.d(TAG, "getCount():" + mCount);
        return mCount;
    }

    @Override
    public boolean isAfterLast() {
        boolean result = false;
        if (mCount > -1) {
            result = (mIndex >= mCount);
        }

        Logger.d(TAG, "isAfterLast():" + result);
        return result;
    }

    @Override
    public boolean init() {
        boolean result = false;
        String fileName = mParentFolderPath + File.separator + Constants.ModulePath.FOLDER_CALENDAR
                + File.separator + Constants.ModulePath.NAME_CALENDAR;
        File file = new File(fileName);
        if (file.exists() && file.isFile()) {
            mCount = getCalendarEventNum(fileName);
            if (fileName.contains("#")) {
                return false;
            }
            mIndex = 0;
            result = true;
        }

        Logger.d(TAG, "init():" + result);
        return result;
    }

    @Override
    protected boolean implementComposeOneEntity() {
        Logger.d(TAG, "implementComposeOneEntity():" + mIndex++);
        return mResult;
    }

    private int getCalendarEventNum(String fileName) {
        int calEventNum = 0;
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(fileName));
            String str = null;
            while ((str = reader.readLine()) != null) {
                if (str.contains("END:VEVENT")) {
                    ++calEventNum;
                }
            }
        } catch (IOException e) {
            Logger.e(TAG, "getCalendarEventNum read file failed");
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    Logger.e(TAG, "getCalendarEventNum close reader failed");
                }
            }
        }

        return (calEventNum == 0) ? -1 : calEventNum;
    }

    public final boolean composeOneEntity() {
        return implementComposeOneEntity();
    }

    private boolean deleteAllCalendarEvents() {
        String selection = CalendarContract.Events._ID + ">0";
        mContext.getContentResolver().delete(CALANDER_EVENT_URI, selection, null);

        Logger.d(TAG, "deleteAllCalendarEvents() and all events will be deleted!");
        return true;
    }

    public void onStart() {
        mResult = true;
        deleteAllCalendarEvents();

        super.onStart();
        Logger.d(TAG, "onStart()");
    }


    public void onEnd() {

        super.onEnd();
        Logger.d(TAG, "onEnd()");
    }

}
