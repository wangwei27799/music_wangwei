package com.wangwei.music_wangwei.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.wangwei.music_wangwei.R;
import com.wangwei.music_wangwei.entity.MusicInfo;
import com.wangwei.music_wangwei.event.MusicItemClickListener;

import java.util.List;

public class TwoColumnsAdapter extends RecyclerView.Adapter<TwoColumnsAdapter.TwoColumnsViewHolder> {
    private Context context;
    private List<MusicInfo> musicInfoList;
    private LayoutInflater inflater;

    private MusicItemClickListener itemClickListener;

    public TwoColumnsAdapter(Context context, List<MusicInfo> musicInfoList) {
        this.context = context;
        this.musicInfoList = musicInfoList;
        this.inflater = LayoutInflater.from(context);
    }

    public void setItemClickListener(MusicItemClickListener listener) {
        this.itemClickListener = listener;
    }

    @Override
    public TwoColumnsViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.item_two_columns_item, parent, false);
        return new TwoColumnsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(TwoColumnsViewHolder holder, int position) {
        MusicInfo musicInfo = musicInfoList.get(position);

        // 加载封面图
        Glide.with(context)
                .load(musicInfo.getCoverUrl())
                .placeholder(R.drawable.placeholder_music)
                .error(R.drawable.error_music)
                .into(holder.coverImageView);

        // 设置歌曲信息
        holder.musicNameTextView.setText(musicInfo.getMusicName());
        holder.authorTextView.setText(musicInfo.getAuthor());

        // 加号按钮点击事件
        holder.addButton.setOnClickListener(v -> {
            Toast.makeText(context, "将 " + musicInfo.getMusicName() + " 添加到音乐列表", Toast.LENGTH_SHORT).show();
        });

        // 项点击事件
        holder.itemView.setOnClickListener(v -> {
            if (itemClickListener != null){
                Toast.makeText(context, musicInfo.getMusicName(), Toast.LENGTH_SHORT).show();
                itemClickListener.onMusicItemClick(position, musicInfo);
            }
        });
    }

    @Override
    public int getItemCount() {
        return musicInfoList.size();
    }

    static class TwoColumnsViewHolder extends RecyclerView.ViewHolder {
        ImageView coverImageView;
        TextView musicNameTextView;
        TextView authorTextView;
        ImageView addButton;

        TwoColumnsViewHolder(View itemView) {
            super(itemView);
            coverImageView = itemView.findViewById(R.id.cover_image);
            musicNameTextView = itemView.findViewById(R.id.music_name);
            authorTextView = itemView.findViewById(R.id.author);
            addButton = itemView.findViewById(R.id.add_button);
        }
    }
}