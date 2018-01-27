package com.ape.backuprestore;

import android.app.Notification;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Binder;
import android.os.IBinder;
import android.widget.Toast;

import com.ape.backuprestore.modules.Composer;
import com.ape.backuprestore.utils.Constants;
import com.ape.backuprestore.utils.Logger;
import com.ape.backuprestore.utils.ModuleType;
import com.ape.backuprestore.utils.NotifyManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;


/**
 * @author way
 */
public class BackupService extends Service implements ProgressReporter, BackupEngine.OnBackupDoneListener {
    private static final String TAG = "BackupService";
    HashMap<Integer, ArrayList<String>> mParasMap = new HashMap<>();
    NewDataNotifyReceiver mNotificationReceiver = null;
    private BackupBinder mBinder = new BackupBinder();
    private int mState;
    private BackupEngine mBackupEngine;
    private ArrayList<ResultDialog.ResultEntity> mResultList;
    private BackupProgress mCurrentProgress = new BackupProgress();
    private OnBackupStatusListener mStatusListener;
    private BackupEngine.BackupResultType mResultType;
    private boolean mComposerResult = true;

    @Override
    public IBinder onBind(Intent intent) {
        Logger.i(TAG, "onBind");
        return mBinder;
    }

    /**
     * onUnbind.
     *
     * @return boolean
     */
    public boolean onUnbind(Intent intent) {
        super.onUnbind(intent);
        Logger.i(TAG, "onUnbind");
        return true;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mState = Constants.State.INIT;
        Logger.i(TAG, "onCreate");
        mNotificationReceiver = new NewDataNotifyReceiver();
        IntentFilter filter = new IntentFilter(Constants.ACTION_NEW_DATA_DETECTED);
        filter.setPriority(1000);
        registerReceiver(mNotificationReceiver, filter);
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Logger.i(TAG, "onStartCommand");
        return START_NOT_STICKY;
    }

    /**
     * onRebind.
     */
    public void onRebind(Intent intent) {
        super.onRebind(intent);
        Logger.i(TAG, "onRebind");
    }

    /**
     * onDestroy.
     */
    public void onDestroy() {
        super.onDestroy();
        stopForeground(true);
        Logger.i(TAG, "onDestroy");
        if (mBackupEngine != null && mBackupEngine.isRunning()) {
            mBackupEngine.setOnBackupDoneListener(null);
            mBackupEngine.cancel();
        }

        if (mNotificationReceiver != null) {
            unregisterReceiver(mNotificationReceiver);
            mNotificationReceiver = null;
        }
    }

    @Override
    public void onStart(Composer composer) {
        mCurrentProgress.mComposer = composer;
        mCurrentProgress.mType = composer.getModuleType();
        mCurrentProgress.mMax = composer.getCount();
        mCurrentProgress.mCurNum = 0;
        if (mStatusListener != null) {
            mStatusListener.onComposerChanged(composer);
        }
        if (mCurrentProgress.mMax != 0) {
            NotifyManager.getInstance(BackupService.this).setMaxPercent(mCurrentProgress.mMax);
        }
    }

    @Override
    public void onOneFinished(Composer composer, boolean result) {
        mCurrentProgress.mCurNum++;
        if (mStatusListener != null) {
            mStatusListener.onProgressChanged(composer, mCurrentProgress.mCurNum);
        }

        if (mCurrentProgress.mMax != 0) {
            NotifyManager.getInstance(BackupService.this).showBackupNotification(
                    ModuleType.getModuleStringFromType(this, composer.getModuleType()),
                    composer.getModuleType(), mCurrentProgress.mCurNum);
        }
    }

    @Override
    public void onEnd(Composer composer, boolean result) {
        int resultType = ResultDialog.ResultEntity.SUCCESS;
        if (mResultList == null) {
            mResultList = new ArrayList<>();
        }
        if (!result) {
            if (composer.getCount() == 0) {
                resultType = ResultDialog.ResultEntity.NO_CONTENT;
            } else {
                resultType = ResultDialog.ResultEntity.FAIL;
                mComposerResult = false;
            }
        }
        Logger.d(TAG, "one Composer end: type = " + composer.getModuleType()
                + ", result = " + resultType);
        ResultDialog.ResultEntity item = new ResultDialog.ResultEntity(composer.getModuleType(), resultType);
        mResultList.add(item);
    }

    @Override
    public void onErr(IOException e) {
        Logger.d(TAG, "onErr " + e.getMessage());
        if (mStatusListener != null) {
            mStatusListener.onBackupErr(e);
        }
    }

    @Override
    public void onFinishBackup(BackupEngine.BackupResultType result) {
        Logger.d(TAG, "onFinishBackup result = " + result);
        mResultType = result;
        if (mStatusListener != null) {
            if (mState == Constants.State.CANCELLING) {
                result = BackupEngine.BackupResultType.Cancel;
            }
            if (result != BackupEngine.BackupResultType.Success && result != BackupEngine.BackupResultType.Cancel) {
                for (ResultDialog.ResultEntity item : mResultList) {
                    if (item.getResult() == ResultDialog.ResultEntity.SUCCESS) {
                        item.setResult(ResultDialog.ResultEntity.FAIL);
                    }
                }
            }
            mState = Constants.State.FINISH;
            mStatusListener.onBackupEnd(result, mResultList);
        } else {
            mState = Constants.State.FINISH;
        }
        if (mResultType != BackupEngine.BackupResultType.Cancel && mComposerResult) {
            NotifyManager.getInstance(BackupService.this).showFinishNotification(
                    NotifyManager.NOTIFY_BACKUPING, true);
        } else if (!mComposerResult) {
            NotifyManager.getInstance(BackupService.this).showFinishNotification(
                    NotifyManager.NOTIFY_BACKUPING, false);
            mComposerResult = true;
        } else {
            NotifyManager.getInstance(BackupService.this).clearNotification();
        }
        Intent intent = new Intent(Constants.ACTION_SCAN_DATA);
        this.sendBroadcast(intent);
    }

    private void stayForeground() {
        Notification notification = new Notification.Builder(this)
                .setSmallIcon(R.drawable.ic_application_am)
                .build();
        //notification.flags |= Notification.FLAG_HIDE_NOTIFICATION;
        startForeground(1, notification);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Logger.d(TAG, "onConfigurationChanged: setRefreshFlag");
        NotifyManager.getInstance(this).setRefreshFlag();
    }

    /**
     * @author way
     */
    public interface OnBackupStatusListener {

        /**
         * @param composer
         */
        void onComposerChanged(Composer composer);

        /**
         * @param composer composer
         * @param progress progress
         */
        void onProgressChanged(Composer composer, int progress);

        /**
         * @param resultCode   resultCode
         * @param resultRecord resultRecord
         */
        void onBackupEnd(final BackupEngine.BackupResultType resultCode,
                         final ArrayList<ResultDialog.ResultEntity> resultRecord);

        /**
         * @param e IOException
         */
        void onBackupErr(IOException e);
    }

    /**
     * @author way
     */
    public static class BackupProgress {
        Composer mComposer;
        int mType;
        int mMax;
        int mCurNum;
    }

    /**
     * @author way
     */
    public class BackupBinder extends Binder {
        public int getState() {
            Logger.i(TAG, "getState: " + mState);
            return mState;
        }

        /**
         * @param list list
         */
        public void setBackupModelList(ArrayList<Integer> list) {
            reset();
            if (mBackupEngine == null)
                mBackupEngine = BackupEngine.getInstance(getApplicationContext(), BackupService.this);

            mBackupEngine.setBackupModelList(list);
        }

        /**
         * @param itemType type
         * @param paraList list
         */
        public void setBackupItemParam(int itemType, ArrayList<String> paraList) {
            Logger.d(
                    TAG,
                    "Param List Size is " + (paraList == null ? 0 : paraList.size()));
            mParasMap.put(itemType, paraList);
            mBackupEngine.setBackupItemParam(itemType, paraList);
        }

        /**
         * @param itemType type
         * @return ArrayList<String>
         */
        public ArrayList<String> getBackupItemParam(int itemType) {
            return mParasMap.get(itemType);
        }

        /**
         * @param folderName folder
         * @return boolean
         */
        public boolean startBackup(String folderName) {
            stayForeground();
            boolean ret;
            mBackupEngine.setOnBackupDoneListener(BackupService.this);
            ret = mBackupEngine.startBackup(folderName);
            if (ret) {
                mState = Constants.State.RUNNING;
            } else {
                mBackupEngine.setOnBackupDoneListener(null);
            }
            Logger.d(TAG, "startBackup: " + ret);
            return ret;
        }

        /**
         * stopForeground.
         */
        public void stopForeground() {
            BackupService.this.stopForeground(true);
            Logger.d(TAG, "stopFreground");
        }

        /**
         * pauseBackup.
         */
        public void pauseBackup() {
            mState = Constants.State.PAUSE;
            if (mBackupEngine != null) {
                mBackupEngine.pause();
            }
            Logger.d(TAG, "pauseBackup");
        }

        /**
         * cancelBackup.
         */
        public void cancelBackup() {
            mState = Constants.State.CANCELLING;
            if (mBackupEngine != null) {
                mBackupEngine.cancel();
            }
            Logger.d(TAG, "cancelBackup");
        }

        /**
         * continueBackup.
         */
        public void continueBackup() {
            mState = Constants.State.RUNNING;
            if (mBackupEngine != null) {
                mBackupEngine.continueBackup();
            }
            Logger.d(TAG, "continueBackup");
        }

        /**
         * reset.
         */
        public void reset() {
            mState = Constants.State.INIT;
            if (mResultList != null) {
                mResultList.clear();
            }
            if (mParasMap != null) {
                mParasMap.clear();
            }
        }

        public BackupProgress getCurBackupProgress() {
            return mCurrentProgress;
        }

        public void setOnBackupChangedListner(OnBackupStatusListener listener) {
            mStatusListener = listener;
        }

        public ArrayList<ResultDialog.ResultEntity> getBackupResult() {
            return mResultList;
        }

        public BackupEngine.BackupResultType getBackupResultType() {
            return mResultType;
        }

    }

    /**
     * @author way
     */
    class NewDataNotifyReceiver extends BroadcastReceiver {
        public static final String CLASS_TAG = "NotificationReceiver";

        @Override
        public void onReceive(Context context, Intent intent) {
            if (Constants.ACTION_NEW_DATA_DETECTED.equals(intent.getAction())) {
                Logger.d(CLASS_TAG, "BackupService ------>ACTION_NEW_DATA_DETECTED received");
                int type = intent.getIntExtra(Constants.NOTIFY_TYPE, 0);
                String folder = intent.getStringExtra(Constants.FILENAME);
                if (mBackupEngine != null && mBackupEngine.isRunning()) {
                    NotifyManager.getInstance(context).showNewDetectionNotification(type, folder);
                    Toast.makeText(getApplicationContext(),
                            getString(R.string.backup_running_toast), Toast.LENGTH_SHORT).show();
                    abortBroadcast();
                }
            }
        }

    }
}
