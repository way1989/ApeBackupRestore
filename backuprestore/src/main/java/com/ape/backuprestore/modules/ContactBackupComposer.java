package com.ape.backuprestore.modules;

import android.content.Context;

import com.android.vcard.VCardComposer;
import com.android.vcard.VCardConfig;
import com.ape.backuprestore.utils.Constants;
import com.ape.backuprestore.utils.Logger;
import com.ape.backuprestore.utils.ModuleType;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by android on 16-7-16.
 */
public class ContactBackupComposer extends Composer {
    public static final String INDICATE_PHONE_SIM = "indicate_phone_sim";
    private static final String TAG = "ContactBackupComposer";
    FileOutputStream mOutStream;
    private int mIndex;
    private VCardComposer mVCardComposer;
    private int mCount;

    public ContactBackupComposer(Context context) {
        super(context);
    }

    @Override
    public int getModuleType() {
        return ModuleType.TYPE_CONTACT;
    }

    @Override
    public int getCount() {
        return mCount;
    }

    @Override
    public boolean isAfterLast() {
        boolean result = true;
        if (mVCardComposer != null) {
            result = mVCardComposer.isAfterLast();
        }

        Logger.d(TAG, "isAfterLast():" + result);
        return result;
    }

    @Override
    public boolean init() {
        boolean result = false;
        mCount = 0;
        mVCardComposer = new VCardComposer(mContext, VCardConfig.VCARD_TYPE_V21_GENERIC, true);
        String condition = getCondition();
        Logger.d(TAG, "condition:" + condition);
        if (mVCardComposer.init(condition, null)) {
            result = true;
            mCount = mVCardComposer.getCount();
        } else {
            mVCardComposer = null;
        }

        Logger.d(TAG, "init():" + result + ",count:" + mCount);

        return result;
    }

    @Override
    protected boolean implementComposeOneEntity() {
        boolean result = false;
        if (mVCardComposer != null && !mVCardComposer.isAfterLast()) {
            String tmpVcard = mVCardComposer.createOneEntry();
            if (tmpVcard != null && tmpVcard.length() > 0) {
                if (mOutStream != null) {
                    try {
                        byte[] buf = tmpVcard.getBytes();
                        mOutStream.write(buf, 0, buf.length);
                        result = true;
                    } catch (IOException e) {
                        if (super.mReporter != null) {
                            super.mReporter.onErr(e);
                        }
                    } catch (Exception e) {
                        Logger.d(TAG, "Exception");
                    }
                }
            }
        }

        Logger.d(TAG, "add result:" + result);
        return result;
    }

    /**
     * Describe <code>onStart</code> method here.
     */
    public final void onStart() {
        super.onStart();
        if (getCount() > 0) {
            String path = mParentFolderPath + File.separator + Constants.ModulePath.FOLDER_CONTACT;
            File folder = new File(path);
            if (!folder.exists()) {
                folder.mkdirs();
            }

            String fileName = path + File.separator + Constants.ModulePath.NAME_CONTACT;
            try {
                mOutStream = new FileOutputStream(fileName);
            } catch (IOException e) {
                mOutStream = null;
            } catch (Exception e) {
                Logger.d(TAG, "Exception");
            }

        }

    }

    /**
     * return .
     */
    public void onEnd() {
        super.onEnd();
        if (mVCardComposer != null) {
            mVCardComposer.terminate();
            mVCardComposer = null;
        }

        if (mOutStream != null) {
            try {
                mOutStream.flush();
                mOutStream.close();
            } catch (IOException e) {
                Logger.d(TAG, "IOException");
            } catch (Exception e) {
                Logger.d(TAG, "Exception");
            }
        }

    }

    private String getCondition() {
        List<String> conditionList = new ArrayList<>();

        if (mParams != null) {
//            List<SubscriptionInfo> simInfoList = SubscriptionManager.from(mContext)
//                    .getActiveSubscriptionInfoList();
//            if (simInfoList != null && simInfoList.size() > 0) {
//                for (SubscriptionInfo simInfo : simInfoList) {
//                    String key = simInfo.getDisplayName().toString() + simInfo.getSubscriptionId();
//                    if (mParams.contains(key)) {
//                        conditionList.add(String.valueOf(simInfo.getSubscriptionId()));
//                    }
//                }
//            }
//
//            if (mParams.contains(Constants.ContactType.PHONE)) {
//                conditionList.add("-1");
//            }
        } else {
//            List<SubscriptionInfo> simInfoList = SubscriptionManager.from(mContext)
//                    .getActiveSubscriptionInfoList();
//            if (simInfoList != null && simInfoList.size() > 0) {
//                for (SubscriptionInfo simInfo : simInfoList) {
//                    String key = simInfo.getDisplayName().toString() + simInfo.getSubscriptionId();
//                    if (key != null && key.length() > 0) {
//                        conditionList.add(String.valueOf(simInfo.getSubscriptionId()));
//                    }
//                }
//            }
            conditionList.add("-1");
        }

        int len = conditionList.size();
        if (len > 0) {
//            StringBuilder condition = new StringBuilder();
//             if ("-1".equals(conditionList.get(0))) {
//                 condition.append(" (" + INDICATE_PHONE_SIM + " ="
//                         + conditionList.get(0));
//                 condition
//                         .append(" AND 'Local Phone Account' = ("
//                                 + "select view_raw_contacts."
//                                 + ContactsContract.RawContacts.ACCOUNT_TYPE
//                                 + " from view_raw_contacts where view_raw_contacts._id = "
//                                 + "view_contacts.name_raw_contact_id) ) ");
//             } else {
//                 condition.append(INDICATE_PHONE_SIM + " ="
//                         + conditionList.get(0));
//             }
//             for (int i = 1; i < len; ++i) {
//                 if ("-1".equals(conditionList.get(i))) {
//                     condition.append(" OR (" + INDICATE_PHONE_SIM
//                             + " =" + conditionList.get(i));
//                     condition
//                             .append(" AND 'Local Phone Account' = ("
//                                     + "select view_raw_contacts."
//                                     + ContactsContract.RawContacts.ACCOUNT_TYPE
//                                     + " from view_raw_contacts where view_raw_contacts._id = "
//                                     + "view_contacts.name_raw_contact_id) ) ");
//                 } else {
//                     condition.append(" OR " + INDICATE_PHONE_SIM
//                             + " =" + conditionList.get(i));
//                 }
//
//             }
//            return condition.toString();
        }

        return null;
    }
}
