package com.ape.backuprestore.fragment;


import android.app.ProgressDialog;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.text.TextUtils;
import android.view.WindowManager;
import android.widget.Toast;

import com.ape.backup.R;
import com.ape.backuprestore.PersonalItemData;
import com.ape.backuprestore.RecordXmlComposer;
import com.ape.backuprestore.RecordXmlInfo;
import com.ape.backuprestore.RecordXmlParser;
import com.ape.backuprestore.RestoreService;
import com.ape.backuprestore.ResultDialog;
import com.ape.backuprestore.modules.Composer;
import com.ape.backuprestore.util.RestoreUtil;
import com.ape.backuprestore.util.RxSchedulers;
import com.ape.backuprestore.utils.BackupFilePreview;
import com.ape.backuprestore.utils.Constants;
import com.ape.backuprestore.utils.Logger;
import com.ape.backuprestore.utils.ModuleType;
import com.ape.backuprestore.utils.StorageUtils;
import com.ape.backuprestore.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.observers.DisposableObserver;

/**
 * Created by way on 2018/1/18.
 */

public class RestoreFragment extends BaseFragment implements RestoreService.OnRestoreStatusListener {
    private static final String TAG = "RestoreFragment";
    protected RestoreService.RestoreBinder mRestoreService;

    ServiceConnection mServiceCon = new ServiceConnection() {

        public void onServiceConnected(ComponentName name, IBinder service) {
            Logger.i(TAG, " onServiceConnected");
            mRestoreService = (RestoreService.RestoreBinder) service;
            if (mRestoreService != null) {
                mRestoreService.setOnRestoreChangedListner(RestoreFragment.this);
            }

        }

        public void onServiceDisconnected(ComponentName name) {
            Logger.i(TAG, " onServiceDisconnected");
            if (mRestoreService != null) {
                mRestoreService.setOnRestoreChangedListner(null);
            }
            mRestoreService = null;
        }
    };

    private String mRestoreFolderPath;

    public RestoreFragment() {
        // Required empty public constructor
    }

    public static RestoreFragment newInstance() {
        RestoreFragment fragment = new RestoreFragment();
        return fragment;
    }

    @Override
    protected void start() {
        startRestore();
    }

    @Override
    protected void bindService() {
        getContext().bindService(new Intent(getContext(), RestoreService.class), mServiceCon, Service.BIND_AUTO_CREATE);
    }

    @Override
    protected void unBindService() {
        if (mRestoreService != null) {
            mRestoreService.setOnRestoreChangedListner(null);
        }
        try {
            getContext().unbindService(mServiceCon);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void getData(DisposableObserver<List<PersonalItemData>> observer) {
        RestoreUtil.getPersonalItemDatas(getContext())
                .compose(RxSchedulers.<List<PersonalItemData>>io_main()).subscribe(observer);
    }

    @Override
    public int getLayoutId() {
        return R.layout.fragment_backup_restore;
    }

    @Override
    protected int getButtonStrRes() {
        return R.string.restore;
    }


    public void startRestore() {
        String path = StorageUtils.getInstance(getContext()).getBackupPath();
        if (path == null) {
            Toast.makeText(getContext(), "can not get the storage...", Toast.LENGTH_SHORT).show();
            return;
        }
        mRestoreFolderPath = path;
        if (!isCanStartRestore()) {
            return;
        }
        final ArrayList<Integer> selectedItemList = getSelectedItemList();
        if (selectedItemList.isEmpty()) {
            Toast.makeText(getContext(), "Please select on or more items", Toast.LENGTH_SHORT).show();
            return;
        }
        startService();
        Logger.d(TAG, "startRestore");

        mRestoreService.setRestoreModelList(selectedItemList);
        boolean ret = mRestoreService.startRestore(mRestoreFolderPath);
        if (ret) {
            int count = BackupFilePreview.getInstance().getItemCount(selectedItemList.get(0));
            int type = selectedItemList.get(0);
            showProgressDialog(count, type);
        } else {
            stopService();
        }
    }

    protected void startService() {
        getContext().startService(new Intent(getContext(), RestoreService.class));
    }

    protected void stopService() {
        if (mRestoreService != null) {
            mRestoreService.reset();
        }
        getContext().stopService(new Intent(getContext(), RestoreService.class));
    }

    private void showProgressDialog(int count, int type) {
        if (mProgressDialog == null) {
            mProgressDialog = createProgressDlg();
        }
        mProgressDialog.setMessage(getString(R.string.restoring,
                ModuleType.getModuleStringFromType(getContext(), type)));
        mProgressDialog.setMax(count);
        mProgressDialog.setProgress(0);
        try {
            if (!mProgressDialog.isShowing())
                mProgressDialog.show();
        } catch (WindowManager.BadTokenException e) {
            Logger.e(TAG, " BadTokenException :" + e.toString());
        }
    }

    protected boolean isCanStartRestore() {
        if (mRestoreService == null) {
            Logger.e(TAG, "isCanStartRestore(): mRestoreService is null");
            return false;
        }

        if (mRestoreService.getState() != Constants.State.INIT) {
            Logger.e(TAG,
                    "isCanStartRestore(): Can not to start Restore. Restore Service state is "
                            + mRestoreService.getState());
            return false;
        }
        return true;
    }

    protected ProgressDialog createProgressDlg() {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(getActivity());
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            mProgressDialog.setMessage(getString(R.string.restoring));
            mProgressDialog.setCancelable(false);
        }
        return mProgressDialog;
    }

    protected void dismissProgressDialog() {
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
    public void onComposerChanged(final int type, final int num) {
        AndroidSchedulers.mainThread().scheduleDirect(new Runnable() {
            @Override
            public void run() {
                showProgressDialog(num, type);
            }
        });
    }

    @Override
    public void onProgressChanged(Composer composer, int progress) {
        if (mProgressDialog != null) {
            Logger.i(TAG, "onProgressChange, setProgress = " + progress);
            mProgressDialog.setProgress(progress);
        }
    }

    @Override
    public void onRestoreEnd(boolean bSuccess, ArrayList<ResultDialog.ResultEntity> resultRecord) {
        final ArrayList<ResultDialog.ResultEntity> iResultRecord = resultRecord;
        Logger.d(TAG, "onRestoreEnd");
        boolean hasSuccess = false;
        for (ResultDialog.ResultEntity result : resultRecord) {
            if (ResultDialog.ResultEntity.SUCCESS == result.getResult()) {
                hasSuccess = true;
                break;
            }
        }

        if (hasSuccess) {
            String recordXmlFile = mRestoreFolderPath + File.separator + Constants.RECORD_XML;
            String content = Utils.readFromFile(recordXmlFile);
            ArrayList<RecordXmlInfo> recordList = new ArrayList<>();
            if (content != null) {
                recordList = RecordXmlParser.parse(content.toString());
            }
            RecordXmlComposer xmlComposer = new RecordXmlComposer();
            xmlComposer.startCompose();

            RecordXmlInfo restoreInfo = new RecordXmlInfo();
            restoreInfo.setRestore(true);
            restoreInfo.setDevice(Utils.getPhoneSearialNumber());
            restoreInfo.setTime(String.valueOf(System.currentTimeMillis()));

            boolean bAdded = false;
            for (RecordXmlInfo record : recordList) {
                if (record.getDevice().equals(restoreInfo.getDevice())) {
                    xmlComposer.addOneRecord(restoreInfo);
                    bAdded = true;
                } else {
                    xmlComposer.addOneRecord(record);
                }
            }

            if (!bAdded) {
                xmlComposer.addOneRecord(restoreInfo);
            }
            xmlComposer.endCompose();
            Utils.writeToFile(xmlComposer.getXmlInfo(), recordXmlFile);
        }
        Logger.d(TAG, " Restore show Result Dialog");
        final int state = mRestoreService.getState();
        AndroidSchedulers.mainThread().scheduleDirect(new Runnable() {
            @Override
            public void run() {
                if (!TextUtils.isEmpty(mRestoreFolderPath) && state != Constants.State.FINISH) {
                    //mNeedUpdateResult = true;
                } else {
                    showRestoreResult(iResultRecord);
                }
            }
        });
    }

    private void showRestoreResult(ArrayList<ResultDialog.ResultEntity> list) {
        dismissProgressDialog();
        Bundle args = new Bundle();
        args.putParcelableArrayList("result", list);
        try {
            Toast.makeText(getContext(), "scuessed!", Toast.LENGTH_SHORT).show();
            //showDialog(Constants.DialogID.DLG_RESULT, args);
        } catch (WindowManager.BadTokenException e) {
            Logger.e(TAG, "BadTokenException");
        }
    }

    @Override
    public void onRestoreErr(IOException e) {
        if (errChecked()) {
            if (mRestoreService != null && mRestoreService.getState() != Constants.State.INIT
                    && mRestoreService.getState() != Constants.State.FINISH) {
                mRestoreService.pauseRestore();
            }
        }
    }

    protected boolean errChecked() {
        boolean ret = false;

        boolean isStorageMissing = StorageUtils.getInstance(getContext()).isStorageMissing();
        String path = StorageUtils.getInstance(getContext()).getBackupPath();

        if (isStorageMissing) {
            Logger.i(TAG, "SDCard is removed");
            getContext().stopService(new Intent(getContext(), RestoreService.class));
        } else if (StorageUtils.getAvailableSize(path) <= StorageUtils.MINIMUM_SIZE) {
            Logger.i(TAG, "SDCard is full");
            ret = true;

        } else {
            Logger.e(TAG, "Unkown error, don't pause.");
        }
        return ret;
    }

}
