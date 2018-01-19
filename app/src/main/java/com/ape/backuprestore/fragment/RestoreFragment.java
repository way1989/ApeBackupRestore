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
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import com.ape.backup.R;
import com.ape.backuprestore.PersonalItemData;
import com.ape.backuprestore.RecordXmlComposer;
import com.ape.backuprestore.RecordXmlInfo;
import com.ape.backuprestore.RecordXmlParser;
import com.ape.backuprestore.RestoreService;
import com.ape.backuprestore.ResultDialog;
import com.ape.backuprestore.adapter.BackupAdapter;
import com.ape.backuprestore.modules.Composer;
import com.ape.backuprestore.util.RestoreUtil;
import com.ape.backuprestore.util.RxSchedulers;
import com.ape.backuprestore.utils.BackupFilePreview;
import com.ape.backuprestore.utils.Constants;
import com.ape.backuprestore.utils.Logger;
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

/**
 * Created by way on 2018/1/18.
 */

public class RestoreFragment extends Fragment implements BackupAdapter.OnItemClickListener, RestoreService.OnRestoreStatusListener {
    private static final String TAG = "RestoreFragment";
    protected RestoreService.RestoreBinder mRestoreService;
    protected ProgressDialog mProgressDialog;
    @BindView(R.id.rv_data_category)
    RecyclerView mRvDataCategory;
    @BindView(R.id.loading_layout)
    LoadingLayout mLoadingLayout;
    @BindView(R.id.btn_backup)
    Button mBtnBackup;
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
    private BackupAdapter mAdapter;
    @NonNull
    private CompositeDisposable mCompositeDisposable;
    private String mRestoreFolderPath;

    public RestoreFragment() {
        // Required empty public constructor
    }

    public static RestoreFragment newInstance() {
        RestoreFragment fragment = new RestoreFragment();
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
        bindRestoreService();
        mAdapter = new BackupAdapter(getContext(), this);
        mRvDataCategory.setLayoutManager(new GridLayoutManager(getContext(), 3));
        mRvDataCategory.setAdapter(mAdapter);
        mCompositeDisposable = new CompositeDisposable();

        mCompositeDisposable.clear();
        DisposableObserver<List<PersonalItemData>> observer = new DisposableObserver<List<PersonalItemData>>() {

            @Override
            public void onNext(List<PersonalItemData> itemDataList) {
                Log.d(TAG, "onNext: itemDataList = " + itemDataList.size());
                if (itemDataList.isEmpty()) {
                    mLoadingLayout.setStatus(LoadingLayout.Empty);
                } else {
                    mAdapter.setDatas(itemDataList);
                    mLoadingLayout.setStatus(LoadingLayout.Success);
                }
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
        RestoreUtil.getPersonalItemDatas(getContext())
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
        unBindRestoreService();
        mCompositeDisposable.clear();
    }

    public void startRestore(ArrayList<Integer> restoreModeLists) {
        if (!isCanStartRestore()) {
            return;
        }
        startService();
        Logger.d(TAG, "startRestore");
        if (restoreModeLists.size() == 0) {
            Toast.makeText(getContext(), getString(R.string.no_item_selected), Toast.LENGTH_SHORT).show();
            return;
        }
        mRestoreService.setRestoreModelList(restoreModeLists);
        boolean ret = mRestoreService.startRestore(mRestoreFolderPath);
        if (ret) {
            String path = StorageUtils.getBackupPath();
            if (path == null) {
                // no sdcard
                Logger.d(TAG, "SDCard is removed");
                return;
            }
            int count = BackupFilePreview.getInstance().getItemCount(restoreModeLists.get(0));
            int type = restoreModeLists.get(0);
            showProgressDialog(count, type);
        } else {
            stopService();
        }
    }

    private void bindRestoreService() {
        getContext().bindService(new Intent(getContext(), RestoreService.class), mServiceCon, Service.BIND_AUTO_CREATE);
    }

    private void unBindRestoreService() {
        if (mRestoreService != null) {
            mRestoreService.setOnRestoreChangedListner(null);
        }
        try {
            getContext().unbindService(mServiceCon);
        } catch (Exception e) {
            e.printStackTrace();
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

        boolean isStorageMissing = StorageUtils.isStorageMissing();
        String path = StorageUtils.getBackupPath();

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

    @Override
    public void onItemClick(View v) {
        mBtnBackup.setEnabled(!getSelectedItemList().isEmpty());
    }

    @OnClick(R.id.btn_backup)
    public void onViewClicked() {
        startRestore(getSelectedItemList());
    }
}
