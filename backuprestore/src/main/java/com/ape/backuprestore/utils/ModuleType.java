package com.ape.backuprestore.utils;

import android.content.Context;

import com.ape.backuprestore.R;

import java.util.HashMap;
import java.util.Map;


public class ModuleType {
    public static final int TYPE_INVALID = 0x0;
    public static final int TYPE_CONTACT = 0x1;
    public static final int TYPE_SMS = 0x2;
    public static final int TYPE_MMS = 0x4;
    public static final int TYPE_CALENDAR = 0x8;
    public static final int TYPE_APP = 0x10;
    public static final int TYPE_PICTURE = 0x20;
    public static final int TYPE_MESSAGE = 0x40;
    public static final int TYPE_MUSIC = 0x80;
    public static final int TYPE_NOTEBOOK = 0x100;
    public static final int TYPE_BOOKMARK = 0x200;
    public static final int TYPE_CALL_LOG = 0x300;
    private static final String CLASS_TAG = Logger.LOG_TAG + "/ModuleType";
    //public static final int TYPE_SELECT = 0x400;
    public static Map<Integer, String> sModuleTypeFolderInfo = new HashMap<>();

    static {
        sModuleTypeFolderInfo.put(TYPE_CONTACT, Constants.ModulePath.FOLDER_CONTACT);
        sModuleTypeFolderInfo.put(TYPE_SMS, Constants.ModulePath.FOLDER_SMS);
        sModuleTypeFolderInfo.put(TYPE_MMS, Constants.ModulePath.FOLDER_MMS);
        sModuleTypeFolderInfo.put(TYPE_CALENDAR, Constants.ModulePath.FOLDER_CALENDAR);
        sModuleTypeFolderInfo.put(TYPE_APP, Constants.ModulePath.FOLDER_APP);
        sModuleTypeFolderInfo.put(TYPE_PICTURE, Constants.ModulePath.FOLDER_PICTURE);
        sModuleTypeFolderInfo.put(TYPE_MUSIC, Constants.ModulePath.FOLDER_MUSIC);
        sModuleTypeFolderInfo.put(TYPE_NOTEBOOK, Constants.ModulePath.FOLDER_NOTEBOOK);
        sModuleTypeFolderInfo.put(TYPE_CALL_LOG, Constants.ModulePath.FOLDER_CALL_LOG);
//        sModuleTypeFolderInfo.put(TYPE_BOOKMARK, ModulePath.FOLDER_BOOKMARK);

    }

    private ModuleType() {
    }

    public static String getModuleStringFromType(Context context, int type) {
        int resId = 0;
        switch (type) {
//        case ModuleType.TYPE_SELECT:
//            resId = R.string.contact_module;
//            break;

            case ModuleType.TYPE_CONTACT:
                resId = R.string.contact_module;
                break;

            case ModuleType.TYPE_MESSAGE:
                resId = R.string.message_module;
                break;

            case ModuleType.TYPE_CALENDAR:
                resId = R.string.calendar_module;
                break;

            case ModuleType.TYPE_PICTURE:
                resId = R.string.picture_module;
                break;

            case ModuleType.TYPE_APP:
                resId = R.string.app_module;
                break;

            case ModuleType.TYPE_MUSIC:
                resId = R.string.music_module;
                break;

            case ModuleType.TYPE_NOTEBOOK:
                resId = R.string.notebook_module;
                break;

            case ModuleType.TYPE_CALL_LOG:
                resId = R.string.calllog_module;
                break;

//        case ModuleType.TYPE_BOOKMARK:
//            resId = R.string.bookmark_module;
//            break;

            default:
                break;
        }
        Logger.d(CLASS_TAG, "getModuleStringFromType: resId = " + resId);
        return context.getResources().getString(resId);
    }
}
