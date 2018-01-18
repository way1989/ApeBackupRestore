package com.ape.backuprestore.modules;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.ape.backuprestore.utils.Constants;
import com.ape.backuprestore.utils.Logger;
import com.ape.backuprestore.utils.ModuleType;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by android on 16-7-16.
 */
public class MessageRestoreComposer extends Composer {
    private static final String CLASS_TAG = Logger.LOG_TAG + "/MessageRestoreComposer";
    private List<Composer> mMessageComposers;
    private long mTime;

    public MessageRestoreComposer(Context context) {
        super(context);
        mMessageComposers = new ArrayList<Composer>();
        mMessageComposers.add(new SmsRestoreComposer(context));
        mMessageComposers.add(new MmsRestoreComposer(context));
    }

    // @Override
    // public void setZipFileName(String fileName) {
    //     super.setZipFileName(fileName);
    //     for (Composer composer : mMessageComposers) {
    //         composer.setZipFileName(fileName);
    //     }
    // }

    @Override
    public int getModuleType() {
        return ModuleType.TYPE_MESSAGE;
    }

    @Override
    public int getCount() {
        int count = 0;
        for (Composer composer : mMessageComposers) {
            if (composer != null) {
                count += composer.getCount();
            }
        }

        Logger.d(CLASS_TAG, "getCount():" + count);
        return count;
    }

    public boolean init() {
        boolean result = false;
        mTime = System.currentTimeMillis();
        for (Composer composer : mMessageComposers) {
            if (composer != null) {
                if (composer.init()) {
                    result = true;
                }
            }
        }

        Logger.d(CLASS_TAG, "init():" + result + ",count:" + getCount());
        return result;
    }

    @Override
    public boolean isAfterLast() {
        boolean result = true;
        for (Composer composer : mMessageComposers) {
            if (composer != null && !composer.isAfterLast()) {
                result = false;
                break;
            }
        }

        Logger.d(CLASS_TAG, "isAfterLast():" + result);
        return result;
    }

    @Override
    public boolean implementComposeOneEntity() {
        for (Composer composer : mMessageComposers) {
            if (composer != null && !composer.isAfterLast()) {
                return composer.composeOneEntity();
            }
        }

        return false;
    }

    private boolean deleteAllMessage() {
        boolean result = false;
        int count = 0;
        if (mContext != null) {
            Logger.d(CLASS_TAG, "begin delete:" + System.currentTimeMillis());
            count = mContext.getContentResolver().delete(Uri.parse(Constants.URI_MMS_SMS),
                    "date < ?", new String[]{Long.toString(mTime)});
            Logger.d(CLASS_TAG, "end delete:" + System.currentTimeMillis());

            result = true;
        }

        Logger.d(CLASS_TAG, "deleteAllMessage(),result" + result + "," + count + " deleted!");
        return result;
    }

    @Override
    public void onStart() {
        super.onStart();

        Logger.d(CLASS_TAG, "onStart()");
    }

    @Override
    public void onEnd() {
        super.onEnd();
        for (Composer composer : mMessageComposers) {
            if (composer != null) {
                composer.onEnd();
            }
        }
        Intent intent = new Intent();
        intent.setAction("com.mediatek.backuprestore.module.MessageRestoreComposer.RESTORE_END");
        Logger.d(CLASS_TAG, "message restore end,sendBroadcast to updata UI ");
        mContext.sendBroadcast(intent);
        Logger.d(CLASS_TAG, "onEnd()");
    }

    /**
     * Describe <code>setParentFolderPath</code> method here.
     *
     * @param path a <code>String</code> value
     */
    public final void setParentFolderPath(final String path) {
        mParentFolderPath = path;
        for (Composer composer : mMessageComposers) {
            composer.setParentFolderPath(path);
        }
    }

}
