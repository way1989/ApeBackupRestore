package com.ape.backuprestore.utils;

import android.util.Log;

public class Logger {
    public static final String LOG_TAG = "ApeTransfer";
    public static final String BACKUP_ACTIVITY_TAG = "BackupActivity: ";
    public static final String BACKUP_SERVICE_TAG = "BackupService: ";
    public static final String BACKUP_ENGINE_TAG = "BackupEngine: ";
    public static final String APP_TAG = "App: ";
    public static final String CONTACT_TAG = "Contact: ";
    public static final String MESSAGE_TAG = "Message: ";
    public static final String MMS_TAG = "Mms: ";
    public static final String SMS_TAG = "SMS: ";
    public static final String MUSIC_TAG = "Music: ";
    public static final String PICTURE_TAG = "Picture: ";
    public static final String NOTEBOOK_TAG = "NoteBook: ";
    public static final String SETTINGS_TAG = "Settings: ";
    public static final String BOOKMARK_TAG = "Bookmark: ";

    private Logger() {
    }

    public static void v(String tag, String msg) {
        Log.v(tag, msg);
    }

    public static void i(String tag, String msg) {
        Log.i(tag, msg);
    }

    public static void d(String tag, String msg) {
        Log.d(tag, msg);
    }

    public static void w(String tag, String msg) {
        Log.w(tag, msg);
    }

    public static void e(String tag, String msg) {
        Log.e(tag, msg);
    }
}
