package com.wangwei.music_wangwei.service;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.wangwei.music_wangwei.R;
import com.wangwei.music_wangwei.activity.HomeActivity;
import com.wangwei.music_wangwei.activity.MusicPlayActivity;
import com.wangwei.music_wangwei.activity.PlaylistDialog;
import com.wangwei.music_wangwei.adapter.FloatingMusicListAdapter;
import com.wangwei.music_wangwei.entity.MusicInfo;
import com.wangwei.music_wangwei.util.MusicPlayer;

import java.util.List;

public class FloatingViewService extends Service {

    private WindowManager windowManager;
    private View floatingView;
    private MusicPlayer musicPlayer;
    private List<MusicInfo> musicList;
    private int currentPosition;
    private ImageView playPauseButton;
    private SeekBar progressBar;
    private TextView musicNameTextView;
    private TextView artistNameTextView;
    private ImageView coverImageView;
    private RecyclerView musicListView;
    private FloatingMusicListAdapter adapter;
    private MediaPlayer mediaPlayer;

    private final IBinder binder = new LocalBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        floatingView = LayoutInflater.from(this).inflate(R.layout.layout_floating_view, null);

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O ?
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                        WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );

        params.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;

        windowManager.addView(floatingView, params);

        musicPlayer = new MusicPlayer(this);

        playPauseButton = floatingView.findViewById(R.id.floating_play_pause);
        progressBar = floatingView.findViewById(R.id.floating_progress_bar);
        musicNameTextView = floatingView.findViewById(R.id.floating_music_name);
        artistNameTextView = floatingView.findViewById(R.id.floating_artist_name);
        coverImageView = floatingView.findViewById(R.id.floating_cover_image);
        musicListView = floatingView.findViewById(R.id.floating_music_list);

        playPauseButton.setOnClickListener(v -> {
            if (musicPlayer.isPlaying()) {
                musicPlayer.pause();
                playPauseButton.setImageResource(R.drawable.ic_play);
            } else {
                musicPlayer.play();
                playPauseButton.setImageResource(R.drawable.ic_pause);
            }
        });

        ImageView playlistButton = floatingView.findViewById(R.id.floating_playlist);
        playlistButton.setOnClickListener(v -> {
            if (musicListView.getVisibility() == View.GONE) {
                musicListView.setVisibility(View.VISIBLE);
            } else {
                musicListView.setVisibility(View.GONE);
            }
        });

        floatingView.setOnClickListener(v -> {
            Intent intent = new Intent(this, MusicPlayActivity.class);
            intent.putExtra("music_list", (java.io.Serializable) musicList);
            intent.putExtra("current_position", currentPosition);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        });

        progressBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    musicPlayer.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        // 定时更新进度条
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(1000);
                    if (musicPlayer.isPlaying()) {
                        progressBar.setProgress(musicPlayer.getCurrentPosition());
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }


    // 在 Service 中处理
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        stopSelf();
        return super.onStartCommand(intent, flags, startId);
    }

    public void setMusicList(List<MusicInfo> musicList, int currentPosition) {
        this.musicList = musicList;
        this.currentPosition = currentPosition;

        if (musicList != null && currentPosition >= 0 && currentPosition < musicList.size()) {
            MusicInfo musicInfo = musicList.get(currentPosition);
            musicNameTextView.setText(musicInfo.getMusicName());
            artistNameTextView.setText(musicInfo.getAuthor());
            Glide.with(this).load(musicInfo.getCoverUrl()).into(coverImageView);
            musicPlayer.playMusic(musicInfo.getMusicUrl());
            progressBar.setMax(musicPlayer.getDuration());
        }

        // 初始化 RecyclerView 和适配器
        adapter = new FloatingMusicListAdapter(this, musicList);
        musicListView.setLayoutManager(new LinearLayoutManager(this));
        musicListView.setAdapter(adapter);

        adapter.setOnMusicItemClickListener((position, info) -> {
            this.currentPosition = position;
            musicPlayer.playMusic(info.getMusicUrl());
            musicNameTextView.setText(info.getMusicName());
            artistNameTextView.setText(info.getAuthor());
            Glide.with(this).load(info.getCoverUrl()).into(coverImageView);
            progressBar.setMax(musicPlayer.getDuration());
            playPauseButton.setImageResource(R.drawable.ic_pause);
        });
    }

    public class LocalBinder extends Binder {
        public FloatingViewService getService() {
            return FloatingViewService.this;
        }
    }

    //
    public void updateMusicInfo(MusicInfo musicInfo) {
        if (floatingView != null) {
            // 确保在主线程更新UI
            new Handler(Looper.getMainLooper()).post(() -> {
                TextView songName = floatingView.findViewById(R.id.tv_floating_song_name);
                TextView artist = floatingView.findViewById(R.id.tv_floating_artist);
                ImageView album = floatingView.findViewById(R.id.iv_floating_album);

                songName.setText(musicInfo.getMusicName());
                artist.setText(musicInfo.getAuthor());
                Glide.with(FloatingViewService.this)
                        .load(musicInfo.getCoverUrl())
                        .into(album);
            });
        }
    }

    // 新增 setCurrentPosition 方法
    public void setCurrentPosition(int position) {
        if (musicList != null && position >= 0 && position < musicList.size()) {
            this.currentPosition = position;
            MusicInfo musicInfo = musicList.get(position);
            musicNameTextView.setText(musicInfo.getMusicName());
            artistNameTextView.setText(musicInfo.getAuthor());
            Glide.with(this).load(musicInfo.getCoverUrl()).into(coverImageView);
            musicPlayer.playMusic(musicInfo.getMusicUrl());
            progressBar.setMax(musicPlayer.getDuration());
            playPauseButton.setImageResource(R.drawable.ic_pause);

            // 更新适配器中的当前位置
            if (adapter != null) {
                adapter.setCurrentPosition(position);
            }
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    // 隐藏悬浮窗
    public void hideFloatingWindow() {
        if (floatingView != null && windowManager != null) {
            windowManager.removeView(floatingView);
            floatingView = null;
        }
    }

    // 停止服务并释放资源
    public void stopFloatingService() {
        hideFloatingWindow();
        stopSelf();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // 移除悬浮窗视图
        if (floatingView != null) {
            try {
                windowManager.removeView(floatingView);
            } catch (IllegalArgumentException e) {
                // 处理可能的异常
                e.printStackTrace();
            }
        }

        // 释放MediaPlayer资源
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}