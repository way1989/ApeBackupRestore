package com.ape.backuprestore.fragment;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ape.backup.R;
import com.ape.backuprestore.adapter.BackupAdapter;
import com.weavey.loading.lib.LoadingLayout;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;

/**
 * Created by way on 2018/1/18.
 */

public class BackupFragment extends Fragment implements BackupAdapter.OnItemClickListener{
    @BindView(R.id.rv_data_category)
    RecyclerView mRvDataCategory;
    @BindView(R.id.loading_layout)
    LoadingLayout mLoadingLayout;
    private BackupAdapter mAdapter;
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
        mAdapter = new BackupAdapter(getContext(), this);
        mRvDataCategory.setLayoutManager(new GridLayoutManager(getContext(), 3));
        mRvDataCategory.setAdapter(mAdapter);

    }

    @Override
    public void onItemClick(View v) {

    }
}
