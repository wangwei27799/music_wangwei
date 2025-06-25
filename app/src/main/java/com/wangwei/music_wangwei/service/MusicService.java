package com.wangwei.music_wangwei.service;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.wangwei.music_wangwei.R;
import com.wangwei.music_wangwei.activity.LyricsActivity;
import com.wangwei.music_wangwei.activity.MusicPlayActivity;
import com.wangwei.music_wangwei.entity.MusicInfo;
import com.wangwei.music_wangwei.util.MusicPlayer;

import java.util.ArrayList;
import java.util.List;

public class MusicService extends Service implements MusicPlayer.OnCompletionListener {

    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "music_channel";

    private MusicPlayer musicPlayer;
    private List<MusicInfo> musicList = new ArrayList<>();
    private int currentPosition = 0;
    private final IBinder binder = new LocalBinder();



    @Override
    public void onCreate() {
        super.onCreate();
        musicPlayer = new MusicPlayer(this);
        musicPlayer.setOnCompletionListener(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            musicList = (List<MusicInfo>) intent.getSerializableExtra("music_list");
            currentPosition = intent.getIntExtra("current_position", 0);

            if (musicList != null && !musicList.isEmpty()) {
                playCurrentMusic();
                showNotification();
            }
        }
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (musicPlayer != null) {
            musicPlayer.release();
            musicPlayer = null;
        }
        stopForeground(true);
    }

    public class LocalBinder extends Binder {
        public MusicService getService() {
            return MusicService.this;
        }
    }


    // 在 MusicService 类中添加以下方法
    public void playMusic(String musicUrl) {
        musicPlayer.playMusic(musicUrl);
    }
    private void playCurrentMusic() {
        if (musicList != null && currentPosition >= 0 && currentPosition < musicList.size()) {
            MusicInfo musicInfo = musicList.get(currentPosition);
            musicPlayer.playMusic(musicInfo.getMusicUrl());
        }
    }

    // 在MusicService中
    private void sendLyricUpdateBroadcast(long currentTime) {
        Intent intent = new Intent(LyricsActivity.ACTION_UPDATE_LYRIC);
        intent.putExtra(LyricsActivity.EXTRA_CURRENT_TIME, currentTime);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    public void play() {
        musicPlayer.play();
        updateNotification();
    }

    public void pause() {
        musicPlayer.pause();
        updateNotification();
    }

    public void playPrevious() {
        currentPosition = (currentPosition - 1 + musicList.size()) % musicList.size();
        playCurrentMusic();
        updateNotification();
    }

    public void playNext() {
        Log.d("MusicService", "mmm" + musicList.size());
        currentPosition = (currentPosition + 1) % musicList.size();
        playCurrentMusic();
        updateNotification();
    }

    public void seekTo(int position) {
        musicPlayer.seekTo(position);
    }

    public boolean isPlaying() {
        return musicPlayer.isPlaying();
    }

    public int getCurrentPosition() {
        return musicPlayer.getCurrentPosition();
    }

    public int getDuration() {
        return musicPlayer.getDuration();
    }

    @Override
    public void onCompletion() {
        // 歌曲播放完成，自动播放下一首
        playNext();
    }

    @Override
    public void onError() {
        // 音乐播放错误处理
        // 可以在这里处理播放错误，如通知用户或尝试播放下一首
        playNext(); // 示例：播放错误时尝试播放下一首
    }

    @SuppressLint("ForegroundServiceType")
    private void showNotification() {
        createNotificationChannel();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("正在播放")
                .setContentText("音乐标题")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(getContentIntent())
                .setCustomContentView(getCustomView())
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle());

        Notification notification = builder.build();
        startForeground(NOTIFICATION_ID, notification);
    }

    private void updateNotification() {
        createNotificationChannel(); // 确保通知渠道已创建
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) {
            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle("正在播放")
                    .setContentText("音乐标题")
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setContentIntent(getContentIntent())
                    .setCustomContentView(getCustomView())
                    .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                    .build();

            manager.notify(NOTIFICATION_ID, notification);
        }
    }

    private RemoteViews getCustomView() {
        RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.notification_music);

        if (musicList != null && currentPosition >= 0 && currentPosition < musicList.size()) {
            MusicInfo musicInfo = musicList.get(currentPosition);
            remoteViews.setTextViewText(R.id.tv_notification_title, musicInfo.getMusicName());
            remoteViews.setTextViewText(R.id.tv_notification_artist, musicInfo.getAuthor());

            // 设置封面图片
            Bitmap coverBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.default_cover);
            remoteViews.setImageViewBitmap(R.id.iv_notification_cover, coverBitmap);

            // 设置播放/暂停按钮状态
            remoteViews.setImageViewResource(R.id.iv_notification_play,
                    musicPlayer.isPlaying() ? R.drawable.ic_notification_pause : R.drawable.ic_notification_play);

            // 设置点击事件
            Intent prevIntent = new Intent(this, MusicService.class);
            prevIntent.setAction(MusicPlayActivity.ACTION_PREVIOUS);
            PendingIntent prevPendingIntent = PendingIntent.getService(
                    this, 0, prevIntent, PendingIntent.FLAG_IMMUTABLE);
            remoteViews.setOnClickPendingIntent(R.id.iv_notification_previous, prevPendingIntent);

            Intent playIntent = new Intent(this, MusicService.class);
            playIntent.setAction(musicPlayer.isPlaying() ?
                    MusicPlayActivity.ACTION_PAUSE : MusicPlayActivity.ACTION_PLAY);
            PendingIntent playPendingIntent = PendingIntent.getService(
                    this, 1, playIntent, PendingIntent.FLAG_IMMUTABLE);
            remoteViews.setOnClickPendingIntent(R.id.iv_notification_play, playPendingIntent);

            Intent nextIntent = new Intent(this, MusicService.class);
            nextIntent.setAction(MusicPlayActivity.ACTION_NEXT);
            PendingIntent nextPendingIntent = PendingIntent.getService(
                    this, 2, nextIntent, PendingIntent.FLAG_IMMUTABLE);
            remoteViews.setOnClickPendingIntent(R.id.iv_notification_next, nextPendingIntent);

            Intent closeIntent = new Intent(this, MusicService.class);
            closeIntent.setAction("CLOSE");
            PendingIntent closePendingIntent = PendingIntent.getService(
                    this, 3, closeIntent, PendingIntent.FLAG_IMMUTABLE);
            remoteViews.setOnClickPendingIntent(R.id.iv_notification_close, closePendingIntent);
        }

        return remoteViews;
    }

    private PendingIntent getContentIntent() {
        Intent intent = new Intent(this, MusicPlayActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "音乐播放", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("音乐播放通知");

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }
}