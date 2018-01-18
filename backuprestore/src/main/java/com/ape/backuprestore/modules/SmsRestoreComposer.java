package com.ape.backuprestore.modules;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.provider.Telephony;

import com.ape.backuprestore.utils.Constants;
import com.ape.backuprestore.utils.Logger;
import com.ape.backuprestore.utils.ModuleType;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by android on 16-7-16.
 */
public class SmsRestoreComposer extends Composer {
    private static final String CLASS_TAG = Logger.LOG_TAG + "/SmsRestoreComposer";
    private static final String COLUMN_NAME_IMPORT_SMS = "import_sms";
    private static final Uri[] mSmsUriArray = {
            Telephony.Sms.Inbox.CONTENT_URI,
            Telephony.Sms.Sent.CONTENT_URI
            //Sms.Draft.CONTENT_URI,
            //Sms.Outbox.CONTENT_URI
    };
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

    /*   private boolean deleteAllPhoneSms() {
           boolean result = false;
           if (mContext != null) {
               Logger.d(CLASS_TAG, "begin delete:" + System.currentTimeMillis());
               int count = mContext.getContentResolver().delete(Uri.parse(Constants.URI_SMS),
                       "type <> ?", new String[] { Constants.MESSAGE_BOX_TYPE_INBOX });
               count += mContext.getContentResolver().delete(Uri.parse(Constants.URI_SMS), "date < ?",
                       new String[] { Long.toString(mTime) });

               int count2 = mContext.getContentResolver().delete(WapPush.CONTENT_URI, null, null);
               Logger.d(CLASS_TAG, "deleteAllPhoneSms():" + count + " sms deleted!" + count2
                       + "wappush deleted!");
               result = true;
               Logger.d(CLASS_TAG, "end delete:" + System.currentTimeMillis());
           }

           return result;
       }*/
    private static final String XSEEN = "X-SEEN:";
    private static final String XSIMID = "X-SIMID:";
    private static final String XLOCKED = "X-LOCKED:";
    private static final String XTYPE = "X-TYPE:";
    private static final String DATE = "Date:";
    private static final String DATE_SENT = "DateSent:";
    private static final String SUBJECT = "Subject";
    private static final String VMESSAGE_END_OF_COLON = ":";
    private int mIndex;
    private long mTime;
    private ArrayList<ContentProviderOperation> mOperationList;
    private ArrayList<SmsRestoreEntry> mVmessageList;

    /**
     * @param context
     */
    public SmsRestoreComposer(Context context) {
        super(context);
    }

    /**
     * @return
     */
    public int getModuleType() {
        return ModuleType.TYPE_SMS;
    }

    /**
     * @return
     */
    public int getCount() {
        int count = 0;
        if (mVmessageList != null) {
            count = mVmessageList.size();
        }

        Logger.d(CLASS_TAG, "getCount():" + count);
        return count;
    }

    /**
     * @return
     */
    public boolean init() {
        boolean result = false;

        Logger.d(CLASS_TAG, "begin init:" + System.currentTimeMillis());
        mOperationList = new ArrayList<ContentProviderOperation>();
        try {
            mTime = System.currentTimeMillis();
            mVmessageList = getSmsRestoreEntry();
            result = true;
        } catch (Exception e) {
            Logger.d(CLASS_TAG, "init failed");
        }

        Logger.d(CLASS_TAG, "end init:" + System.currentTimeMillis());
        Logger.d(CLASS_TAG, "init():" + result + ",count:" + mVmessageList.size());
        return result;
    }

    @Override
    public boolean isAfterLast() {
        boolean result = true;
        if (mVmessageList != null) {
            result = (mIndex >= mVmessageList.size()) ? true : false;
        }

        Logger.d(CLASS_TAG, "isAfterLast():" + result);
        return result;
    }

    @Override
    public boolean implementComposeOneEntity() {
        boolean result = false;
        SmsRestoreEntry vMsgFileEntry = mVmessageList.get(mIndex++);

        if (vMsgFileEntry != null) {
            ContentValues values = parsePdu(vMsgFileEntry);
            if (values == null) {
                Logger.d(CLASS_TAG, "parsePdu():values=null");
            } else {
                Logger.d(CLASS_TAG, "begin restore:" + System.currentTimeMillis());
                int mboxType = vMsgFileEntry.getBoxType().equals("INBOX") ? 1 : 2;
                Logger.d(CLASS_TAG, "mboxType:" + mboxType);
                ContentProviderOperation.Builder builder = ContentProviderOperation
                        .newInsert(mSmsUriArray[mboxType - 1]);
                builder.withValues(values);
                mOperationList.add(builder.build());
                if ((mIndex % Constants.NUMBER_IMPORT_SMS_EACH != 0) && !isAfterLast()) {
                    return true;
                }

                if (isAfterLast()) {
                    values.remove("import_sms");
                }

                if (mOperationList.size() > 0) {
                    try {
                        mContext.getContentResolver().applyBatch("sms", mOperationList);
                    } catch (android.os.RemoteException e) {
                        Logger.d(CLASS_TAG, "RemoteException");
                    } catch (android.content.OperationApplicationException e) {
                        Logger.d(CLASS_TAG, "RemoteException");
                    } finally {
                        mOperationList.clear();
                    }
                }

                Logger.d(CLASS_TAG, "end restore:" + System.currentTimeMillis());
                result = true;
            }
        } else {
            if (super.mReporter != null) {
                super.mReporter.onErr(new IOException());
            }
        }

        return result;
    }

    private ContentValues parsePdu(SmsRestoreEntry pdu) {

        ContentValues values = new ContentValues();

        values.put(Telephony.Sms.ADDRESS, pdu.getSmsAddress());
        //       values.put(Sms.SUBJECT, null);
        values.put(Telephony.Sms.BODY, pdu.getBody());
        Logger.d(CLASS_TAG, "readorunread :" + pdu.getReadByte());

        values.put(Telephony.Sms.READ, (pdu.getReadByte().equals("UNREAD") ? 0 : 1));
        //values.put(Sms.SEEN, pdu.getSeen());
        //values.put(Sms.LOCKED, (pdu.getLocked().equals("LOCKED") ? "1" : "0"));
        String simCardid = "-1";
        //values.put(Sms.SUBSCRIPTION_ID, simCardid);

        values.put(Telephony.Sms.DATE, pdu.getTimeStamp());
        values.put(Telephony.Sms.DATE_SENT, pdu.getDateSent());
        values.put(Telephony.Sms.TYPE, (pdu.getBoxType().equals("INBOX") ? 1 : 2));
        //       values.put("import_sms", true);

        return values;
    }

    @Override
    public void onStart() {
        super.onStart();
        //deleteAllPhoneSms();

        Logger.d(CLASS_TAG, "onStart()");
    }

    @Override
    public void onEnd() {
        super.onEnd();
        if (mVmessageList != null) {
            mVmessageList.clear();
        }

        if (mOperationList != null) {
            mOperationList = null;
        }

        Logger.d(CLASS_TAG, "onEnd()");
    }

    /**
     * @return
     */
    public ArrayList<SmsRestoreEntry> getSmsRestoreEntry() {
        ArrayList<SmsRestoreEntry> smsEntryList = new ArrayList<SmsRestoreEntry>();
        SimpleDateFormat sd = new SimpleDateFormat("yyyy/MM/dd kk:mm:ss");
        try {
            File file = new File(mParentFolderPath + File.separator + Constants.ModulePath.FOLDER_SMS
                    + File.separator + Constants.ModulePath.SMS_VMSG);
            InputStream instream = new FileInputStream(file);
            InputStreamReader inreader = new InputStreamReader(instream);
            BufferedReader buffreader = new BufferedReader(inreader);
            String line = null;
            StringBuffer tmpbody = new StringBuffer();
            boolean appendbody = false;
            SmsRestoreEntry smsentry = null;
            while ((line = buffreader.readLine()) != null) {
                if (line.startsWith(BEGIN_VMSG) && !appendbody) {
                    smsentry = new SmsRestoreEntry();
                    Logger.d(CLASS_TAG, "startsWith(BEGIN_VMSG)");
                }
                if (line.startsWith(FROMTEL) && !appendbody && smsentry != null) {
                    smsentry.setSmsAddress(line.substring(FROMTEL.length()));
                    Logger.d(CLASS_TAG, "startsWith(FROMTEL)");
                }
                if (line.startsWith(XBOX) && !appendbody && smsentry != null) {
                    smsentry.setBoxType(line.substring(XBOX.length()));
                    Logger.d(CLASS_TAG, "startsWith(XBOX)");
                }
                if (line.startsWith(XREAD) && !appendbody && smsentry != null) {
                    smsentry.setReadByte(line.substring(XREAD.length()));
                    Logger.d(CLASS_TAG, "startsWith(XREAD)");
                }
                if (line.startsWith(XSEEN) && !appendbody && smsentry != null) {
                    smsentry.setSeen(line.substring(XSEEN.length()));
                    Logger.d(CLASS_TAG, "startsWith(XSEEN)");
                }
                if (line.startsWith(XSIMID) && !appendbody && smsentry != null) {
                    smsentry.setSimCardid(Integer.parseInt(line
                            .substring(XSIMID.length())));
                    Logger.d(CLASS_TAG, "startsWith(XSIMID)");
                }
                if (line.startsWith(XLOCKED) && !appendbody && smsentry != null) {
                    smsentry.setLocked(line.substring(XLOCKED.length()));
                    Logger.d(CLASS_TAG, "startsWith(XLOCKED)");
                }
                if (line.startsWith(DATE) && !appendbody && smsentry != null) {
                    long result = sd.parse(line.substring(DATE.length())).getTime();
                    smsentry.setTimeStamp(String.valueOf(result));
                    Logger.d(CLASS_TAG, "startsWith(DATE)");
                }
                if (line.startsWith(DATE_SENT) && !appendbody && smsentry != null) {
                    long result = sd.parse(line.substring(DATE_SENT.length())).getTime();
                    smsentry.setDateSent(String.valueOf(result));
                    Logger.d(CLASS_TAG, "startsWith(DATE_SENT)");
                }

                if (line.startsWith(SUBJECT) && !appendbody && smsentry != null) {
                    // String bodySlash = line.substring(SUBJECT.length());
                    String bodySlash = line.substring(line.indexOf(VMESSAGE_END_OF_COLON) + 1);
                    int m = bodySlash.indexOf("END:VBODY");
                    if (m > 0) {
                        StringBuffer tempssb = new StringBuffer(bodySlash);
                        do {
                            if (m > 0) {
                                tempssb.deleteCharAt(m - 1);
                            } else {
                                break;
                            }
                        } while ((m = tempssb.indexOf("END:VBODY", m + "END:VBODY".length())) > 0);
                        bodySlash = tempssb.toString();
                    }

                    tmpbody.append(bodySlash);
                    appendbody = true;
                    Logger.d(CLASS_TAG, "startsWith(SUBJECT)");
                    continue;
                }
                if (line.startsWith(END_VBODY) && smsentry != null) {
                    appendbody = false;
                    smsentry.setBody(tmpbody.toString());
                    smsEntryList.add(smsentry);
                    tmpbody.setLength(0);
                    Logger.d(CLASS_TAG, "startsWith(END_VBODY)");
                    continue;
                }
                if (appendbody) {
                    tmpbody.append(VMESSAGE_END_OF_LINE);
                    int n = line.indexOf("END:VBODY");
                    if (n > 0) {
                        StringBuffer tempsub = new StringBuffer(line);
                        do {
                            if (n > 0) {
                                tempsub.deleteCharAt(n - 1);
                            } else {
                                break;
                            }
                        } while ((n = tempsub.indexOf("END:VBODY", n
                                + "END:VBODY".length())) > 0);
                        line = tempsub.toString();
                    }
                    tmpbody.append(line);
                    Logger.d(CLASS_TAG, "appendbody=true,tmpbody="
                            + tmpbody.toString());
                }

            }
            instream.close();
        } catch (Exception e) {
            Logger.e(CLASS_TAG, "init failed");
        }
        if (smsEntryList != null && smsEntryList.size() > 0) {
            Collections.sort(smsEntryList);
        }
        return smsEntryList;
    }

    /**
     * @author mtk81330
     */
    class SmsRestoreEntry implements Comparable<SmsRestoreEntry> {
        private String mTimeStamp;

        private String mDateSent;

        private String mReadByte;

        private String mSeen;

        private String mBoxType;

        private int mSimCardid;

        private String mLocked;

        private String mSmsAddress;

        private String mBody;

        public String getTimeStamp() {
            return mTimeStamp;
        }

        public void setTimeStamp(String timeStamp) {
            this.mTimeStamp = timeStamp;
        }

        public String getDateSent() {
            return mDateSent;
        }

        public void setDateSent(String dateSent) {
            this.mDateSent = dateSent;
        }

        public String getReadByte() {
            return mReadByte == null ? "READ" : mReadByte;
        }

        public void setReadByte(String readByte) {
            this.mReadByte = readByte;
        }

        public String getSeen() {
            return mSeen == null ? "1" : mSeen;
        }

        public void setSeen(String seen) {
            this.mSeen = seen;
        }

        public String getBoxType() {
            return mBoxType;
        }

        public void setBoxType(String boxType) {
            this.mBoxType = boxType;
        }

        public int getSimCardid() {
            return mSimCardid;
        }

        public void setSimCardid(int simCardid) {
            this.mSimCardid = simCardid;
        }

        public String getLocked() {
            return mLocked;

        }

        public void setLocked(String locked) {
            this.mLocked = locked;
        }

        public String getSmsAddress() {
            return mSmsAddress;
        }

        public void setSmsAddress(String smsAddress) {
            this.mSmsAddress = smsAddress;
        }

        public String getBody() {
            return mBody;
        }

        public void setBody(String body) {
            this.mBody = body;
        }

        @Override
        public int compareTo(SmsRestoreEntry another) {
            // TODO Auto-generated method stub
            return this.mTimeStamp.compareTo(another.mTimeStamp);
        }
    }
}
