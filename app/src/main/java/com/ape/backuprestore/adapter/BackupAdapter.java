package com.ape.backuprestore.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.ape.backuprestore.PersonalItemData;
import com.ape.backup.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by android on 16-7-16.
 */
public class BackupAdapter extends RecyclerView.Adapter<BackupAdapter.ViewHolder> {

    private LayoutInflater mInflater;
    private ArrayList<PersonalItemData> mPersonalItemDatas;
    private OnItemClickListener mListener;

    public BackupAdapter(Context context, OnItemClickListener onItemClickListener) {
        mInflater = LayoutInflater.from(context);
        setHasStableIds(true);
        mPersonalItemDatas = new ArrayList<>();
        mListener = onItemClickListener;
    }

    public void setDatas(List<PersonalItemData> datas) {
        mPersonalItemDatas.addAll(datas);
        notifyDataSetChanged();
    }

    public void reset() {
        mPersonalItemDatas.clear();
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.item_data, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        PersonalItemData itemData = mPersonalItemDatas.get(position);
        holder.itemView.setTag(itemData);

        holder.ivIcon.setImageResource(itemData.getIconId());
        holder.tvName.setText(itemData.getTextId());
        holder.tvCount.setText(String.valueOf(itemData.getCount()));
        holder.ivSelected.setVisibility(itemData.isSelected() ? View.VISIBLE : View.GONE);
    }

    @Override
    public int getItemCount() {
        return mPersonalItemDatas.size();
    }

    @Override
    public long getItemId(int position) {
//        return super.getItemId(position);
        return mPersonalItemDatas.get(position).getType();
    }

    public PersonalItemData getItemByPosition(int position) {
        if (position < 0 || position >= getItemCount()) return null;
        return mPersonalItemDatas.get(position);
    }

    public void notifyPersonalItemData(PersonalItemData itemData) {
        int index = mPersonalItemDatas.indexOf(itemData);
        boolean isSelected = itemData.isSelected();
        itemData.setSelected(itemData.isEnable() && !isSelected);
        notifyItemChanged(index);
    }

    public interface OnItemClickListener {
        void onItemClick(View v);
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        ImageView ivIcon;
        ImageView ivSelected;
        TextView tvCount;
        TextView tvName;

        public ViewHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            ivIcon = (ImageView) itemView.findViewById(R.id.iv_icon);
            ivSelected = (ImageView) itemView.findViewById(R.id.iv_selected);
            tvCount = (TextView) itemView.findViewById(R.id.tv_count);
            tvName = (TextView) itemView.findViewById(R.id.tv_name);
        }

        @Override
        public void onClick(View v) {
            PersonalItemData itemData = (PersonalItemData) v.getTag();
            notifyPersonalItemData(itemData);

            mListener.onItemClick(v);
        }
    }
}
