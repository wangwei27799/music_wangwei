package com.wangwei.music_wangwei.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.wangwei.music_wangwei.R;
import com.wangwei.music_wangwei.entity.MusicInfo;
import com.wangwei.music_wangwei.event.MusicItemClickListener;

import java.util.List;

public class BannerAdapter extends RecyclerView.Adapter<BannerAdapter.BannerViewHolder> {
    private Context context;
    private List<MusicInfo> musicInfoList;
    private LayoutInflater inflater;

    private MusicItemClickListener itemClickListener;

    public BannerAdapter(Context context, List<MusicInfo> musicInfoList) {
        this.context = context;
        this.musicInfoList = musicInfoList;
        this.inflater = LayoutInflater.from(context);
    }

    public void setItemClickListener(MusicItemClickListener listener) {
        this.itemClickListener = listener;
    }
    @Override
    public BannerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.item_banner_image, parent, false);
        return new BannerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(BannerViewHolder holder, int position) {
        MusicInfo musicInfo = musicInfoList.get(position);
        Glide.with(context)
                .load(musicInfo.getCoverUrl())
                .placeholder(R.drawable.placeholder_banner)
                .error(R.drawable.error_banner)
                .into(holder.bannerImageView);

        // 设置点击事件
        holder.itemView.setOnClickListener(v -> {
            if(itemClickListener != null){
                Toast.makeText(context, musicInfo.getMusicName(), Toast.LENGTH_SHORT).show();
                itemClickListener.onMusicItemClick(position, musicInfo);
            }

        });
    }

    @Override
    public int getItemCount() {
        return musicInfoList.size();
    }

    static class BannerViewHolder extends RecyclerView.ViewHolder {
        ImageView bannerImageView;

        BannerViewHolder(View itemView) {
            super(itemView);
            bannerImageView = itemView.findViewById(R.id.banner_image);
        }
    }
}