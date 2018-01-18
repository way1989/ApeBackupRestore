package com.ape.backuprestore.modules;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.provider.ContactsContract;

import com.android.vcard.VCardConfig;
import com.android.vcard.VCardEntry;
import com.android.vcard.VCardEntryCommitter;
import com.android.vcard.VCardEntryConstructor;
import com.android.vcard.VCardInterpreter;
import com.android.vcard.VCardParser;
import com.android.vcard.VCardParser_V21;
import com.android.vcard.VCardParser_V30;
import com.android.vcard.exception.VCardException;
import com.android.vcard.exception.VCardNestedException;
import com.android.vcard.exception.VCardNotSupportedException;
import com.android.vcard.exception.VCardVersionException;
import com.ape.backuprestore.utils.Constants;
import com.ape.backuprestore.utils.Logger;
import com.ape.backuprestore.utils.ModuleType;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by android on 16-7-16.
 */
public class ContactRestoreComposer extends Composer {
    private static final String CLASS_TAG = Logger.LOG_TAG + "/ContactRestoreComposer";
    private int mIndex;
    private int mCount;
    private InputStream mInputStream;

    public ContactRestoreComposer(Context context) {
        super(context);
    }

    public int getModuleType() {
        return ModuleType.TYPE_CONTACT;
    }

    public int getCount() {
        Logger.d(CLASS_TAG, "getCount():" + mCount);
        return mCount;
    }

    public boolean init() {
        boolean result = false;
        Logger.d(CLASS_TAG, "begin init:" + System.currentTimeMillis());
        try {
            mCount = getContactCount();
            result = true;
        } catch (Exception e) {
            Logger.d(CLASS_TAG, "Exception");
        }

        Logger.d(CLASS_TAG, "end init:" + System.currentTimeMillis());
        Logger.d(CLASS_TAG, "init():" + result + ",count:" + mCount);
        return result;
    }

    /**
     * @return boolean result
     */
    public boolean isAfterLast() {
        boolean result = (mIndex >= mCount) ? true : false;
        Logger.d(CLASS_TAG, "isAfterLast():" + result);
        return result;
    }

    /**
     * @return boolean
     */
    public boolean composeOneEntity() {
        return implementComposeOneEntity();
    }

    /**
     * @return boolean
     */
    public boolean implementComposeOneEntity() {
        boolean result = false;

        ++mIndex;
        if (mIndex == 1) {
            if (mInputStream != null) {
                Account account = new Account("Phone", "Local Phone Account");
                final VCardEntryConstructor constructor =
                        new VCardEntryConstructor(VCardConfig.VCARD_TYPE_V21_GENERIC, account);
                final RestoreVCardEntryCommitter committer =
                        new RestoreVCardEntryCommitter(mContext.getContentResolver());
                constructor.addEntryHandler(committer);
                final int[] possibleVCardVersions = new int[]{
                        VCardConfig.VCARD_TYPE_V21_GENERIC,
                        VCardConfig.VCARD_TYPE_V30_GENERIC
                };
                result = readOneVCard(
                        mInputStream,
                        VCardConfig.VCARD_TYPE_V21_GENERIC,
                        constructor,
                        committer,
                        possibleVCardVersions);
            }
        } else {
            result = true;
        }

        Logger.d(CLASS_TAG, "implementComposeOneEntity()" + ",result:" + result);
        return result;
    }

    private boolean deleteAllContact() {
        if (mContext != null) {
            Logger.d(CLASS_TAG, "begin delete:" + System.currentTimeMillis());

            int count = mContext.getContentResolver().delete(
                    Uri.parse(ContactsContract.RawContacts.CONTENT_URI.toString() + "?"
                            + ContactsContract.CALLER_IS_SYNCADAPTER + "=true"),
                    ContactsContract.RawContacts._ID + ">0", null);

            Logger.d(CLASS_TAG, "end delete:" + System.currentTimeMillis());

            Logger.d(CLASS_TAG, "deleteAllContact()," + count + " records deleted!");

            return true;
        }

        return false;
    }

    private boolean readOneVCard(InputStream is,
                                 int vcardType,
                                 final VCardInterpreter interpreter,
                                 final RestoreVCardEntryCommitter committer,
                                 final int[] possibleVCardVersions) {
        boolean successful = false;
        final int length = possibleVCardVersions.length;
        VCardParser vcardParser;

        for (int i = 0; i < length; i++) {
            final int vcardVersion = possibleVCardVersions[i];
            try {
                if (i > 0 && (interpreter instanceof VCardEntryConstructor)) {
                    // Let the object clean up internal temporary objects,
                    ((VCardEntryConstructor) interpreter).clear();
                }

                // We need synchronized block here,
                // since we need to handle mCanceled and mVCardParser at once.
                // In the worst case, a user may call cancel() just before
                // creating
                // mVCardParser.
                synchronized (this) {
                    vcardParser =
                            (vcardVersion == VCardConfig.VCARD_TYPE_V21_GENERIC) ?
                                    new VCardParser_V21(vcardType) :
                                    new VCardParser_V30(vcardType);
                    committer.setParser(vcardParser);
                }

                vcardParser.parse(is, interpreter);
                successful = true;
                break;
            } catch (IOException e) {
                e.printStackTrace();
            } catch (VCardNestedException e) {
                e.printStackTrace();
            } catch (VCardNotSupportedException e) {
                e.printStackTrace();
            } catch (VCardVersionException e) {
                e.printStackTrace();
            } catch (VCardException e) {
                e.printStackTrace();
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        Logger.d(CLASS_TAG, "IOException");
                    }
                }
            }
        }

        Logger.d(CLASS_TAG, "readOneVCard() " + successful);
        return successful;
    }

    /**
     * return .
     */
    public void onStart() {
        super.onStart();
        // deleteAllContact();
        try {
            String fileName = mParentFolderPath + File.separator +
                    Constants.ModulePath.FOLDER_CONTACT + File.separator + Constants.ModulePath.NAME_CONTACT;
            mInputStream = new FileInputStream(fileName);
        } catch (Exception e) {
            mInputStream = null;
        }

        Logger.d(CLASS_TAG, " onStart()");
    }

    /**
     * return .
     */
    public void onEnd() {
        super.onEnd();
        if (mInputStream != null) {
            try {
                mInputStream.close();
            } catch (IOException e) {
                Logger.d(CLASS_TAG, "IOException");
            }
        }

        Logger.d(CLASS_TAG, " onEnd()");
    }

    private int getContactCount() {
        int count = 0;
        try {
            String fileName = mParentFolderPath + File.separator +
                    Constants.ModulePath.FOLDER_CONTACT + File.separator + Constants.ModulePath.NAME_CONTACT;
            InputStream instream = new FileInputStream(fileName);
            InputStreamReader inreader = new InputStreamReader(instream);
            BufferedReader buffreader = new BufferedReader(inreader);
            String line = null;
            while ((line = buffreader.readLine()) != null) {
                if (line.contains("END:VCARD")) {
                    ++count;
                }
            }
            instream.close();
        } catch (IOException e) {
            Logger.d(CLASS_TAG, "IOException");
        }

        return count;
    }

    /**
     * Describe class <code>RestoreVCardEntryCommitter</code> here.
     */
    private class RestoreVCardEntryCommitter extends VCardEntryCommitter {

        private VCardParser mParser = null;

        /**
         * Creates a new <code>RestoreVCardEntryCommitter</code> instance.
         *
         * @param resolver a <code>ContentResolver</code> value
         */
        public RestoreVCardEntryCommitter(ContentResolver resolver) {
            super(resolver);
        }

        public void setParser(VCardParser parser) {
            mParser = parser;
        }

        /**
         * Describe <code>onEntryCreated</code> method here.
         *
         * @param vcardEntry a <code>VCardEntry</code> value
         */
        public void onEntryCreated(final VCardEntry vcardEntry) {
            if (ContactRestoreComposer.this.isCancel()) {
                if (mParser != null) {
                    mParser.cancel();
                    Logger.d(CLASS_TAG, "Cancel in RestoreVCardEntryCommitter");
                } else {
                    Logger.d(
                            CLASS_TAG,
                            "mParser is null. Cannot cancel in RestoreVCardEntryCommitter");
                }
            } else {
                super.onEntryCreated(vcardEntry);
                increaseComposed(true);
            }
        }
    }
}
