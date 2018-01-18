package com.ape.backuprestore;

import android.content.Context;
import android.widget.Toast;

import com.ape.backuprestore.modules.AppBackupComposer;
import com.ape.backuprestore.modules.CalendarBackupComposer;
import com.ape.backuprestore.modules.CallLogBackupComposer;
import com.ape.backuprestore.modules.Composer;
import com.ape.backuprestore.modules.ContactBackupComposer;
import com.ape.backuprestore.modules.MessageBackupComposer;
import com.ape.backuprestore.modules.MmsBackupComposer;
import com.ape.backuprestore.modules.MusicBackupComposer;
import com.ape.backuprestore.modules.NoteBookBackupComposer;
import com.ape.backuprestore.modules.PictureBackupComposer;
import com.ape.backuprestore.modules.SmsBackupComposer;
import com.ape.backuprestore.utils.Constants;
import com.ape.backuprestore.utils.FileUtils;
import com.ape.backuprestore.utils.Logger;
import com.ape.backuprestore.utils.ModuleType;
import com.ape.backuprestore.utils.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class BackupEngine {
    private static final String TAG = "BackupEngine";
    private static BackupEngine mSelfInstance;
    private final Object mLock = new Object();
    private HashMap<Integer, ArrayList<String>> mParasMap = new HashMap<>();
    private ArrayList<Integer> mModuleList;
    private Context mContext;
    private ProgressReporter mProgressReporter;
    private List<Composer> mComposerList;
    private OnBackupDoneListener mBackupDoneListener;
    private boolean mIsRunning = false;
    private long mThreadIdentifier = -1;
    private boolean mIsPause = false;
    private boolean mIsCancel = false;
    private String mBackupFolder;

    private BackupEngine(final Context context, final ProgressReporter reporter) {
        mContext = context;
        mProgressReporter = reporter;
        mComposerList = new ArrayList<>();
        mSelfInstance = this;
    }

    public static BackupEngine getInstance(final Context context, final ProgressReporter reporter) {
        if (mSelfInstance == null) {
            new BackupEngine(context, reporter);
        } else {
            mSelfInstance.updateInfo(context, reporter);
        }

        return mSelfInstance;
    }

    void setBackupModelList(ArrayList<Integer> moduleList) {
        reset();
        mModuleList = moduleList;
    }

    void setBackupItemParam(int itemType, ArrayList<String> paraList) {
        mParasMap.put(itemType, paraList);
    }

    boolean startBackup(final String folderName) {
        boolean startSuccess = true;
        mBackupFolder = folderName;
        Logger.d(TAG, "startBackup():" + folderName);

        Utils.isBackingUp = mIsRunning = true;
        if (setupComposer(mModuleList)) {
            mThreadIdentifier = System.currentTimeMillis();
            new BackupThread(mThreadIdentifier).start();
        } else {
            Utils.isBackingUp = mIsRunning = false;
            startSuccess = false;
        }
        return startSuccess;
    }

    final boolean isRunning() {
        return mIsRunning;
    }

    private void updateInfo(final Context context, final ProgressReporter reporter) {
        mContext = context;
        mProgressReporter = reporter;
    }

    public final void pause() {
        mIsPause = true;
    }

    public final boolean isPaused() {
        return mIsPause;
    }

    final void continueBackup() {
        synchronized (mLock) {
            if (mIsPause) {
                mIsPause = false;
                mLock.notify();
            }
        }
    }

    public final void cancel() {
        if (mComposerList != null && mComposerList.size() > 0) {
            for (Composer composer : mComposerList) {
                composer.setCancel(true);
            }
            mIsCancel = true;
            continueBackup();
        }
    }

    final void setOnBackupDoneListener(final OnBackupDoneListener listener) {
        mBackupDoneListener = listener;
    }

    private void addComposer(final Composer composer) {
        if (composer != null) {
            int type = composer.getModuleType();
            ArrayList<String> params = mParasMap.get(type);
            if (params != null) {
                Logger.d(TAG, "Params size is " + params);
                composer.setParams(params);
            } else {
                Logger.d(TAG, "Params is null");
            }
            composer.setReporter(mProgressReporter);
            composer.setParentFolderPath(mBackupFolder);
            mComposerList.add(composer);
        }
    }

    private void reset() {
        if (mComposerList != null) {
            mComposerList.clear();
        }

        if (mParasMap != null) {
            mParasMap.clear();
        }

        mIsPause = false;
        mIsCancel = false;
    }

    private boolean setupComposer(final ArrayList<Integer> list) {
        Logger.d(TAG, "setupComposer begin...");

        boolean result = true;
        File path = new File(mBackupFolder);
        if (!path.exists()) {
            result = path.mkdirs();
        }
        Logger.d(TAG, "makedir end...");
        if (result) {
            Logger.d(TAG, "create folder " + mBackupFolder + " success");

            for (int type : list) {
                switch (type) {
                    case ModuleType.TYPE_CONTACT:
                        addComposer(new ContactBackupComposer(mContext));
                        break;

                    case ModuleType.TYPE_CALENDAR:
                        addComposer(new CalendarBackupComposer(mContext));
                        break;

                    case ModuleType.TYPE_SMS:
                        addComposer(new SmsBackupComposer(mContext));
                        break;

                    case ModuleType.TYPE_MMS:
                        addComposer(new MmsBackupComposer(mContext));
                        break;

                    case ModuleType.TYPE_MESSAGE:
                        addComposer(new MessageBackupComposer(mContext));
                        break;

                    case ModuleType.TYPE_APP:
                        addComposer(new AppBackupComposer(mContext));
                        break;

                    case ModuleType.TYPE_PICTURE:
                        addComposer(new PictureBackupComposer(mContext));
                        break;

                    case ModuleType.TYPE_MUSIC:
                        addComposer(new MusicBackupComposer(mContext));
                        break;

                    case ModuleType.TYPE_NOTEBOOK:
                        addComposer(new NoteBookBackupComposer(mContext));
                        break;

                    case ModuleType.TYPE_CALL_LOG:
                        addComposer(new CallLogBackupComposer(mContext));
                        break;

                    default:
                        result = false;
                        break;
                }
            }

            Logger.d(TAG, "setupComposer finish");
        } else {
            Logger.e(TAG, "setupComposer failed");
            result = false;
        }
        return result;
    }

    private void deleteFolder(File file) {
        if (file.exists()) {
            if (file.isFile()) {
                file.delete();
            } else if (file.isDirectory()) {
                File files[] = file.listFiles();
                for (File file1 : files) {
                    this.deleteFolder(file1);
                }
            }

            file.delete();
        }
    }

    public enum BackupResultType {
        Success, Fail, Error, Cancel
    }

    interface OnBackupDoneListener {
        void onFinishBackup(BackupResultType result);
    }

    private class BackupThread extends Thread {
        private final long mId;

        BackupThread(long id) {
            super();
            mId = id;
        }

        @Override
        public void run() {
            try {
                BackupResultType result;

                Logger.d(TAG, "BackupThread begin...");
                for (Composer composer : mComposerList) {
                    Logger.d(TAG, "BackupThread->composer:" + composer.getModuleType()
                            + " start...");
                    if (!composer.isCancel() && mId == mThreadIdentifier) {
                        composer.init();
                        composer.onStart();
                        Logger.d(TAG, "BackupThread->composer:" + composer.getModuleType()
                                + " init finish");
                        while (!composer.isAfterLast() && !composer.isCancel() &&
                                mId == mThreadIdentifier) {
                            if (mIsPause) {
                                synchronized (mLock) {
                                    try {
                                        Logger.d(TAG, "BackupThread wait...");
                                        while (mIsPause) {
                                            mLock.wait();
                                        }
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                            if (!composer.isCancel()) {
                                composer.composeOneEntity();
                                Logger.d(TAG, "BackupThread->composer:"
                                        + composer.getModuleType() + " compose one entiry");
                            }
                        }
                    }

                    try {
                        sleep(Constants.TIME_SLEEP_WHEN_COMPOSE_ONE);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    composer.onEnd();

                    // send scan file request (Refactoring)
                    sendScanFileRequests(composer.getModuleType(), mContext);

                    // generateModleXmlInfo(composer);

                    Logger.d(TAG,
                            "BackupThread-> composer:" + composer.getModuleType() + " finish");
                }

                Utils.isBackingUp = mIsRunning = false;
                if (mIsCancel) {
                    result = BackupResultType.Cancel;
                    if (!mModuleList.contains(ModuleType.TYPE_APP)) {
                        try {
                            deleteFolder(new File(mBackupFolder));
                        } catch (NullPointerException e) {
                            e.fillInStackTrace();
                        }
                    }
                } else {
                    result = BackupResultType.Success;
                }
                Logger.d(TAG, "BackupThread run finish, result:" + result);

                if (mBackupDoneListener != null) {
                    if (mIsPause) {
                        synchronized (mLock) {
                            try {
                                Logger.d(TAG, "BackupThread wait before end...");
                                while (mIsPause) {
                                    mLock.wait();
                                }
                                if (mIsCancel) {
                                    result = BackupResultType.Cancel;
                                    if (!mModuleList.contains(ModuleType.TYPE_APP)) {
                                        try {
                                            if (new File(mBackupFolder).exists()) {
                                                deleteFolder(new File(mBackupFolder));
                                            }
                                        } catch (NullPointerException e) {
                                            e.fillInStackTrace();
                                        }
                                    }
                                }
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    mBackupDoneListener.onFinishBackup(result);
                }
            } catch (SecurityException e) {
                e.printStackTrace();
                Toast.makeText(mContext, R.string.permission_not_satisfied_exit,
                        Toast.LENGTH_SHORT).show();
                android.os.Process.killProcess(android.os.Process.myPid());
            }
        }

        private void sendScanFileRequests(int moduleType, Context mContext) {
            String backupFolderName = ModuleType.sModuleTypeFolderInfo.get(moduleType);
            if (moduleType == ModuleType.TYPE_APP) {
                FileUtils.scanPathforMediaStore(mBackupFolder, mContext);
            } else if (backupFolderName != null) {
                String path = mBackupFolder + File.separator + backupFolderName;
                FileUtils.scanPathforMediaStore(path, mContext);
            }
        }
    }

}
