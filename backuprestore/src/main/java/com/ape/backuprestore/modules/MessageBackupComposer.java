package com.ape.backuprestore.modules;

import android.content.Context;

import com.ape.backuprestore.utils.Constants;
import com.ape.backuprestore.utils.Logger;
import com.ape.backuprestore.utils.ModuleType;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by android on 16-7-16.
 */
public class MessageBackupComposer extends Composer {
    private static final String CLASS_TAG = Logger.LOG_TAG + "/MessageBackupComposer";
    private List<Composer> mMessageComposers;

    public MessageBackupComposer(Context context) {
        super(context);
        mContext = context;
        mMessageComposers = new ArrayList<>();
    }

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

    @Override
    public boolean init() {
        boolean result = true;
        for (Composer composer : mMessageComposers) {
            if (composer != null) {
                if (!composer.init()) {
                    result = false;
                }
            }
        }

        Logger.d(CLASS_TAG, "init():" + result);
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
        boolean result = false;
        for (Composer composer : mMessageComposers) {
            if (composer != null && !composer.isAfterLast()) {
                return composer.composeOneEntity();
            }
        }

        return result;
    }

    public int getComposed(int type) {
        int count = 0;
        for (Composer composer : mMessageComposers) {
            if (composer != null && composer.getModuleType() == type) {
                count = composer.getComposed();
                break;
            }
        }

        return count;
    }

    @Override
    public void onStart() {
        super.onStart();
        for (Composer composer : mMessageComposers) {
            if (composer != null) {
                composer.onStart();
            }
        }
    }

    @Override
    public void onEnd() {
        super.onEnd();
        for (Composer composer : mMessageComposers) {
            if (composer != null) {
                composer.onEnd();
            }
        }
    }

    /**
     * Describe <code>setParentFolderPath</code> method here.
     *
     * @param path a <code>String</code> value
     */
    public final void setParentFolderPath(final String path) {
        if (mParams != null && mParams.size() > 0) {
            if (mParams.contains(Constants.ModulePath.NAME_SMS)) {
                mMessageComposers.add(new SmsBackupComposer(mContext));
            }
            if (mParams.contains(Constants.ModulePath.NAME_MMS)) {
                mMessageComposers.add(new MmsBackupComposer(mContext));
            }
        }
        mParentFolderPath = path;
        for (Composer composer : mMessageComposers) {
            composer.setParentFolderPath(path);
        }
    }
}
