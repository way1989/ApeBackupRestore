package com.ape.backuprestore.modules;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony;

import com.ape.backuprestore.utils.Constants;
import com.ape.backuprestore.utils.Logger;
import com.ape.backuprestore.utils.ModuleType;
import com.google.android.mms.InvalidHeaderValueException;
import com.google.android.mms.MmsException;
import com.google.android.mms.pdu.NotificationInd;
import com.google.android.mms.pdu.PduComposer;
import com.google.android.mms.pdu.PduHeaders;
import com.google.android.mms.pdu.PduPersister;
import com.google.android.mms.pdu.RetrieveConf;
import com.google.android.mms.pdu.SendReq;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by android on 16-7-16.
 */
public class MmsBackupComposer extends Composer {
    private static final String CLASS_TAG = Logger.LOG_TAG + "/MmsBackupComposer";
    //private static final String MMS_SPECIAL_TYPE = "134";
    private static final String[] MMS_EXCLUDE_TYPE = {"134", "130"};
    private static final String COLUMN_NAME_ID = "_id";
    private static final String COLUMN_NAME_TYPE = "m_type";
    private static final String COLUMN_NAME_DATE = "date";
    private static final String COLUMN_NAME_MESSAGE_BOX = "msg_box";
    private static final String COLUMN_NAME_READ = "read";
    //private static final String COLUMN_NAME_ST = "st";
    private static final String COLUMN_NAME_SIMID = "sub_id";
    private static final String COLUMN_NAME_LOCKED = "locked";

    private static final Uri[] MMS_URI_LIST = {
            Telephony.Mms.Sent.CONTENT_URI,
            Telephony.Mms.Inbox.CONTENT_URI
    };
    private static final String mStoragePath = "mms";
    private Cursor[] mMmsCursorArray = {null, null};
    private int mMmsIndex = 0;
    private MmsXmlComposer mXmlComposer;
    private Object mLock = new Object();
    private ArrayList<MmsBackupContent> mPduList = null;
    private ArrayList<MmsBackupContent> mTempPduList = null;

    public MmsBackupComposer(Context context) {
        super(context);
    }

    @Override
    public int getModuleType() {
        return ModuleType.TYPE_MMS;
    }

    @Override
    public int getCount() {
        int count = 0;
        for (Cursor cur : mMmsCursorArray) {
            if (cur != null && !cur.isClosed() && cur.getCount() > 0) {
                count += cur.getCount();
            }
        }
        Logger.d(CLASS_TAG, "getCount():" + count);
        return count;
    }

    @Override
    public boolean isAfterLast() {
        boolean result = true;
        for (Cursor cur : mMmsCursorArray) {
            if (cur != null && !cur.isAfterLast()) {
                result = false;
                break;
            }
        }

        Logger.d(CLASS_TAG, "isAfterLast():" + result);
        return result;
    }

    /**
     * Describe <code>composeOneEntity</code> method here.
     *
     * @return a <code>boolean</code> value
     */
    public boolean composeOneEntity() {
        return implementComposeOneEntity();
    }

    @Override
    public boolean init() {
        boolean result = false;
        mMmsIndex = 0;
        mTempPduList = new ArrayList<MmsBackupContent>();
        for (int i = 0; i < MMS_URI_LIST.length; ++i) {
            if (MMS_URI_LIST[i].equals(Telephony.Mms.Inbox.CONTENT_URI)) {
                mMmsCursorArray[i] = mContext.getContentResolver().query(
                        MMS_URI_LIST[i],
                        null,
                        "m_type <> ? AND m_type <> ?",
                        MMS_EXCLUDE_TYPE,
                        null);
            } else {
                mMmsCursorArray[i] = mContext.getContentResolver().query(
                        MMS_URI_LIST[i],
                        null,
                        null,
                        null,
                        null);
            }

            if (mMmsCursorArray[i] != null) {
                mMmsCursorArray[i].moveToFirst();
                result = true;
            }
        }

        Logger.d(CLASS_TAG, "init():" + result + " count:" + getCount());
        return result;
    }

    @Override
    protected boolean implementComposeOneEntity() {
        boolean result = false;
        byte[] pduMid = null;

        for (int i = 0; i < mMmsCursorArray.length; ++i) {
            if (mMmsCursorArray[i] != null && !mMmsCursorArray[i].isAfterLast()) {
                int id = mMmsCursorArray[i].getInt(
                        mMmsCursorArray[i].getColumnIndex(COLUMN_NAME_ID));
                Uri realUri = ContentUris.withAppendedId(MMS_URI_LIST[i], id);
                Logger.d(CLASS_TAG, "id:" + id + ",realUri:" + realUri);
                PduPersister p = PduPersister.getPduPersister(mContext);
                try {
                    if (MMS_URI_LIST[i].equals(Telephony.Mms.Inbox.CONTENT_URI)) {
                        int type = mMmsCursorArray[i].getInt(
                                mMmsCursorArray[i].getColumnIndex(COLUMN_NAME_TYPE));
                        Logger.d(CLASS_TAG, "inbox, m_type:" + type);
                        if (type == PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND) {
                            NotificationInd nPdu = (NotificationInd) p.load(realUri);
                            pduMid = new PduComposer(mContext, nPdu).make(true);
                        } else if (type == PduHeaders.MESSAGE_TYPE_RETRIEVE_CONF) {
                            RetrieveConf rPdu = (RetrieveConf) p.load(realUri, true);
                            pduMid = new PduComposer(mContext, rPdu).make(true);
                        } else {
                            pduMid = null;
                        }
                    } else {
                        SendReq sPdu = (SendReq) p.load(realUri);
                        pduMid = new PduComposer(mContext, sPdu).make();
                    }

                    if (pduMid != null) {
                        String fileName = Integer.toString(mMmsIndex) + Constants.ModulePath.FILE_EXT_PDU;
                        String isRead = mMmsCursorArray[i].getString(
                                mMmsCursorArray[i].getColumnIndex(COLUMN_NAME_READ));
                        String msgBox = mMmsCursorArray[i].getString(
                                mMmsCursorArray[i].getColumnIndex(COLUMN_NAME_MESSAGE_BOX));
                        String date = mMmsCursorArray[i].getString(
                                mMmsCursorArray[i].getColumnIndex(COLUMN_NAME_DATE));
                        long simId = mMmsCursorArray[i].getLong(
                                mMmsCursorArray[i].getColumnIndex(COLUMN_NAME_SIMID));
                        String slotId = "0";
                        /*if (Utils.isGeminiSupport() && simId >= 0) {
                            int slot = SubscriptionManager.getSlotId((int) simId);
                            Logger.d(
                                    CLASS_TAG,
                                    "SubscriptionManager.getslot : " + slot);
                            slotId = String.valueOf(slot + 1);
                        }*/

                        String isLocked = mMmsCursorArray[i].getString(
                                mMmsCursorArray[i].getColumnIndex(COLUMN_NAME_LOCKED));
                        MmsXmlInfo record = new MmsXmlInfo();
                        record.setID(fileName);
                        record.setIsRead(isRead);

                        record.setMsgBox(msgBox);
                        record.setDate(date);
                        record.setSize(Integer.toString(pduMid.length));
                        record.setSimId(slotId);
                        record.setIsLocked(isLocked);
                        MmsBackupContent tmpContent = new MmsBackupContent();
                        tmpContent.pduMid = pduMid;
                        tmpContent.fileName = fileName;
                        tmpContent.mRecord = record;
                        mTempPduList.add(tmpContent);
                        mMmsIndex += 1;
                    }

                    // Write to file every 5 (NUMBER_IMPORT_MMS_EACH) items until all are processed
                    if (mMmsIndex % Constants.NUMBER_IMPORT_MMS_EACH == 0 ||
                            mMmsIndex >= getCount()) {
                        while (mPduList != null) {
                            synchronized (mLock) {
                                try {
                                    Logger.d(
                                            CLASS_TAG,
                                            Logger.MMS_TAG + "wait for WriteFileThread:");
                                    while (mPduList != null) {
                                        mLock.wait();
                                    }
                                } catch (InterruptedException e) {
                                    Logger.d(CLASS_TAG, "InterruptedException");
                                }
                            }
                        }
                        mPduList = mTempPduList;
                        new WriteFileThread().start();
                        if (!isAfterLast()) {
                            mTempPduList = new ArrayList<MmsBackupContent>();
                        }
                    }

                    result = true;
                } catch (InvalidHeaderValueException e) {
                    Logger.d(CLASS_TAG, "InvalidHeaderValueException");
                } catch (MmsException e) {
                    Logger.d(CLASS_TAG, "MmsException");
                } finally {
                    Logger.d(CLASS_TAG, "in implementComposeOneEntity finally");
                }

                mMmsCursorArray[i].moveToNext();
                break;
            }
        }

        Logger.d(CLASS_TAG, "implementComposeOneEntity:" + result);
        return result;
    }

    /**
     * Describe <code>onStart</code> method here.
     */
    public void onStart() {
        super.onStart();
        if (getCount() > 0) {
            if ((mXmlComposer = new MmsXmlComposer()) != null) {
                mXmlComposer.startCompose();
            }

            File path = new File(mParentFolderPath + File.separator + Constants.ModulePath.FOLDER_MMS);
            if (!path.exists()) {
                path.mkdirs();
            } else {
                File[] files = path.listFiles();
                for (File file : files) {
                    if (file.isFile()) {
                        file.delete();
                    }
                }
            }
        }
    }


    /**
     * Describe <code>onEnd</code> method here.
     */
    public void onEnd() {
        if (mPduList != null) {
            synchronized (mLock) {
                try {
                    Logger.d(CLASS_TAG, Logger.MMS_TAG + "wait for WriteFileThread:");
                    while (mPduList != null) {
                        mLock.wait();
                    }
                } catch (InterruptedException e) {
                    Logger.d(CLASS_TAG, "InterruptedException");
                }
            }
        }

        super.onEnd();
        if (mXmlComposer != null) {
            mXmlComposer.endCompose();
            String msgXmlInfo = mXmlComposer.getXmlInfo();
            if (getComposed() > 0 && msgXmlInfo != null) {
                try {
                    writeToFile(mParentFolderPath + File.separator + Constants.ModulePath.FOLDER_MMS
                                    + File.separator + Constants.ModulePath.MMS_XML,
                            msgXmlInfo.getBytes());
                } catch (IOException e) {
                    if (super.mReporter != null) {
                        super.mReporter.onErr(e);
                    }
                }
            }
        }

        for (Cursor cur : mMmsCursorArray) {
            if (cur != null) {
                cur.close();
                cur = null;
            }
        }
    }

    /**
     * Describe <code>writeToFile</code> method here.
     *
     * @param fileName a <code>String</code> value
     * @param buf      a <code>byte</code> value
     * @throws IOException if an error occurs
     */
    private void writeToFile(String fileName, byte[] buf) throws IOException {
        try {
            FileOutputStream outStream = new FileOutputStream(fileName);
            // byte[] buf = inBuf.getBytes();
            outStream.write(buf, 0, buf.length);
            outStream.flush();
            outStream.close();
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class WriteFileThread extends Thread {
        @Override
        public void run() {
            for (int j = 0; (mPduList != null) && (j < mPduList.size()); ++j) {
                byte[] pduByteArray = mPduList.get(j).pduMid;
                String fileName = mPduList.get(j).fileName;

                try {
                    if (pduByteArray != null) {
                        Logger.d(CLASS_TAG, Logger.MMS_TAG
                                + "WriteFileThread() pduMid.length:"
                                + pduByteArray.length);
                        writeToFile(mParentFolderPath + File.separator + Constants.ModulePath.FOLDER_MMS
                                + File.separator + fileName, pduByteArray);
                        if (mXmlComposer != null) {
                            mXmlComposer.addOneMmsRecord(mPduList.get(j).mRecord);
                        }

                        increaseComposed(true);
                        Logger.d(CLASS_TAG, "WriteFileThread() addFile:"
                                + fileName + " success");
                    }
                } catch (IOException e) {
                    if (mReporter != null) {
                        mReporter.onErr(e);
                    }
                    Logger.e(
                            CLASS_TAG,
                            Logger.MMS_TAG + "WriteFileThread() addFile:" + fileName + " fail");
                }
            }

            synchronized (mLock) {
                mPduList = null;
                mLock.notifyAll();
            }
        }
    }

    /**
     * Describe class <code>MmsBackupContent</code> here.
     */
    private class MmsBackupContent {
        public byte[] pduMid;
        public String fileName;
        MmsXmlInfo mRecord;
    }
}
