package com.ape.backuprestore.modules;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony;
import android.text.format.DateFormat;

import com.ape.backuprestore.utils.Logger;
import com.ape.backuprestore.utils.ModuleType;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

/**
 * Created by android on 16-7-16.
 */
public class SmsBackupComposer extends Composer {
    private static final String CLASS_TAG = Logger.LOG_TAG + "/SmsBackupComposer";
    private static final String TRICKY_TO_GET_DRAFT_SMS_ADDRESS =
            "canonical_addresses.address from sms,threads,canonical_addresses " +
                    "where sms.thread_id=threads._id and threads.recipient_ids=canonical_addresses._id" +
                    " and sms.thread_id =";

    private static final String COLUMN_NAME_DATE = "date";
    private static final String COLUMN_NAME_DATE_SENT = "date_sent";
    private static final String COLUMN_NAME_READ = "read";
    private static final String COLUMN_NAME_SEEN = "seen";
    private static final String COLUMN_NAME_TYPE = "type";
    private static final String COLUMN_NAME_SIM_ID = "sub_id";
    private static final String COLUMN_NAME_LOCKED = "locked";
    private static final String COLUMN_NAME_THREAD_ID = "thread_id";
    private static final String COLUMN_NAME_ADDRESS = "address";
    private static final String COLUMN_NAME_SC = "service_center";
    private static final String COLUMN_NAME_BODY = "body";

    private static final Uri[] mSmsUriArray = {
            Telephony.Sms.Inbox.CONTENT_URI,
            Telephony.Sms.Sent.CONTENT_URI,
            //Sms.Outbox.CONTENT_URI,
            //Sms.Draft.CONTENT_URI
    };
    private static final String mStoragePath = "sms";
    private static final String mStorageName = "sms.vmsg";
    private static final String UTF = "UTF-8";
    private static final String QUOTED = "QUOTED-PRINTABLE";
    private static final String CHARSET = "CHARSET=";
    private static final String ENCODING = "ENCODING=";
    private static final String VMESSAGE_END_OF_SEMICOLON = ";";
    private static final String VMESSAGE_END_OF_COLON = ":";
    private static final String VMESSAGE_END_OF_LINE = "\r\n";
    private static final String BEGIN_VMSG = "BEGIN:VMSG";
    private static final String END_VMSG = "END:VMSG";
    private static final String VERSION = "VERSION:";
    private static final String BEGIN_VCARD = "BEGIN:VCARD";
    private static final String END_VCARD = "END:VCARD";
    private static final String BEGIN_VBODY = "BEGIN:VBODY";
    private static final String END_VBODY = "END:VBODY";
    private static final String FROMTEL = "FROMTEL:";
    private static final String XBOX = "X-BOX:";
    private static final String XREAD = "X-READ:";
    private static final String XSEEN = "X-SEEN:";
    private static final String XSIMID = "X-SIMID:";
    private static final String XLOCKED = "X-LOCKED:";
    private static final String XTYPE = "X-TYPE:";
    private static final String DATE = "Date:";
    private static final String DATE_SENT = "DateSent:";
    private static final String SUBJECT = "Subject;";
    Writer mWriter = null;
    private Cursor[] mSmsCursorArray = {null, null};

    public SmsBackupComposer(Context context) {
        super(context);
    }

    /**
     * @param timeStamp  time
     * @param sentTime   sent time
     * @param readByte   read
     * @param boxType    box
     * @param mSlotid    slot id
     * @param locked     locked
     * @param smsAddress address
     * @param body       body
     * @param mseen      seen
     * @return String.
     */
    public static String combineVmsg(String timeStamp, String sentTime, String readByte,
                                     String boxType, String mSlotid, String locked, String smsAddress, String body,
                                     String mseen) {
        StringBuilder mBuilder = new StringBuilder();
        mBuilder.append(BEGIN_VMSG);
        mBuilder.append(VMESSAGE_END_OF_LINE);
        mBuilder.append(VERSION);
        mBuilder.append("1.1");
        mBuilder.append(VMESSAGE_END_OF_LINE);
        mBuilder.append(BEGIN_VCARD);
        mBuilder.append(VMESSAGE_END_OF_LINE);
        mBuilder.append(FROMTEL);
        mBuilder.append(smsAddress);
        mBuilder.append(VMESSAGE_END_OF_LINE);
        mBuilder.append(END_VCARD);
        mBuilder.append(VMESSAGE_END_OF_LINE);
        mBuilder.append(BEGIN_VBODY);
        mBuilder.append(VMESSAGE_END_OF_LINE);
        mBuilder.append(XBOX);
        mBuilder.append(boxType);
        mBuilder.append(VMESSAGE_END_OF_LINE);
        mBuilder.append(XREAD);
        mBuilder.append(readByte);
        mBuilder.append(VMESSAGE_END_OF_LINE);
        mBuilder.append(XSEEN);
        mBuilder.append(mseen);
        mBuilder.append(VMESSAGE_END_OF_LINE);
        mBuilder.append(XSIMID);
        mBuilder.append(mSlotid);
        mBuilder.append(VMESSAGE_END_OF_LINE);
        mBuilder.append(XLOCKED);
        mBuilder.append(locked);
        mBuilder.append(VMESSAGE_END_OF_LINE);
        mBuilder.append(XTYPE);
        mBuilder.append("SMS");
        mBuilder.append(VMESSAGE_END_OF_LINE);
        mBuilder.append(DATE);
        mBuilder.append(timeStamp);
        mBuilder.append(VMESSAGE_END_OF_LINE);
        mBuilder.append(DATE_SENT);
        mBuilder.append(sentTime);
        mBuilder.append(VMESSAGE_END_OF_LINE);
        mBuilder.append(SUBJECT);
        mBuilder.append(ENCODING);
        mBuilder.append(QUOTED);
        mBuilder.append(VMESSAGE_END_OF_SEMICOLON);
        mBuilder.append(CHARSET);
        mBuilder.append(UTF);
        mBuilder.append(VMESSAGE_END_OF_COLON);
        mBuilder.append(body);
        mBuilder.append(VMESSAGE_END_OF_LINE);
        mBuilder.append(END_VBODY);
        mBuilder.append(VMESSAGE_END_OF_LINE);
        mBuilder.append(END_VMSG);
        mBuilder.append(VMESSAGE_END_OF_LINE);
        return mBuilder.toString();
    }

    @Override
    public int getModuleType() {
        return ModuleType.TYPE_SMS;
    }

    @Override
    public int getCount() {
        int count = 0;
        for (Cursor cur : mSmsCursorArray) {
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
        for (Cursor cur : mSmsCursorArray) {
            if (cur != null && !cur.isAfterLast()) {
                result = false;
                break;
            }
        }

        Logger.d(CLASS_TAG, "isAfterLast():" + result);
        return result;
    }

    @Override
    public boolean init() {
        boolean result = false;
        for (int i = 0; i < mSmsUriArray.length; ++i) {
            mSmsCursorArray[i] = mContext.getContentResolver().query(mSmsUriArray[i], null, null,
                    null, "date ASC");
            if (mSmsCursorArray[i] != null) {
                mSmsCursorArray[i].moveToFirst();
                result = true;
            }
        }

        Logger.d(CLASS_TAG, "init():" + result + ",count:" + getCount());
        return result;
    }

    @Override
    protected boolean implementComposeOneEntity() {
        boolean result = false;

        for (int i = 0; i < mSmsCursorArray.length; ++i) {
            if (mSmsCursorArray[i] != null && !mSmsCursorArray[i].isAfterLast()) {
                Cursor tmpCur = mSmsCursorArray[i];

                long mtime = tmpCur.getLong(tmpCur.getColumnIndex(COLUMN_NAME_DATE));

                String timeStamp = formatTimeStampString(mContext, mtime);

                String sentTime = formatTimeStampString(
                        mContext,
                        tmpCur.getLong(tmpCur.getColumnIndex(COLUMN_NAME_DATE_SENT)));

                int read = tmpCur.getInt(tmpCur.getColumnIndex(COLUMN_NAME_READ));
                String readByte = (read == 0 ? "UNREAD" : "READ");

                String seen = tmpCur.getString(tmpCur.getColumnIndex(COLUMN_NAME_SEEN));


                int box = tmpCur.getInt(tmpCur.getColumnIndex(COLUMN_NAME_TYPE));
                String boxType = null;
                switch (box) {
                    case 1:
                        boxType = "INBOX";
                        break;

                    case 2:
                        boxType = "SENDBOX";
                        break;

                    default:
                        boxType = "INBOX";
                        break;
                }


                //long simid = tmpCur.getLong(tmpCur.getColumnIndex(COLUMN_NAME_SIM_ID));
                String mSlotid = "0";
                /*if (SystemProperties.getBoolean("ro.mediatek.gemini_support", false)
                        && simid >= 0) {

                    SubscriptionInfo simInfo = SubscriptionManager.from(mContext)
                            .getActiveSubscriptionInfo((int) simid);
                    int slot = -1;
                    if (simInfo != null) {
                        slot = simInfo.getSimSlotIndex();
                    }
                    mSlotid = String.valueOf(slot + 1);

                }*/

                int lock = tmpCur.getInt(tmpCur.getColumnIndex(COLUMN_NAME_LOCKED));
                String locked = (lock == 1 ? "LOCKED" : "UNLOCKED");

                String smsAddress = null;
                if (i == 3) {
                    String threadId = tmpCur
                            .getString(tmpCur.getColumnIndex(COLUMN_NAME_THREAD_ID));
                    Cursor draftCursor = mContext
                            .getContentResolver()
                            .query(Uri.parse("content://sms"),
                                    new String[]{TRICKY_TO_GET_DRAFT_SMS_ADDRESS + threadId + " --"},
                                    null,
                                    null,
                                    null);

                    if (draftCursor != null) {
                        if (draftCursor.moveToFirst()) {
                            smsAddress = draftCursor.getString(draftCursor
                                    .getColumnIndex(COLUMN_NAME_ADDRESS));
                        }
                        draftCursor.close();
                    }
                } else {
                    smsAddress = tmpCur.getString(tmpCur.getColumnIndex(COLUMN_NAME_ADDRESS));
                }

                if (smsAddress == null) {
                    smsAddress = "";
                }

                //String sc = tmpCur.getString(tmpCur.getColumnIndex(COLUMN_NAME_SC));

                String body = tmpCur.getString(tmpCur.getColumnIndex(COLUMN_NAME_BODY));

                if (body != null) {
                    StringBuffer sbf = new StringBuffer(body);
                    int num = 0;
                    num = sbf.indexOf("END:VBODY");
                    do {
                        if (num >= 0) {
                            sbf.insert(num, "/");
                        } else {
                            break;
                        }
                    } while ((num = sbf.indexOf("END:VBODY", num + 1 + "END:VBODY".length())) >= 0);
                    body = sbf.toString();
                } else {
                    body = "";
                }

                try {
                    if (mWriter != null) {
                        mWriter.write(combineVmsg(timeStamp, sentTime, readByte, boxType, mSlotid,
                                locked, smsAddress, body, seen));
                        result = true;
                    }
                } catch (Exception e) {
                    Logger.e(CLASS_TAG, "mWriter.write() failed");
                } finally {
                    tmpCur.moveToNext();
                }
                break;
            }
        }

        return result;
    }

    /**
     * Describe <code>onStart</code> method here.
     */
    public final void onStart() {
        super.onStart();
        Logger.e(CLASS_TAG, "onStart():mParentFolderPath:" + mParentFolderPath);

        if (getCount() > 0) {
            File path = new File(mParentFolderPath + File.separator + mStoragePath);
            if (!path.exists()) {
                path.mkdirs();
            }

            File file = new File(path.getAbsolutePath() + File.separator + mStorageName);
            if (!file.exists()) {
                try {
                    file.createNewFile();
                } catch (Exception e) {
                    Logger.e(CLASS_TAG, "onStart():file:" + file.getAbsolutePath());
                    Logger.e(CLASS_TAG, "onStart():create file failed");
                }
            }

            try {
                mWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));
            } catch (Exception e) {
                Logger.e(CLASS_TAG, "new BufferedWriter failed");
            }
        }
    }

    /**
     * Describe <code>onEnd</code> method here.
     */
    public final void onEnd() {
        super.onEnd();
        try {
            Logger.d(CLASS_TAG, "SmsBackupComposer onEnd");
            if (mWriter != null) {
                Logger.e(CLASS_TAG, "mWriter.close()");
                mWriter.close();
            }
        } catch (Exception e) {
            Logger.e(CLASS_TAG, "mWriter.close() failed");
        }

        for (Cursor cur : mSmsCursorArray) {
            if (cur != null) {
                cur.close();
                cur = null;
            }
        }
    }

    private String formatTimeStampString(Context context, long when) { // boolean fullFormat
        CharSequence formattor = DateFormat.format("yyyy/MM/dd kk:mm:ss", when);
        return formattor.toString();
    }
}
