package com.wangwei.music_wangwei.activity;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.palette.graphics.Palette;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.wangwei.music_wangwei.R;
import com.wangwei.music_wangwei.entity.MusicInfo;
import com.wangwei.music_wangwei.service.FloatingViewService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class MusicPlayActivity extends AppCompatActivity implements View.OnClickListener {

    public static final String ACTION_PLAY = "com.example.music.action.PLAY";
    public static final String ACTION_PAUSE = "com.example.music.action.PAUSE";
    public static final String ACTION_NEXT = "com.example.music.action.NEXT";
    public static final String ACTION_PREVIOUS = "com.example.music.action.PREVIOUS";
    public static final String ACTION_UPDATE_UI = "com.example.music.action.UPDATE_UI";

    public static final String ACTION_PLAY_PAUSE = "com.example.music.action.PLAY_PAUSE";


    // 播放模式
    public static final int PLAY_MODE_ORDER = 0;    // 顺序播放
    public static final int PLAY_MODE_REPEAT = 1;   // 单曲循环
    public static final int PLAY_MODE_SHUFFLE = 2;  // 随机播放
    public static final String  ACTION_CHANGE_MODE = "com.example.music.action.CHANGE_MODE";
    public static final String ACTION_SEEK = "com.example.music.action.SEEK";

    public static int playMode = PLAY_MODE_ORDER;

    // UI组件
    private ImageView ivClose, ivAlbum, ivPlayPause, ivPrevious, ivNext;
    private TextView tvSongName, tvArtist, tvCurrentTime, tvTotalTime;
    private SeekBar sbProgress;

    private FloatingViewService floatingViewService;
    private boolean isServiceBound;

    // 数据
    private ArrayList<MusicInfo> musicList;
    private int currentPosition;
    private List<Integer> shuffleList = new ArrayList<>();
    private int currentShuffleIndex = 0;

    // 播放器和动画
    private MediaPlayer mediaPlayer;
    private RotateAnimation rotateAnimation;
    private Handler progressHandler;

    private GestureDetector gestureDetector;

    private ImageView ivPlaylist;

    private ImageView ivFavorite;
    private boolean isFavorite = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_play);


        Log.d("FloatViewService", "Service started");

        initViews();
        initData();
        initAnimation();
        setupListeners();
        initMediaPlayer();
        initGestureDetector();

        // 注册广播接收器
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_PLAY);
        filter.addAction(ACTION_PAUSE);
        filter.addAction(ACTION_NEXT);
        filter.addAction(ACTION_PREVIOUS);
        filter.addAction(ACTION_UPDATE_UI);
        registerReceiver(musicReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
    }


    private static final int UPDATE_MUSIC_INFO = 1;

    private Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == UPDATE_MUSIC_INFO) {
                MusicInfo musicInfo = (MusicInfo) msg.obj;
                tvSongName.setText(musicInfo.getMusicName());
                tvArtist.setText(musicInfo.getAuthor());

                Glide.with(MusicPlayActivity.this)
                        .asBitmap()
                        .load(musicInfo.getCoverUrl())
                        .transition(BitmapTransitionOptions.withCrossFade())
                        .into(new CustomTarget<Bitmap>() {
                            @Override
                            public void onResourceReady(Bitmap resource, Transition<? super Bitmap> transition) {
                                ivAlbum.setImageBitmap(resource);
                                ivAlbum.startAnimation(rotateAnimation);
                            }

                            @Override
                            public void onLoadCleared(android.graphics.drawable.Drawable placeholder) {
                            }
                        });

                playMusic(musicInfo.getMusicUrl());
            }
        }
    };
    private void initViews() {
        ivClose = findViewById(R.id.iv_close);
        ivAlbum = findViewById(R.id.iv_album);
        ivPlayPause = findViewById(R.id.iv_play_pause);
        ivPrevious = findViewById(R.id.iv_previous);
        ivNext = findViewById(R.id.iv_next);
        tvSongName = findViewById(R.id.tv_song_name);
        tvArtist = findViewById(R.id.tv_artist);
        tvCurrentTime = findViewById(R.id.tv_current_time);
        tvTotalTime = findViewById(R.id.tv_total_time);
        sbProgress = findViewById(R.id.sb_progress);

        ivPlaylist = findViewById(R.id.iv_playlist);
        ivPlaylist.setOnClickListener(this);

        ivFavorite = findViewById(R.id.iv_favorite);
        ivFavorite.setOnClickListener(this);
    }

    private void initData() {
        Intent intent = getIntent();
        musicList = intent.getParcelableArrayListExtra("music_list");
        currentPosition = intent.getIntExtra("current_position", 0);

        // 添加空值检查
        if (musicList == null || musicList.isEmpty()) {
            Toast.makeText(this, "音乐列表为空", Toast.LENGTH_SHORT).show();
            finish(); // 关闭当前活动
            return;
        }

        // 将数据处理操作移到子线程中执行
        new Thread(() -> {
            if (musicList != null && currentPosition >= 0 && currentPosition < musicList.size()) {
                MusicInfo musicInfo = musicList.get(currentPosition);
                Message msg = handler.obtainMessage(UPDATE_MUSIC_INFO, musicInfo);
                handler.sendMessage(msg);
            }
        }).start();
    }

    private void initAnimation() {
        rotateAnimation = new RotateAnimation(0, 360,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        rotateAnimation.setDuration(20000);
        rotateAnimation.setInterpolator(new LinearInterpolator());
        rotateAnimation.setRepeatCount(Animation.INFINITE);
        rotateAnimation.setFillAfter(true);
    }


    // 切换播放模式的方法
    public void changePlayMode() {
        switch (playMode) {
            case PLAY_MODE_ORDER:
                playMode = PLAY_MODE_REPEAT;
                Toast.makeText(this, "切换到单曲循环模式", Toast.LENGTH_SHORT).show();
                break;
            case PLAY_MODE_REPEAT:
                playMode = PLAY_MODE_SHUFFLE;
                Toast.makeText(this, "切换到随机播放模式", Toast.LENGTH_SHORT).show();
                break;
            case PLAY_MODE_SHUFFLE:
                playMode = PLAY_MODE_ORDER;
                Toast.makeText(this, "切换到顺序播放模式", Toast.LENGTH_SHORT).show();
                break;
        }

        // 你可以在这里添加根据不同播放模式做相应处理的逻辑
        handlePlayModeChange();
    }

    // 处理播放模式改变的逻辑
    private void handlePlayModeChange() {
        // 根据当前的 playMode 变量进行不同的操作
        switch (playMode) {
            case PLAY_MODE_ORDER:
                // 顺序播放的逻辑
                break;
            case PLAY_MODE_REPEAT:
                // 单曲循环的逻辑
                break;
            case PLAY_MODE_SHUFFLE:
                // 随机播放的逻辑
                break;
        }
    }

    private void setupListeners() {
        ivClose.setOnClickListener(this);
        ivPlayPause.setOnClickListener(this);
        ivPrevious.setOnClickListener(this);
        ivNext.setOnClickListener(this);

        sbProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    int seekPosition = mediaPlayer.getDuration() * progress / 1000;
                    mediaPlayer.seekTo(seekPosition);
                    tvCurrentTime.setText(formatTime(seekPosition));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                progressHandler.removeMessages(0);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                progressHandler.sendEmptyMessage(0);
            }
        });
    }

    private void initMediaPlayer() {
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnCompletionListener(mp -> playNextMusic());

        progressHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                if (mediaPlayer.isPlaying()) {
                    Log.d("MusicPlayActivity", "handleMessage: ");
                    int currentPos = mediaPlayer.getCurrentPosition();
                    sbProgress.setProgress(currentPos * 1000 / mediaPlayer.getDuration());
                    tvCurrentTime.setText(formatTime(currentPos));
                    sendEmptyMessageDelayed(0, 1000);
                }
            }
        };
    }

    private void updateMusicInfo() {
        MusicInfo musicInfo = musicList.get(currentPosition);
        tvSongName.setText(musicInfo.getMusicName());
        tvArtist.setText(musicInfo.getAuthor());

        Glide.with(this)
                .asBitmap()
                .load(musicInfo.getCoverUrl())
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, Transition<? super Bitmap> transition) {
                        ivAlbum.setImageBitmap(resource);
                        ivAlbum.startAnimation(rotateAnimation);
                    }

                    @Override
                    public void onLoadCleared(Drawable placeholder) {
                    }
                });

        playMusic(musicInfo.getMusicUrl());
    }

    private void playMusic(String musicUrl) {
        try {
            mediaPlayer.reset();
            mediaPlayer.setDataSource(musicUrl);
            mediaPlayer.prepare();
            mediaPlayer.start();
            progressHandler.sendEmptyMessage(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void togglePlayPause() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            ivPlayPause.setImageResource(R.drawable.ic_play);
        } else {
            mediaPlayer.start();
            ivPlayPause.setImageResource(R.drawable.ic_pause);
        }
    }

    private void playPreviousMusic() {
        if (currentPosition == 0) {
            currentPosition = musicList.size() - 1; // 循环到最后一首
        } else {
            currentPosition--;
        }
        updateMusicInfo();
    }

    private void playNextMusic() {
        if (currentPosition == musicList.size() - 1) {
            currentPosition = 0; // 循环到第一首
        } else {
            currentPosition++;
        }
        updateMusicInfo();
    }

    private String formatTime(int timeMs) {
        int seconds = timeMs / 1000;
        return String.format("%02d:%02d", seconds / 60, seconds % 60);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if(id == R.id.iv_close){
            finish();
        }
        if(id == R.id.iv_play_pause){
            togglePlayPause();
        }
        if(id == R.id.iv_previous){
            playPreviousMusic();
        }
        if(id == R.id.iv_next){
            playNextMusic();
        }
        if (id == R.id.iv_play_mode) {
            changePlayMode();
        }
        if (id == R.id.iv_playlist) {
            showPlaylistDialog();
        }
        if (id == R.id.iv_favorite) {
            if (isFavorite) {
                // 取消收藏
                ObjectAnimator scaleX = ObjectAnimator.ofFloat(ivFavorite, "scaleX", 1.0f, 0.8f, 1.0f);
                ObjectAnimator scaleY = ObjectAnimator.ofFloat(ivFavorite, "scaleY", 1.0f, 0.8f, 1.0f);
                AnimatorSet animatorSet = new AnimatorSet();
                animatorSet.playTogether(scaleX, scaleY);
                animatorSet.setDuration(1000);
                animatorSet.start();
                isFavorite = false;
            } else {
                // 收藏
                ObjectAnimator scaleX = ObjectAnimator.ofFloat(ivFavorite, "scaleX", 1.0f, 1.2f, 1.0f);
                ObjectAnimator scaleY = ObjectAnimator.ofFloat(ivFavorite, "scaleY", 1.0f, 1.2f, 1.0f);
                ObjectAnimator rotationY = ObjectAnimator.ofFloat(ivFavorite, "rotationY", 0f, 360f);
                AnimatorSet animatorSet = new AnimatorSet();
                animatorSet.playTogether(scaleX, scaleY, rotationY);
                animatorSet.setDuration(1000);
                animatorSet.start();
                isFavorite = true;
            }
        }
    }

    private void showPlaylistDialog() {
        PlaylistDialog dialog = new PlaylistDialog(this, musicList, currentPosition);
        dialog.setOnItemClickListener((position, musicInfo) -> {
            currentPosition = position;
            updateMusicInfo();
            dialog.dismiss();
        });
        dialog.show();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        mediaPlayer.release();
        unregisterReceiver(musicReceiver);
    }

    private final BroadcastReceiver musicReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // 处理广播事件
        }
    };


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event);
    }

    private void initGestureDetector() {
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                float distanceX = e2.getX() - e1.getX();
                // 判断是否是有效滑动（X 轴变化较大，且速度足够）
                if (Math.abs(distanceX) > 50 && Math.abs(velocityX) > 50) {
                    if (distanceX < 0) {
                        // 向左滑动，进入歌词页面
                        navigateToLyricsPage();
                        return true;
                    } else {
                        // 向右滑动，进入播放列表页面
                        navigateToListPage();
                        return true;
                    }
                }
                return false;
            }

            @Override
            public boolean onContextClick(MotionEvent event) {
                return gestureDetector.onTouchEvent(event) || super.onContextClick(event);
            }
        });
    }

    private void navigateToLyricsPage() {
        MusicInfo currentMusic = musicList.get(currentPosition);
        Intent intent = new Intent(MusicPlayActivity.this, LyricsActivity.class);
        intent.putExtra("musicInfo", currentMusic);
        startActivity(intent);
        // 添加切换动画，从右向左滑入
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    private void navigateToListPage() {
        Intent intent = new Intent(MusicPlayActivity.this, LyricsActivity.class); // 假设这是播放列表Activity
        intent.putExtra("musicList", (ArrayList<MusicInfo>) musicList);
        intent.putExtra("currentPosition", currentPosition);
        startActivity(intent);
        // 添加切换动画，从左向右滑入
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}
