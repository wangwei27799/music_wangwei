package com.wangwei.music_wangwei.adapter;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.wangwei.music_wangwei.R;
import com.wangwei.music_wangwei.entity.MusicInfo;
import com.wangwei.music_wangwei.event.MusicItemClickListener;

import java.util.List;

public class HorizontalCardAdapter extends RecyclerView.Adapter<HorizontalCardAdapter.HorizontalCardViewHolder> {
    private Context context;
    private List<MusicInfo> musicInfoList;
    private LayoutInflater inflater;

    private MusicItemClickListener itemClickListener;

    public HorizontalCardAdapter(Context context, List<MusicInfo> musicInfoList) {
        this.context = context;
        this.musicInfoList = musicInfoList;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public HorizontalCardViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.item_horizontal_card_item, parent, false);
        return new HorizontalCardViewHolder(view);
    }

    public void setItemClickListener(MusicItemClickListener listener) {
        this.itemClickListener = listener;
    }

    @Override
    public void onBindViewHolder(HorizontalCardViewHolder holder, int position) {
        MusicInfo musicInfo = musicInfoList.get(position);
        Log.d("HorizontalCardAdapter", "onBindViewHolder: " + musicInfo.getCoverUrl());
        Glide.with(context)
                .load(musicInfo.getCoverUrl())
                .placeholder(R.drawable.placeholder_music)
                .error(R.drawable.error_music)
                .into(holder.coverImageView);

        holder.musicNameTextView.setText(musicInfo.getMusicName());
        holder.authorTextView.setText(musicInfo.getAuthor());

        // 加号按钮点击事件
        holder.addButton.setOnClickListener(v -> {
            Toast.makeText(context, "将 " + musicInfo.getMusicName() + " 添加到音乐列表", Toast.LENGTH_SHORT).show();
            if (checkSystemAlertWindowPermission()) {
                showFloatingWindow(musicInfo);
            } else {
                requestSystemAlertWindowPermission();
            }
        });

        // 项点击事件
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

    static class HorizontalCardViewHolder extends RecyclerView.ViewHolder {
        ImageView coverImageView;
        TextView musicNameTextView;
        TextView authorTextView;
        ImageView addButton;

        HorizontalCardViewHolder(View itemView) {
            super(itemView);
            coverImageView = itemView.findViewById(R.id.cover_image);
            musicNameTextView = itemView.findViewById(R.id.music_name);
            authorTextView = itemView.findViewById(R.id.author);
            addButton = itemView.findViewById(R.id.add_button);
        }
    }

    private boolean checkSystemAlertWindowPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(context);
        }
        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void requestSystemAlertWindowPermission() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + context.getPackageName()));
        ((android.app.Activity) context).startActivityForResult(intent, 1);
    }

    private void showFloatingWindow(MusicInfo musicInfo) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        View floatingView = LayoutInflater.from(context).inflate(R.layout.floating_music_view, null);

        // 设置悬浮窗布局参数
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                        WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                android.graphics.PixelFormat.TRANSLUCENT
        );

        // 设置悬浮窗的位置
        params.gravity = Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM;
        params.x = 0;
        params.y = 100;

        // 添加悬浮窗到窗口管理器
        windowManager.addView(floatingView, params);

        // 在悬浮窗中显示音乐信息
        TextView musicNameTextView = floatingView.findViewById(R.id.tv_floating_song_name);
        TextView authorTextView = floatingView.findViewById(R.id.tv_floating_artist);
        musicNameTextView.setText(musicInfo.getMusicName());
        authorTextView.setText(musicInfo.getAuthor());
    }
}