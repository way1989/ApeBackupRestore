package com.ape.backuprestore.fragment;


import android.app.ProgressDialog;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.ape.backup.R;
import com.ape.backuprestore.BackupEngine;
import com.ape.backuprestore.BackupService;
import com.ape.backuprestore.PersonalItemData;
import com.ape.backuprestore.RecordXmlComposer;
import com.ape.backuprestore.RecordXmlInfo;
import com.ape.backuprestore.ResultDialog;
import com.ape.backuprestore.modules.Composer;
import com.ape.backuprestore.util.BackupUtil;
import com.ape.backuprestore.util.RxSchedulers;
import com.ape.backuprestore.utils.Constants;
import com.ape.backuprestore.utils.ModuleType;
import com.ape.backuprestore.utils.StorageUtils;
import com.ape.backuprestore.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.observers.DisposableObserver;

import static com.ape.backuprestore.utils.FileUtils.deleteFileOrFolder;

/**
 * Created by way on 2018/1/18.
 */

public class BackupFragment extends BaseFragment implements BackupService.OnBackupStatusListener {
    private static final String TAG = "BackupFragment";
    protected BackupService.BackupBinder mBackupService;

    private ServiceConnection mServiceCon = new ServiceConnection() {
        @Override
        public void onServiceConnected(final ComponentName name, final IBinder service) {
            Log.i(TAG, "onServiceConnected");
            mBackupService = (BackupService.BackupBinder) service;
            if (mBackupService != null)
                mBackupService.setOnBackupChangedListner(BackupFragment.this);
        }

        @Override
        public void onServiceDisconnected(final ComponentName name) {
            Log.i(TAG, "onServiceDisconnected");
            if (mBackupService != null)
                mBackupService.setOnBackupChangedListner(null);
            mBackupService = null;
        }
    };
    private String mBackupFolderPath;

    public BackupFragment() {
        // Required empty public constructor
    }

    public static BackupFragment newInstance() {
        BackupFragment fragment = new BackupFragment();
        return fragment;
    }

    @Override
    protected void start() {
        startBackup();
    }

    @Override
    protected void bindService() {
        getContext().bindService(new Intent(getContext(), BackupService.class), mServiceCon, Service.BIND_AUTO_CREATE);
    }

    @Override
    protected void unBindService() {
        if (mBackupService != null) {
            mBackupService.setOnBackupChangedListner(null);
        }
        try {
            getContext().unbindService(mServiceCon);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void getData(DisposableObserver<List<PersonalItemData>> observer) {
        BackupUtil.getPersonalItemDatas(getContext())
                .compose(RxSchedulers.<List<PersonalItemData>>io_main()).subscribe(observer);
    }

    @Override
    public int getLayoutId() {
        return R.layout.fragment_backup_restore;
    }

    @Override
    protected int getButtonStrRes() {
        return R.string.backup;
    }

    private void startBackup() {
        //SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        //String folderName = dateFormat.format(new Date(System.currentTimeMillis()));

        String path = StorageUtils.getInstance(getContext()).getBackupPath();
        if (path == null) {
            Toast.makeText(getContext(), "can not get the storage...", Toast.LENGTH_SHORT).show();
            return;
        }

        if (StorageUtils.getAvailableSize(path) <= StorageUtils.MINIMUM_SIZE) {
            // no space
            Log.d(TAG, "SDCard is full");
            Toast.makeText(getContext(), "storage is full...", Toast.LENGTH_SHORT).show();
            return;
        }

        mBackupFolderPath = path;
        Log.d(TAG, "[processClickStart] mBackupFolderPath is " + mBackupFolderPath);
        File folder = new File(mBackupFolderPath);
        File[] files = null;
        if (folder.exists()) {
            files = folder.listFiles();
        }
        if (files != null && files.length > 0) {
            Log.d(TAG, "[processClickStart] DLG_BACKUP_CONFIRM_OVERWRITE Here! ");
            for (File file : files) {
                deleteFileOrFolder(file);
            }
        }
        startPersonalDataBackup();
    }

    private void startPersonalDataBackup() {
        if (TextUtils.isEmpty(mBackupFolderPath)) {
            Toast.makeText(getContext(), "back path is null", Toast.LENGTH_SHORT).show();
            return;
        }

        final ArrayList<Integer> selectedItemList = getSelectedItemList();
        if (selectedItemList.isEmpty()) {
            Toast.makeText(getContext(), "Please select on or more items", Toast.LENGTH_SHORT).show();
            return;
        }

        startService();
        if (mBackupService != null) {
            mBackupService.setBackupModelList(selectedItemList);
            if (selectedItemList.contains(ModuleType.TYPE_MESSAGE)) {
                ArrayList<String> params = new ArrayList<>();
                params.add(Constants.ModulePath.NAME_SMS);
                params.add(Constants.ModulePath.NAME_MMS);
                mBackupService.setBackupItemParam(ModuleType.TYPE_MESSAGE, params);
            }
            boolean result = mBackupService.startBackup(mBackupFolderPath);
            if (!result) {
                String path = StorageUtils.getInstance(getContext()).getBackupPath();
                if (path == null) {
                    // no sdcard
                    Log.d(TAG, "SDCard is removed");
                } else if (StorageUtils.getAvailableSize(path) <= StorageUtils.MINIMUM_SIZE) {
                    // no space
                    Log.d(TAG, "SDCard is full");
                } else {
                    Log.e(TAG, "Unknown error");
                }
                stopService();
            }
        } else {
            stopService();
            Log.e(TAG, "startPersonalDataBackup: error! service is null");
        }
    }

    private ProgressDialog createProgressDlg() {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(getActivity());
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            mProgressDialog.setMessage(getString(R.string.backuping));
            mProgressDialog.setCancelable(true);
            mProgressDialog.setCanceledOnTouchOutside(false);
        }
        return mProgressDialog;
    }

    protected void startService() {
        getContext().startService(new Intent(getContext(), BackupService.class));
    }

    protected void stopService() {
        if (mBackupService != null) mBackupService.reset();
        getContext().stopService(new Intent(getContext(), BackupService.class));
    }

    @Override
    public void onComposerChanged(final Composer composer) {
        if (composer == null) {
            Log.e(TAG, "onComposerChanged: error[composer is null]");
            return;
        }
        Log.i(TAG, "onComposerChanged: type = " + composer.getModuleType() + "Max = " + composer.getCount());
        if (mBackupService == null || mBackupService.getState() == Constants.State.PAUSE) {
            Log.e(TAG, "onComposerChanged: error[mBackupService is null] or [state is pause]");
            return;
        }
        AndroidSchedulers.mainThread().scheduleDirect(new Runnable() {
            @Override
            public void run() {
                if (mProgressDialog == null) createProgressDlg();
                Log.d(TAG, "mProgressDialog : " + mProgressDialog);
                String msg = getString(R.string.backuping, ModuleType.getModuleStringFromType(
                        getContext(), composer.getModuleType()));
                mProgressDialog.setMessage(msg);
                mProgressDialog.setMax(composer.getCount());
                mProgressDialog.setProgress(0);
                try {
                    if (!mProgressDialog.isShowing()) mProgressDialog.show();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void onProgressChanged(Composer composer, final int progress) {
        AndroidSchedulers.mainThread().scheduleDirect(new Runnable() {
            @Override
            public void run() {
                if (mProgressDialog != null) {
                    mProgressDialog.setProgress(progress);
                }
            }
        });
    }

    @Override
    public void onBackupEnd(BackupEngine.BackupResultType resultCode, ArrayList<ResultDialog.ResultEntity> resultRecord) {
        if (resultCode != BackupEngine.BackupResultType.Cancel) {
            RecordXmlInfo backupInfo = new RecordXmlInfo();
            backupInfo.setRestore(false);
            backupInfo.setDevice(Utils.getPhoneSearialNumber());
            backupInfo.setTime(String.valueOf(System.currentTimeMillis()));
            RecordXmlComposer xmlComposer = new RecordXmlComposer();
            xmlComposer.startCompose();
            xmlComposer.addOneRecord(backupInfo);
            xmlComposer.endCompose();
            Log.i(TAG, "onBackupEnd.. write xml = " + xmlComposer.getXmlInfo() + ", dir = " + mBackupFolderPath);
            if (!TextUtils.isEmpty(mBackupFolderPath)) {
                Utils.writeToFile(xmlComposer.getXmlInfo(), mBackupFolderPath + File.separator
                        + Constants.RECORD_XML);
            }
        } else {
            Log.e(TAG, "ResultCode is cancel, not write record.xml");
        }
        AndroidSchedulers.mainThread().scheduleDirect(new Runnable() {
            @Override
            public void run() {
                if (mProgressDialog != null && mProgressDialog.isShowing()) {
                    mProgressDialog.dismiss();
                }
            }
        });
    }

    @Override
    public void onBackupErr(IOException e) {
        if (mBackupService != null && mBackupService.getState() != Constants.State.INIT
                && mBackupService.getState() != Constants.State.FINISH) {
            mBackupService.pauseBackup();
        }
    }

}
