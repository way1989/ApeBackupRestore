package com.ape.backuprestore.fragment;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.ape.backup.R;
import com.ape.backuprestore.PersonalItemData;
import com.ape.backuprestore.adapter.BackupAdapter;
import com.jakewharton.rxbinding2.view.RxView;
import com.weavey.loading.lib.LoadingLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;
import io.reactivex.observers.DisposableObserver;

/**
 * Created by android on 18-1-26.
 */

public abstract class BaseFragment extends Fragment implements BackupAdapter.OnItemClickListener {
    private static final String TAG = "BaseFragment";
    protected ProgressDialog mProgressDialog;
    @BindView(R.id.rv_data_category)
    RecyclerView mRvDataCategory;
    @BindView(R.id.loading_layout)
    LoadingLayout mLoadingLayout;
    @BindView(R.id.btn_backup)
    Button mBtnBackup;
    private BackupAdapter mAdapter;
    @NonNull
    private CompositeDisposable mCompositeDisposable = new CompositeDisposable();

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(getLayoutId(), container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindService();
        mBtnBackup.setText(getButtonStrRes());
        mAdapter = new BackupAdapter(getContext(), this);
        mRvDataCategory.setLayoutManager(new GridLayoutManager(getContext(), 3));
        mRvDataCategory.setAdapter(mAdapter);
        RxView.clicks(mBtnBackup)
                .throttleFirst(1, TimeUnit.SECONDS)
                .subscribe(new Consumer<Object>() {
                    @Override
                    public void accept(Object o) throws Exception {
                        start();
                    }
                });

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
        mLoadingLayout.post(new Runnable() {
            @Override
            public void run() {
                mLoadingLayout.setStatus(LoadingLayout.Loading);
            }
        });
        getData(observer);
        mCompositeDisposable.add(observer);
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unBindService();
        mCompositeDisposable.clear();
    }

    protected abstract void start();

    protected abstract void bindService();

    protected abstract void unBindService();

    protected abstract void getData(DisposableObserver<List<PersonalItemData>> observer);

    public abstract @LayoutRes
    int getLayoutId();

    protected abstract @StringRes
    int getButtonStrRes();

    @Override
    public void onItemClick(View v) {
        mBtnBackup.setEnabled(!getSelectedItemList().isEmpty());
    }

    protected ArrayList<Integer> getSelectedItemList() {
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
}
