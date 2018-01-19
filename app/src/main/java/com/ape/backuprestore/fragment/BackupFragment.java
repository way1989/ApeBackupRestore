package com.ape.backuprestore.fragment;


import android.app.ProgressDialog;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.ape.backup.R;
import com.ape.backuprestore.BackupEngine;
import com.ape.backuprestore.BackupService;
import com.ape.backuprestore.PersonalItemData;
import com.ape.backuprestore.RecordXmlComposer;
import com.ape.backuprestore.RecordXmlInfo;
import com.ape.backuprestore.ResultDialog;
import com.ape.backuprestore.adapter.BackupAdapter;
import com.ape.backuprestore.modules.Composer;
import com.ape.backuprestore.util.BackupUtil;
import com.ape.backuprestore.util.RxSchedulers;
import com.ape.backuprestore.utils.Constants;
import com.ape.backuprestore.utils.ModuleType;
import com.ape.backuprestore.utils.StorageUtils;
import com.ape.backuprestore.utils.Utils;
import com.weavey.loading.lib.LoadingLayout;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.observers.DisposableObserver;

import static com.ape.backuprestore.utils.FileUtils.deleteFileOrFolder;

/**
 * Created by way on 2018/1/18.
 */

public class BackupFragment extends Fragment implements BackupAdapter.OnItemClickListener, BackupService.OnBackupStatusListener {
    private static final String TAG = "BackupFragment";
    protected BackupService.BackupBinder mBackupService;
    protected ProgressDialog mProgressDialog;
    @BindView(R.id.rv_data_category)
    RecyclerView mRvDataCategory;
    @BindView(R.id.loading_layout)
    LoadingLayout mLoadingLayout;
    @BindView(R.id.btn_backup)
    Button mBtnBackup;
    private BackupAdapter mAdapter;
    @NonNull
    private CompositeDisposable mCompositeDisposable;
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

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_backup, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindBackupService();
        mAdapter = new BackupAdapter(getContext(), this);
        mRvDataCategory.setLayoutManager(new GridLayoutManager(getContext(), 3));
        mRvDataCategory.setAdapter(mAdapter);
        mCompositeDisposable = new CompositeDisposable();

        mCompositeDisposable.clear();
        DisposableObserver<List<PersonalItemData>> observer = new DisposableObserver<List<PersonalItemData>>() {

            @Override
            public void onNext(List<PersonalItemData> itemDataList) {
                Log.d(TAG, "onNext: itemDataList = " + itemDataList.size());
                mAdapter.setDatas(itemDataList);
                mLoadingLayout.setStatus(LoadingLayout.Success);
            }

            @Override
            public void onError(Throwable e) {
                Log.d(TAG, "onError: e = " + e);
                mLoadingLayout.setStatus(LoadingLayout.Error);
            }

            @Override
            public void onComplete() {

            }
        };
        BackupUtil.getPersonalItemDatas(getContext())
                .doOnSubscribe(new Consumer<Disposable>() {
                    @Override
                    public void accept(Disposable disposable) throws Exception {
                        mLoadingLayout.setStatus(LoadingLayout.Loading);
                    }
                })
                .compose(RxSchedulers.<List<PersonalItemData>>io_main()).subscribe(observer);
        mCompositeDisposable.add(observer);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unBindBackupService();
        mCompositeDisposable.clear();
    }

    @Override
    public void onItemClick(View v) {
        mBtnBackup.setEnabled(!getSelectedItemList().isEmpty());
    }

    private void startBackup() {
        //SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        //String folderName = dateFormat.format(new Date(System.currentTimeMillis()));

        String path = StorageUtils.getBackupPath();
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
                String path = StorageUtils.getBackupPath();
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

    private ArrayList<Integer> getSelectedItemList() {
        ArrayList<Integer> list = new ArrayList<>();
        int count = mAdapter.getItemCount();
        for (int position = 0; position < count; position++) {
            PersonalItemData item = mAdapter.getItemByPosition(position);
            if (item.isSelected()) {
                list.add(item.getType());
            }
        }
        return list;
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

    private void bindBackupService() {
        getContext().bindService(new Intent(getContext(), BackupService.class), mServiceCon, Service.BIND_AUTO_CREATE);
    }

    private void unBindBackupService() {
        try {
            getContext().unbindService(mServiceCon);
        } catch (Exception e) {
            e.printStackTrace();
        }
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

    @OnClick(R.id.btn_backup)
    public void onViewClicked() {
        startBackup();
    }
}
