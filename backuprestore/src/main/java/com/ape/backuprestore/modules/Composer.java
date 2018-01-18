package com.ape.backuprestore.modules;


import android.content.Context;

import com.ape.backuprestore.ProgressReporter;
import com.ape.backuprestore.utils.Logger;

import java.util.List;

public abstract class Composer {
    private static final String TAG = "Composer";
    protected static final String RESTORE = "Restore";

    protected Context mContext;
    protected ProgressReporter mReporter;
    protected boolean mIsCancel = false;
    protected String mParentFolderPath;
    protected List<String> mParams;
    private int mComposeredCount = 0;

    public Composer(Context context) {
        mContext = context;
    }

    public void setParentFolderPath(String path) {
        mParentFolderPath = path;
    }

    public void setReporter(ProgressReporter reporter) {
        mReporter = reporter;
    }

    synchronized public boolean isCancel() {
        return mIsCancel;
    }

    synchronized public void setCancel(boolean cancel) {
        mIsCancel = cancel;
    }

    public int getComposed() {
        return mComposeredCount;
    }

    public void increaseComposed(boolean result) {
        if (result) {
            ++mComposeredCount;
        }

        if (mReporter != null) {
            mReporter.onOneFinished(this, result);
        }
    }

    public void onStart() {
        if (mReporter != null) {
            mReporter.onStart(this);
        }
    }

    public void onEnd() {
        if (mReporter != null) {
            boolean bResult = (getCount() == mComposeredCount && mComposeredCount > 0);
            mReporter.onEnd(this, bResult);
            Logger.d(TAG, "onEnd: result is " + bResult);
            Logger.d(TAG, "onEnd: getCount is " + getCount()
                    + ", and composed count is " + mComposeredCount);
        }
    }

    public void setParams(List<String> params) {
        mParams = params;
    }

    public boolean composeOneEntity() {
        boolean result = implementComposeOneEntity();
        if (result) {
            ++mComposeredCount;
        }

        if (mReporter != null) {
            mReporter.onOneFinished(this, result);
        }
        return result;
    }

    abstract public int getModuleType();

    abstract public int getCount();

    abstract public boolean isAfterLast();

    abstract public boolean init();

    abstract protected boolean implementComposeOneEntity();
}
