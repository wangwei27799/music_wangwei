package com.wangwei.music_wangwei.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.wangwei.music_wangwei.entity.MusicInfo;

import java.util.List;

public class FloatingMusicListAdapter extends RecyclerView.Adapter<FloatingMusicListAdapter.MusicViewHolder> {

    private Context context;
    private List<MusicInfo> musicInfoList;
    private OnMusicItemClickListener listener;
    private int currentPosition = -1; // 新增：当前选中的音乐项的位置

    public FloatingMusicListAdapter(Context context, List<MusicInfo> musicInfoList) {
        this.context = context;
        this.musicInfoList = musicInfoList;
    }

    public void setOnMusicItemClickListener(OnMusicItemClickListener listener) {
        this.listener = listener;
    }

    // 新增：setCurrentPosition 方法
    public void setCurrentPosition(int position) {
        this.currentPosition = position;
        notifyDataSetChanged(); // 通知适配器数据已更改，刷新列表
    }

    @NonNull
    @Override
    public MusicViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_2, parent, false);
        return new MusicViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MusicViewHolder holder, int position) {
        MusicInfo musicInfo = musicInfoList.get(position);
        holder.text1.setText(musicInfo.getMusicName());
        holder.text2.setText(musicInfo.getAuthor());

        // 新增：根据当前选中位置设置样式
        if (position == currentPosition) {
            // 设置选中项的样式，这里可以根据需求修改
            holder.itemView.setBackgroundColor(context.getResources().getColor(android.R.color.darker_gray));
        } else {
            holder.itemView.setBackgroundColor(context.getResources().getColor(android.R.color.transparent));
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onMusicItemClick(position, musicInfo);
            }
        });
    }

    @Override
    public int getItemCount() {
        Log.d("FloatingMusicListAdapter", "getItemCount: ");
        if (musicInfoList == null) {
            return 0;
        }
        int m = musicInfoList.size();
        Log.d("FloatingMusicListAdapter", "getItemCount: ");
        return m;
    }

    static class MusicViewHolder extends RecyclerView.ViewHolder {
        TextView text1;
        TextView text2;

        MusicViewHolder(@NonNull View itemView) {
            super(itemView);
            text1 = itemView.findViewById(android.R.id.text1);
            text2 = itemView.findViewById(android.R.id.text2);
        }
    }

    public interface OnMusicItemClickListener {
        void onMusicItemClick(int position, MusicInfo musicInfo);
    }
}