/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

package com.ape.backuprestore.utils;

import android.content.Context;
import android.text.TextUtils;

import com.ape.backuprestore.RecordXmlComposer;
import com.ape.backuprestore.RecordXmlInfo;
import com.ape.backuprestore.RecordXmlParser;
import com.ape.backuprestore.modules.CalendarRestoreComposer;
import com.ape.backuprestore.modules.CallLogRestoreComposer;
import com.ape.backuprestore.modules.Composer;
import com.ape.backuprestore.modules.ContactRestoreComposer;
import com.ape.backuprestore.modules.MessageRestoreComposer;
import com.ape.backuprestore.modules.MusicRestoreComposer;
import com.ape.backuprestore.modules.NoteBookRestoreComposer;
import com.ape.backuprestore.modules.PictureRestoreComposer;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class BackupFilePreview {
    private static final String TAG = "BackupFilePreview";
    private volatile static BackupFilePreview INSTANCE;
    private final int UN_PARSED_TYPE = -1;
    private File mFolderName = null;
    private long mSize = 0;
    private int mTypes = UN_PARSED_TYPE;
    private String backupTime;
    private boolean mIsRestored = false;
    private boolean mIsOtherBackup = true;
    private boolean mIsSelfBackup = true;
    private HashMap<Integer, Integer> mNumberMap = new HashMap<>();

    private BackupFilePreview() {

    }

    public static BackupFilePreview getInstance() {
        if (INSTANCE == null) {
            synchronized (BackupFilePreview.class) {
                if (INSTANCE == null) {
                    INSTANCE = new BackupFilePreview();
                }
            }
        }
        return INSTANCE;
    }

    public boolean init() {
        String path = StorageUtils.getBackupPath();
        if (TextUtils.isEmpty(path))
            return false;

        File file = new File(path);
        if (FileUtils.isEmptyFolder(file)) {
            Logger.e(TAG, "constractor error! file is null");
            return false;
        }

        mNumberMap.clear();
        mFolderName = file;
        Logger.i(TAG, "new BackupFilePreview: file is " + mFolderName.getAbsolutePath());
        computeSize();
        checkRestored();
        return true;
    }

    private void computeSize() {
        mSize = FileUtils.computeAllFileSizeInFolder(mFolderName);
        Logger.i(TAG, "new BackupFilePreview: size = " + mSize);
    }

    private void checkRestored() {
        mIsRestored = false;
        mIsOtherBackup = true;
        mIsSelfBackup = true;
        String xmlFilePath = mFolderName + File.separator + Constants.RECORD_XML;
        File recordXmlFile = new File(xmlFilePath);
        if (!recordXmlFile.exists()) {
            addToCurrentBackupHistory(xmlFilePath);
            return;
        }

        String content = Utils.readFromFile(xmlFilePath);
        ArrayList<RecordXmlInfo> recordList;
        if (!TextUtils.isEmpty(content)) {
            recordList = RecordXmlParser.parse(content);
            if (recordList != null && recordList.size() > 0) {
                if (recordList.size() > 1) {
                    mIsSelfBackup = false;
                }
                String currentDevice = Utils.getPhoneSearialNumber();
                for (RecordXmlInfo record : recordList) {
                    if (record.getDevice().equals(currentDevice)) {
                        mIsOtherBackup = false;
                        if (record.isRestore()) {
                            mIsRestored = true;
                        }
                    }
                }
                if (mIsOtherBackup) {
                    addCurrentSN(recordList, xmlFilePath);
                }
            }
        }
        Logger.i(TAG, "mIsRestored = " + mIsRestored + ", mIsOtherBackup = " + mIsOtherBackup
                + ", mIsSelfBackup = " + mIsSelfBackup);
    }

    public boolean isSelfBackup() {
        return mIsSelfBackup;
    }

    private void addToCurrentBackupHistory(String xmlFilePath) {
        Logger.d(TAG, "addToCurrentBackupHistory() xmlFilePath : " + xmlFilePath);
        RecordXmlInfo backupInfo = new RecordXmlInfo();
        backupInfo.setRestore(false);
        backupInfo.setDevice(Utils.getPhoneSearialNumber());
        backupInfo.setTime("" + System.currentTimeMillis());
        RecordXmlComposer xmlComposer = new RecordXmlComposer();
        xmlComposer.startCompose();
        xmlComposer.addOneRecord(backupInfo);
        xmlComposer.endCompose();
        if (xmlFilePath != null && xmlFilePath.length() > 0) {
            Utils.writeToFile(xmlComposer.getXmlInfo(), xmlFilePath);
        }
    }

    /**
     * add this phone's SN info to record.xml
     *
     * @return add success return true, otherwise false.
     */
    private boolean addCurrentSN(List<RecordXmlInfo> recordList, String path) {
        boolean success = false;
        try {
            RecordXmlInfo restoreInfo = new RecordXmlInfo();
            restoreInfo.setRestore(false);
            restoreInfo.setDevice(Utils.getPhoneSearialNumber());
            restoreInfo.setTime(String.valueOf(System.currentTimeMillis()));

            recordList.add(restoreInfo);

            RecordXmlComposer xmlComposer = new RecordXmlComposer();
            xmlComposer.startCompose();
            for (RecordXmlInfo record : recordList) {
                xmlComposer.addOneRecord(record);
            }
            xmlComposer.endCompose();
            Utils.writeToFile(xmlComposer.getXmlInfo(), path);
            success = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        Logger.i(TAG, "addCurrentSN() success : " + success);
        return success;
    }

    public boolean isRestored() {
        return mIsRestored;
    }

    public boolean isOtherDeviceBackup() {
        Logger.d(TAG, "isOtherDeviceBackup() : " + mIsOtherBackup);
        return mIsOtherBackup;
    }

    public File getFile() {
        return mFolderName;
    }

    public String getFileName() {
        String showNameString = mFolderName.getName();
        if (showNameString.length() == 14 && showNameString.trim().length() == 14) {
            showNameString = formatString(showNameString);
        }
        return showNameString;
    }

    private String formatString(String showNameString) {
        if (showNameString != null) {
            String yearString = showNameString.substring(0, 4);
            String monthString = showNameString.substring(4, 6);
            String dayString = showNameString.substring(6, 8);
            String hourString = showNameString.substring(8, 10);
            String minString = showNameString.substring(10, 12);
            String secString = showNameString.substring(12, 14);
            return yearString + "-" + monthString + "-" + dayString + "  " + hourString + ":"
                    + minString + ":" + secString;
        }
        return null;
    }

    public String getBackupTime() {
        if (backupTime == null) {
            Long time = mFolderName.lastModified();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
            backupTime = dateFormat.format(new Date(time));
        }
        return backupTime;
    }

    public void setBackupTime(String backupTime) {
        this.backupTime = backupTime;
    }

    public long getFileSize() {
        return mSize;
    }

    public int getBackupModules(Context context) {
        if (mTypes == UN_PARSED_TYPE) {
            mTypes = peekBackupModules(context);
        }
        return mTypes;
    }

    /**
     * parse backup items.
     */
    private int peekBackupModules(Context context) {

        File[] files = mFolderName.listFiles();
        mTypes = 0;
        if (files != null) {
            for (File file : files) {
                String[] moduleFolders = new String[]{
                        Constants.ModulePath.FOLDER_CALENDAR,
                        Constants.ModulePath.FOLDER_CONTACT,
                        Constants.ModulePath.FOLDER_MMS,
                        Constants.ModulePath.FOLDER_MUSIC,
                        Constants.ModulePath.FOLDER_PICTURE,
                        Constants.ModulePath.FOLDER_SMS,
                        Constants.ModulePath.FOLDER_CALL_LOG
                };

                int[] moduleTypes = new int[]{
                        ModuleType.TYPE_CALENDAR,
                        ModuleType.TYPE_CONTACT,
                        ModuleType.TYPE_MESSAGE,
                        ModuleType.TYPE_MUSIC,
                        ModuleType.TYPE_PICTURE,
                        ModuleType.TYPE_MESSAGE,
                        ModuleType.TYPE_CALL_LOG
                };

                if (file.isDirectory() && !FileUtils.isEmptyFolder(file)) {
                    String name = file.getName();
                    int count = moduleFolders.length;
                    for (int index = 0; index < count; index++) {
                        if (moduleFolders[index].equalsIgnoreCase(name)) {
                            initNumByType(context, moduleTypes[index]);
                            if (getItemCount(moduleTypes[index]) > 0) {
                                mTypes |= moduleTypes[index];
                            }
                        }
                    }
                }
            }
        }
        Logger.i(TAG, "parseItemTypes: mTypes =  " + mTypes);
        return mTypes;
    }

    private void initNumByType(Context context, final int type) {
        Composer composer = null;
        switch (type) {
            case ModuleType.TYPE_CONTACT:
                composer = new ContactRestoreComposer(context);
                break;

            case ModuleType.TYPE_CALENDAR:
                composer = new CalendarRestoreComposer(context);
                break;

            case ModuleType.TYPE_MESSAGE:
                composer = new MessageRestoreComposer(context);
                break;

            case ModuleType.TYPE_MUSIC:
                composer = new MusicRestoreComposer(context);
                break;

            case ModuleType.TYPE_NOTEBOOK:
                composer = new NoteBookRestoreComposer(context);
                break;

            case ModuleType.TYPE_PICTURE:
                composer = new PictureRestoreComposer(context);
                break;

            case ModuleType.TYPE_CALL_LOG:
                composer = new CallLogRestoreComposer(context);
                break;

            default:
                break;
        }
        if (composer != null) {
            composer.setParentFolderPath(mFolderName.getAbsolutePath());
            composer.init();
            int count = composer.getCount();
            Logger.d(TAG, "initNumByType: count = " + count);
            mNumberMap.put(type, count);
        }
    }

    public int getItemCount(int type) {
        Logger.d(TAG, "list.get(0).type = " + type);
        Logger.d(TAG, "mNumberMap.size() = " + mNumberMap.size());
        int count = 0;
        if (mNumberMap.get(type) != null) {
            count = mNumberMap.get(type);
        }

        Logger.d(TAG, "getItemCount: type = " + type + ",count = " + count);
        return count;
    }
}
